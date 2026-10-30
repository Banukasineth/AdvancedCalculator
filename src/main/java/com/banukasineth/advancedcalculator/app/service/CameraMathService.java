package com.banukasineth.advancedcalculator.app.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service for extracting mathematical equations, formulas, and expressions
 * from handwritten or printed images using Google Gemini Vision API.
 */
public class CameraMathService {

    private static final List<String> VISION_MODELS = Arrays.asList(
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.8-flash"
    );

    private final HttpClient httpClient;

    public CameraMathService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Extracts mathematical expression from a BufferedImage.
     */
    public CompletableFuture<String> extractMath(BufferedImage image) {
        if (image == null) {
            return CompletableFuture.completedFuture("");
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return extractMath(baos.toByteArray(), "image/png");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Extracts mathematical expression from an image File.
     */
    public CompletableFuture<String> extractMath(File file) {
        if (file == null || !file.exists()) {
            return CompletableFuture.completedFuture("");
        }
        try {
            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int read = fis.read(bytes);
                if (read <= 0) return CompletableFuture.completedFuture("");
            }
            String name = file.getName().toLowerCase();
            String mimeType = "image/png";
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            } else if (name.endsWith(".webp")) {
                mimeType = "image/webp";
            } else if (name.endsWith(".bmp")) {
                mimeType = "image/bmp";
            }
            return extractMath(bytes, mimeType);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Extracts mathematical expression from image bytes and mime type.
     */
    public CompletableFuture<String> extractMath(byte[] imageBytes, String mimeType) {
        if (imageBytes == null || imageBytes.length == 0) {
            return CompletableFuture.completedFuture("");
        }

        String apiKey = AIConfigManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Google Gemini API key not configured. Please open AI Settings and add your API key to use Camera Math OCR."
            ));
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        List<String> modelQueue = buildModelQueue();

        return callGeminiVisionWithFallback(apiKey, base64Image, mimeType, modelQueue)
                .thenApply(this::cleanExtractedLatex);
    }

    private List<String> buildModelQueue() {
        String preferred = AIConfigManager.getModelName();
        List<String> queue = new ArrayList<>();
        if (preferred != null && !preferred.trim().isEmpty()) {
            queue.add(preferred.trim());
        }
        for (String m : VISION_MODELS) {
            if (!queue.contains(m)) {
                queue.add(m);
            }
        }
        return queue;
    }

    private CompletableFuture<String> callGeminiVisionWithFallback(String apiKey, String base64Image, String mimeType, List<String> modelQueue) {
        if (modelQueue == null || modelQueue.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("All Gemini models are currently busy or unavailable. Please try again shortly."));
        }

        String model = modelQueue.get(0);
        List<String> remaining = modelQueue.subList(1, modelQueue.size());

        return callGeminiVision(model, apiKey, base64Image, mimeType)
                .handle((result, ex) -> {
                    if (ex == null && result != null && !result.trim().isEmpty()) {
                        return CompletableFuture.completedFuture(result);
                    }
                    System.out.println("Model " + model + " vision OCR returned (" + (ex != null ? ex.getMessage() : "empty") + "). Attempting fallback...");
                    return callGeminiVisionWithFallback(apiKey, base64Image, mimeType, remaining);
                })
                .thenCompose(java.util.function.Function.identity());
    }

    private CompletableFuture<String> callGeminiVision(String model, String apiKey, String base64Image, String mimeType) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

        String prompt = "You are an expert mathematical OCR and equation scanner for a scientific calculator.\n"
                + "Analyze the provided image containing handwritten or printed mathematical equations, formulas, calculus problems, or numerical calculations.\n\n"
                + "Instructions:\n"
                + "1. Extract the exact mathematical expression, equation, or problem from the image.\n"
                + "2. Format basic arithmetic using standard symbols: use ÷ for division (or /) and × for multiplication (or *). For calculus, use standard notation (\\int, d/dx, \\lim).\n"
                + "3. If the expression in the image ends with a trailing equals sign '=' or '= ?' with no right-hand side, omit the trailing equals sign so the expression can be calculated.\n"
                + "4. If it is an equation with both sides (e.g. 2x + 6 = 18), preserve both sides.\n"
                + "5. Output ONLY the raw mathematical expression or formula. Do NOT wrap in markdown code blocks like ```latex or ```. Do NOT include quotation marks, conversational filler, or explanations.\n"
                + "6. If no mathematical content is found in the image, return strictly: NO_MATH_DETECTED.";

        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray parts = new JSONArray();

        // 1. Text prompt
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);
        parts.put(textPart);

        // 2. Image inline data
        JSONObject inlineDataObj = new JSONObject();
        inlineDataObj.put("mimeType", mimeType);
        inlineDataObj.put("data", base64Image);

        JSONObject imagePart = new JSONObject();
        imagePart.put("inlineData", inlineDataObj);
        parts.put(imagePart);

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
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int code = response.statusCode();
                    if (code == 200) {
                        return extractTextFromGeminiResponse(response.body());
                    }
                    String errMsg = "HTTP " + code;
                    try {
                        JSONObject errJson = new JSONObject(response.body());
                        if (errJson.has("error")) {
                            errMsg = errJson.getJSONObject("error").optString("message", errMsg);
                        }
                    } catch (Exception ignored) {}
                    throw new RuntimeException("Gemini Vision API Error (" + errMsg + ")");
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

    private String cleanExtractedLatex(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.equalsIgnoreCase("NO_MATH_DETECTED")) {
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

        // Remove trailing equals sign if present at the end of arithmetic/problem expressions
        // e.g. "6 \div 2(1 + 2) =" -> "6 ÷ 2(1 + 2)"
        // or "5 + 5 = ?" -> "5 + 5"
        s = s.replaceAll("\\s*=\\s*\\??\\s*$", "").trim();

        // Convert raw LaTeX commands to clean, natural calculator math notation
        // e.g. \div -> ÷, \times -> ×, \cdot -> ×
        s = s.replace("\\div", "÷")
             .replace("\\times", "×")
             .replace("\\cdot", "×");

        return s;
    }
}
