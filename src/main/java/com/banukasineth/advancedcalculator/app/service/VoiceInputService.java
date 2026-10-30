package com.banukasineth.advancedcalculator.app.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service for capturing audio from the microphone and converting spoken voice
 * into mathematical queries and expressions (Speech-to-Math).
 *
 * Uses Google Gemini Multimodal Audio API as the primary high-accuracy engine
 * (specialized for spoken math terminology and equations), with an offline fallback
 * to Windows Speech Recognition Engine when offline or without an API key.
 */
public class VoiceInputService {

    private static final float SAMPLE_RATE = 16000.0f;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1; // Mono
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private static final List<String> GEMINI_MODELS = Arrays.asList(
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.8-flash"
    );

    private final HttpClient httpClient;
    private final AudioFormat audioFormat;
    private TargetDataLine targetLine;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private ByteArrayOutputStream audioBuffer;
    private Thread recordingThread;
    private long recordingStartTime = 0;

    public VoiceInputService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.audioFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
    }

    /**
     * Checks if a microphone recording line is supported and available on this machine.
     */
    public boolean isSupported() {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        return AudioSystem.isLineSupported(info);
    }

    /**
     * Returns true if audio recording is currently active.
     */
    public boolean isRecording() {
        return isRecording.get();
    }

    /**
     * Starts capturing microphone audio in a background thread.
     */
    public synchronized void startRecording() throws LineUnavailableException {
        if (isRecording.get()) {
            return;
        }

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone input line is not supported on this machine.");
        }

        targetLine = (TargetDataLine) AudioSystem.getLine(info);
        targetLine.open(audioFormat);
        targetLine.start();

        isRecording.set(true);
        audioBuffer = new ByteArrayOutputStream();
        recordingStartTime = System.currentTimeMillis();

        recordingThread = new Thread(() -> {
            byte[] data = new byte[2048];
            while (isRecording.get()) {
                int read = targetLine.read(data, 0, data.length);
                if (read > 0) {
                    audioBuffer.write(data, 0, read);
                }
            }
        }, "VoiceInput-Recorder");
        recordingThread.setDaemon(true);
        recordingThread.start();
    }

    /**
     * Stops audio recording and packages the captured PCM data into standard WAV format.
     * @return WAV formatted byte array, or null if recording was too brief or empty.
     */
    public synchronized byte[] stopRecording() {
        if (!isRecording.get()) {
            return null;
        }

        isRecording.set(false);

        if (targetLine != null) {
            try {
                targetLine.stop();
                targetLine.close();
            } catch (Exception ignored) {}
        }

        if (recordingThread != null) {
            try {
                recordingThread.join(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        byte[] rawPcm = audioBuffer != null ? audioBuffer.toByteArray() : new byte[0];
        long durationMs = System.currentTimeMillis() - recordingStartTime;

        // Discard recording if too short (< 250ms or < 2000 bytes)
        if (durationMs < 250 || rawPcm.length < 2000) {
            return null;
        }

        return convertPcmToWav(rawPcm, audioFormat);
    }

    /**
     * Cancels any active recording and discards audio buffers.
     */
    public synchronized void cancelRecording() {
        isRecording.set(false);
        if (targetLine != null) {
            try {
                targetLine.stop();
                targetLine.close();
            } catch (Exception ignored) {}
        }
        audioBuffer = null;
    }

    /**
     * Transcribes recorded WAV audio to a clean mathematical query or expression.
     * Prioritizes Gemini Multimodal Audio if an API key is available, falling back to
     * Windows offline speech recognition if offline or unconfigured.
     */
    public CompletableFuture<String> transcribe(byte[] wavBytes) {
        if (wavBytes == null || wavBytes.length == 0) {
            return CompletableFuture.completedFuture("");
        }

        String apiKey = AIConfigManager.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            List<String> queue = buildModelQueue();
            return transcribeWithGeminiFallback(apiKey, wavBytes, queue)
                    .handle((result, ex) -> {
                        if (ex == null && result != null && !result.trim().isEmpty() && !result.equals("EMPTY_SPEECH")) {
                            return result.trim();
                        }
                        System.out.println("Gemini voice transcription failed or returned empty (" + 
                                (ex != null ? ex.getMessage() : "empty") + "). Falling back to offline recognizer...");
                        return transcribeOffline(wavBytes);
                    });
        } else {
            return CompletableFuture.supplyAsync(() -> transcribeOffline(wavBytes));
        }
    }

    private List<String> buildModelQueue() {
        String preferred = AIConfigManager.getModelName();
        List<String> queue = new ArrayList<>();
        if (preferred != null && !preferred.trim().isEmpty()) {
            queue.add(preferred.trim());
        }
        for (String m : GEMINI_MODELS) {
            if (!queue.contains(m)) {
                queue.add(m);
            }
        }
        return queue;
    }

    private CompletableFuture<String> transcribeWithGeminiFallback(String apiKey, byte[] wavBytes, List<String> modelQueue) {
        if (modelQueue == null || modelQueue.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("All Gemini models unavailable for audio transcription."));
        }

        String model = modelQueue.get(0);
        List<String> remaining = modelQueue.subList(1, modelQueue.size());

        return callGeminiAudioTranscription(model, apiKey, wavBytes)
                .handle((result, ex) -> {
                    if (ex == null && result != null && !result.trim().isEmpty()) {
                        return CompletableFuture.completedFuture(result);
                    }
                    return transcribeWithGeminiFallback(apiKey, wavBytes, remaining);
                })
                .thenCompose(java.util.function.Function.identity());
    }

    private CompletableFuture<String> callGeminiAudioTranscription(String model, String apiKey, byte[] wavBytes) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

        String prompt = "You are a speech-to-math transcription engine for an advanced scientific calculator. "
                + "Listen carefully to the user's spoken voice audio and convert what they asked or said into a clean mathematical expression, equation, or concise math question ready to be solved.\n\n"
                + "Examples:\n"
                + "- 'what is derivative of sin x' -> 'd/dx(sin(x))'\n"
                + "- 'solve two x plus six equals eighteen' -> '2x + 6 = 18'\n"
                + "- 'integrate x squared from zero to three' -> '\\int_{0}^{3} x^2 \\, dx'\n"
                + "- 'forty five times twelve' -> '45 * 12'\n"
                + "- 'square root of one hundred forty four' -> '\\sqrt{144}'\n"
                + "- 'find limit of sin x over x as x approaches zero' -> '\\lim_{x \\to 0} \\frac{\\sin(x)}{x}'\n"
                + "- 'what is twenty five percent of two hundred' -> '25% * 200'\n\n"
                + "Requirements:\n"
                + "1. Return ONLY the mathematical query, equation, or LaTeX formula.\n"
                + "2. Do NOT include markdown code blocks (```), quotation marks, or conversational text.\n"
                + "3. If it is a conceptual question (e.g. 'why is pi irrational?'), return the clean question.\n"
                + "4. If no clear speech or only silence is detected, return strictly: EMPTY_SPEECH.";

        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray parts = new JSONArray();

        // 1. Text prompt
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);
        parts.put(textPart);

        // 2. Audio inline data
        JSONObject inlineDataObj = new JSONObject();
        inlineDataObj.put("mimeType", "audio/wav");
        inlineDataObj.put("data", Base64.getEncoder().encodeToString(wavBytes));

        JSONObject audioPart = new JSONObject();
        audioPart.put("inlineData", inlineDataObj);
        parts.put(audioPart);

        contentObj.put("parts", parts);
        contents.put(contentObj);
        payload.put("contents", contents);

        JSONObject genConfig = new JSONObject();
        genConfig.put("temperature", 0.0);
        genConfig.put("topP", 0.95);
        payload.put("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int code = response.statusCode();
                    if (code == 200) {
                        return cleanTranscribedOutput(extractTextFromGeminiResponse(response.body()));
                    }
                    throw new RuntimeException("Gemini Audio API returned HTTP " + code);
                });
    }

    private String extractTextFromGeminiResponse(String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.optJSONObject("content");
                if (content != null) {
                    JSONArray parts = content.optJSONArray("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return parts.getJSONObject(0).optString("text", "");
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String cleanTranscribedOutput(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.equalsIgnoreCase("EMPTY_SPEECH")) {
            return "";
        }
        // Remove markdown code fences if model returned them
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline != -1) {
                s = s.substring(firstNewline + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
            s = s.trim();
        }
        // Remove leading/trailing quotation marks
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        // Remove trailing period if it looks like a formula or expression
        if (s.endsWith(".") && !s.contains(" ") && s.length() > 1) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    /**
     * Offline speech recognition fallback using Windows System.Speech.Recognition.
     */
    private String transcribeOffline(byte[] wavBytes) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("calc_voice_", ".wav");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(wavBytes);
            }

            String absPath = tempFile.getAbsolutePath().replace("\\", "\\\\");
            String psCommand = "& { " +
                    "Add-Type -AssemblyName System.Speech; " +
                    "$sre = New-Object System.Speech.Recognition.SpeechRecognitionEngine; " +
                    "$sre.SetInputToWaveFile('" + absPath + "'); " +
                    "$sre.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar)); " +
                    "$res = $sre.Recognize([TimeSpan]::FromSeconds(8)); " +
                    "if ($res -ne $null) { Write-Output $res.Text } " +
                    "$sre.Dispose(); " +
                    "}";

            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", psCommand);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(" ");
                }
            }
            process.waitFor();

            String recognized = sb.toString().trim();
            if (!recognized.isEmpty()) {
                return normalizeSpokenMath(recognized);
            }
        } catch (Exception e) {
            System.err.println("Offline speech recognition exception: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
        return "";
    }

    /**
     * Normalizes spoken English mathematical phrases into calculator-friendly notation.
     */
    public static String normalizeSpokenMath(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String s = text.trim();

        // Map spoken number words to digits
        Map<String, String> numMap = new LinkedHashMap<>();
        numMap.put("(?i)\\bzero\\b", "0");
        numMap.put("(?i)\\bone\\b", "1");
        numMap.put("(?i)\\btwo\\b", "2");
        numMap.put("(?i)\\bthree\\b", "3");
        numMap.put("(?i)\\bfour\\b", "4");
        numMap.put("(?i)\\bfive\\b", "5");
        numMap.put("(?i)\\bsix\\b", "6");
        numMap.put("(?i)\\bseven\\b", "7");
        numMap.put("(?i)\\beight\\b", "8");
        numMap.put("(?i)\\bnine\\b", "9");
        numMap.put("(?i)\\bten\\b", "10");
        numMap.put("(?i)\\beleven\\b", "11");
        numMap.put("(?i)\\btwelve\\b", "12");
        numMap.put("(?i)\\btwenty\\b", "20");
        numMap.put("(?i)\\bthirty\\b", "30");
        numMap.put("(?i)\\bforty\\b", "40");
        numMap.put("(?i)\\bfifty\\b", "50");
        numMap.put("(?i)\\bsixty\\b", "60");
        numMap.put("(?i)\\bseventy\\b", "70");
        numMap.put("(?i)\\beighty\\b", "80");
        numMap.put("(?i)\\bninety\\b", "90");
        numMap.put("(?i)\\bhundred\\b", "100");

        for (Map.Entry<String, String> entry : numMap.entrySet()) {
            s = s.replaceAll(entry.getKey(), entry.getValue());
        }

        // Common math operators and functions
        s = s.replaceAll("(?i)\\bmultiplied by\\b", "*");
        s = s.replaceAll("(?i)\\btimes\\b", "*");
        s = s.replaceAll("(?i)\\bdivided by\\b", "/");
        s = s.replaceAll("(?i)\\bover\\b", "/");
        s = s.replaceAll("(?i)\\bplus\\b", "+");
        s = s.replaceAll("(?i)\\bminus\\b", "-");
        s = s.replaceAll("(?i)\\bequals\\b", "=");
        s = s.replaceAll("(?i)\\bequal to\\b", "=");
        s = s.replaceAll("(?i)\\bis equal to\\b", "=");
        s = s.replaceAll("(?i)\\bsquared\\b", "^2");
        s = s.replaceAll("(?i)\\bcubed\\b", "^3");
        s = s.replaceAll("(?i)\\bto the power of\\b", "^");
        s = s.replaceAll("(?i)\\bpower of\\b", "^");
        s = s.replaceAll("(?i)\\bsquare root of\\b", "\\\\sqrt{");
        s = s.replaceAll("(?i)\\bsine of\\b", "sin(");
        s = s.replaceAll("(?i)\\bcosine of\\b", "cos(");
        s = s.replaceAll("(?i)\\btangent of\\b", "tan(");
        s = s.replaceAll("(?i)\\bderivative of\\b", "d/dx(");
        s = s.replaceAll("(?i)\\bintegral of\\b", "\\\\int ");

        // Clean up spaces around operators
        s = s.replaceAll("\\s*([\\+\\-\\*/=\\^])\\s*", " $1 ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private static byte[] convertPcmToWav(byte[] pcmData, AudioFormat format) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
            AudioInputStream ais = new AudioInputStream(bais, format, pcmData.length / format.getFrameSize());
            ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
            return wavOut.toByteArray();
        } catch (Exception e) {
            System.err.println("Error converting PCM to WAV: " + e.getMessage());
            return null;
        }
    }
}
