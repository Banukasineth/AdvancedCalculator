package com.banukasineth.advancedcalculator.app.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;
import org.json.JSONObject;

public class AISolverService {

    private final HttpClient httpClient;

    // Resilient fallback order if the preferred model is temporarily busy (503/429) or migrated (404)
    private static final List<String> KNOWN_MODELS = Arrays.asList(
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.8-flash"
    );

    public AISolverService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Solves the given LaTeX math expression and returns a step-by-step LaTeX solution asynchronously.
     * Uses real Google Gemini API if an API key is configured, otherwise falls back to local simulation.
     */
    public CompletableFuture<String> solveMathExpression(String latexExpression) {
        String apiKey = AIConfigManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return simulateMockResponse(latexExpression);
        }

        String prompt = "You are an expert mathematical AI. Solve the following mathematical expression or problem step-by-step with complete mathematical rigor.\n\n"
                + "Requirements:\n"
                + "1. Format the entire solution strictly as a KaTeX aligned block starting with \\begin{aligned} and ending with \\end{aligned}.\n"
                + "2. Show each step with clear explanations using \\text{...}. Keep math symbols outside \\text{...}.\n"
                + "3. State the final answer on the last line highlighted with \\mathbf{...}.\n"
                + "4. Return ONLY the raw LaTeX string. Do NOT wrap it in markdown code blocks like ```latex or ```. Do NOT include any conversational text outside \\begin{aligned} and \\end{aligned}.\n"
                + "5. Ensure all braces { and } are perfectly matched.\n\n"
                + "Mathematical problem to solve: " + latexExpression;

        List<String> modelQueue = buildModelQueue();
        return callGeminiWithFallback(apiKey, prompt, modelQueue)
                .thenApply(this::cleanLatexOutput)
                .exceptionally(ex -> {
                    System.err.println("Gemini API call failed: " + ex.getMessage());
                    return "\\begin{aligned}\n" +
                           "& \\text{\\textbf{AI Solver Notice:}} \\\\\n" +
                           "& \\text{Failed to connect to Gemini API: " + escapeForLatex(ex.getMessage()) + "} \\\\\n" +
                           "& \\text{Please verify your API key or network in AI Settings.} \\\\\n" +
                           "\\end{aligned}";
                });
    }

    /**
     * Answers a specific question about a mathematical expression asynchronously using Gemini.
     */
    public CompletableFuture<String> askQuestion(String latexExpression, String question) {
        String apiKey = AIConfigManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Mock Response (No Gemini API key configured): Regarding " + latexExpression + ", " + question + 
                       ".\nConfigure your Gemini API key in AI Settings to enable live AI responses.";
            });
        }

        String prompt = "You are an advanced mathematical AI assistant. A user is analyzing the mathematical expression/function: " 
                + latexExpression 
                + ".\nThey asked: \"" + question + "\".\n"
                + "Please provide a clear, concise, and mathematically accurate answer. Format in plain text.";

        List<String> modelQueue = buildModelQueue();
        return callGeminiWithFallback(apiKey, prompt, modelQueue)
                .thenApply(res -> res != null ? res.trim() : "No response generated.")
                .exceptionally(ex -> "Error connecting to AI: " + ex.getMessage());
    }

    private List<String> buildModelQueue() {
        String preferred = AIConfigManager.getModelName();
        List<String> queue = new ArrayList<>();
        if (preferred != null && !preferred.trim().isEmpty()) {
            queue.add(preferred.trim());
        }
        for (String m : KNOWN_MODELS) {
            if (!queue.contains(m)) {
                queue.add(m);
            }
        }
        return queue;
    }

    private CompletableFuture<String> callGeminiWithFallback(String apiKey, String prompt, List<String> modelQueue) {
        if (modelQueue == null || modelQueue.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("All Gemini models are currently experiencing high demand. Please try again shortly."));
        }

        String currentModel = modelQueue.get(0);
        List<String> remainingQueue = modelQueue.subList(1, modelQueue.size());

        return callGeminiGenerateContent(currentModel, apiKey, prompt)
                .handle((result, ex) -> {
                    if (ex == null && result != null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    System.out.println("Model " + currentModel + " returned (" + (ex != null ? ex.getMessage() : "null") + "). Attempting fallback...");
                    return callGeminiWithFallback(apiKey, prompt, remainingQueue);
                })
                .thenCompose(java.util.function.Function.identity());
    }

    private CompletableFuture<String> callGeminiGenerateContent(String model, String apiKey, String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject partObj = new JSONObject();
        partObj.put("text", prompt);
        parts.put(partObj);
        contentObj.put("parts", parts);
        contents.put(contentObj);
        payload.put("contents", contents);

        JSONObject genConfig = new JSONObject();
        genConfig.put("temperature", 0.1);
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
                    throw new RuntimeException("Gemini API Error (" + errMsg + ")");
                });
    }

    private String extractTextFromGeminiResponse(String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);
            if (root.has("candidates")) {
                JSONArray candidates = root.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCand = candidates.getJSONObject(0);
                    if (firstCand.has("content")) {
                        JSONObject content = firstCand.getJSONObject("content");
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                return parts.getJSONObject(0).getString("text");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
        }
        return "Error: Could not parse response from Gemini.";
    }

    private String cleanLatexOutput(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "\\begin{aligned} & \\text{No solution produced.} \\end{aligned}";
        }

        String cleaned = raw.trim();

        // 1. If markdown code fence is present, extract its inner content
        if (cleaned.contains("```")) {
            int firstFence = cleaned.indexOf("```");
            int newlineAfterFence = cleaned.indexOf('\n', firstFence);
            if (newlineAfterFence != -1) {
                int closingFence = cleaned.indexOf("```", newlineAfterFence);
                if (closingFence != -1) {
                    cleaned = cleaned.substring(newlineAfterFence + 1, closingFence).trim();
                } else {
                    cleaned = cleaned.substring(newlineAfterFence + 1).trim();
                }
            }
        }

        // 2. Standardize align* / align / gather / equation environments to aligned
        cleaned = cleaned.replaceAll("\\\\begin\\{align\\*?\\}", "\\\\begin{aligned}")
                         .replaceAll("\\\\end\\{align\\*?\\}", "\\\\end{aligned}")
                         .replaceAll("\\\\begin\\{gather\\*?\\}", "\\\\begin{aligned}")
                         .replaceAll("\\\\end\\{gather\\*?\\}", "\\\\end{aligned}")
                         .replaceAll("\\\\begin\\{equation\\*?\\}", "\\\\begin{aligned}")
                         .replaceAll("\\\\end\\{equation\\*?\\}", "\\\\end{aligned}");

        // 3. Extract strictly from \begin{aligned} to \end{aligned} if present
        int beginIdx = cleaned.indexOf("\\begin{aligned}");
        int endIdx = cleaned.lastIndexOf("\\end{aligned}");
        if (beginIdx != -1 && endIdx != -1 && endIdx > beginIdx) {
            cleaned = cleaned.substring(beginIdx, endIdx + "\\end{aligned}".length()).trim();
        } else if (!cleaned.contains("\\begin{aligned}")) {
            // If model omitted \begin{aligned}, wrap it safely
            cleaned = "\\begin{aligned}\n& " + cleaned.replace("\n\n", " \\\\\n& ") + "\n\\end{aligned}";
        }

        // 4. Ensure all braces are balanced to guarantee KaTeX parsing success
        cleaned = balanceBraces(cleaned);

        return cleaned;
    }

    private String balanceBraces(String latex) {
        if (latex == null) return "";
        StringBuilder sb = new StringBuilder();
        int openCount = 0;
        for (int i = 0; i < latex.length(); i++) {
            char c = latex.charAt(i);
            if (c == '\\' && i + 1 < latex.length() && (latex.charAt(i + 1) == '{' || latex.charAt(i + 1) == '}')) {
                sb.append(c).append(latex.charAt(i + 1));
                i++;
                continue;
            }
            if (c == '{') {
                openCount++;
                sb.append(c);
            } else if (c == '}') {
                if (openCount > 0) {
                    openCount--;
                    sb.append(c);
                }
                // Ignore unmatched closing brace
            } else {
                sb.append(c);
            }
        }
        while (openCount > 0) {
            sb.append('}');
            openCount--;
        }
        return sb.toString();
    }

    private String escapeForLatex(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\backslash ")
                  .replace("{", "\\{")
                  .replace("}", "\\}")
                  .replace("_", "\\_")
                  .replace("^", "\\^")
                  .replace("%", "\\%")
                  .replace("$", "\\$");
    }

    /**
     * Fallback mock simulation used when no Gemini API key is configured.
     */
    private CompletableFuture<String> simulateMockResponse(String inputLatex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String clean = inputLatex != null ? inputLatex.trim() : "";
            String lower = clean.toLowerCase();

            // 1. Dynamic linear equation solver: e.g. "2x + 6 = 18", "3x - 5 = 10"
            java.util.regex.Pattern pLinear = java.util.regex.Pattern.compile("(?i)(?:solve\\s*)?([+-]?[0-9]*\\.?[0-9]*)\\s*x\\s*([+-])\\s*([0-9]*\\.?[0-9]+)\\s*=\\s*([+-]?[0-9]*\\.?[0-9]+)");
            java.util.regex.Matcher mLinear = pLinear.matcher(clean);
            if (mLinear.find()) {
                String aStr = mLinear.group(1).trim();
                double a = 1.0;
                if (aStr.equals("-")) a = -1.0;
                else if (!aStr.isEmpty() && !aStr.equals("+")) a = Double.parseDouble(aStr);

                String sign = mLinear.group(2).trim();
                double b = Double.parseDouble(mLinear.group(3).trim());
                if (sign.equals("-")) b = -b;

                double c = Double.parseDouble(mLinear.group(4).trim());

                double rightAfterSub = c - b;
                double xAns = a != 0 ? (rightAfterSub / a) : Double.NaN;
                String xStr = (Math.abs(xAns - Math.round(xAns)) < 1e-9) ? String.valueOf((long)Math.round(xAns)) : String.format(java.util.Locale.US, "%.4f", xAns);
                String subStr = (Math.abs(rightAfterSub - Math.round(rightAfterSub)) < 1e-9) ? String.valueOf((long)Math.round(rightAfterSub)) : String.format(java.util.Locale.US, "%.4f", rightAfterSub);
                String aFmt = (Math.abs(a - Math.round(a)) < 1e-9) ? String.valueOf((long)Math.round(a)) : String.format(java.util.Locale.US, "%.4f", a);
                String bFmt = (Math.abs(Math.abs(b) - Math.round(Math.abs(b))) < 1e-9) ? String.valueOf((long)Math.round(Math.abs(b))) : String.format(java.util.Locale.US, "%.4f", Math.abs(b));
                String cFmt = (Math.abs(c - Math.round(c)) < 1e-9) ? String.valueOf((long)Math.round(c)) : String.format(java.util.Locale.US, "%.4f", c);

                String opWord = (b >= 0) ? "Subtract " + bFmt + " from both sides" : "Add " + bFmt + " to both sides";
                String opStep = (b >= 0) ? cFmt + " - " + bFmt : cFmt + " + " + bFmt;

                return "\\begin{aligned}\n" +
                        "& \\text{Linear Equation:} \\\\\n" +
                        "& " + (aFmt.equals("1") ? "" : (aFmt.equals("-1") ? "-" : aFmt)) + "x " + (b >= 0 ? "+ " : "- ") + bFmt + " = " + cFmt + " \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: " + opWord + "} \\\\\n" +
                        "& " + (aFmt.equals("1") ? "" : aFmt) + "x = " + opStep + " = " + subStr + " \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 2: Divide by " + aFmt + "} \\\\\n" +
                        "& x = \\frac{" + subStr + "}{" + aFmt + "} = " + xStr + " \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Final Solution:} \\\\\n" +
                        "& \\mathbf{x = " + xStr + "}\n" +
                        "\\end{aligned}";
            }

            // 2. Factoring / Quadratic polynomials (e.g., "factor x^2 - 9")
            if (lower.contains("factor") || lower.contains("x^2 - 9") || lower.contains("x^2-9") || lower.contains("x² - 9") || lower.contains("x²-9")) {
                return "\\begin{aligned}\n" +
                        "& \\text{Factoring Polynomial:} \\\\\n" +
                        "& x^2 - 9 \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: Identify difference of two squares} \\\\\n" +
                        "& a^2 - b^2 = (a - b)(a + b) \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 2: Substitute } a = x, \\; b = 3 \\\\\n" +
                        "& x^2 - 3^2 = (x - 3)(x + 3) \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Factored Expression:} \\\\\n" +
                        "& \\mathbf{(x - 3)(x + 3)}\n" +
                        "\\end{aligned}";
            }

            // 3. Trigonometric derivatives
            if ((lower.contains("derivative") || clean.contains("\\frac{d}{d")) && lower.contains("sin")) {
                return "\\begin{aligned}\n" +
                        "& \\text{Differentiation:} \\\\\n" +
                        "& \\frac{d}{dx}(\\sin(x)) \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: Apply standard trigonometric derivative identity} \\\\\n" +
                        "& \\frac{d}{dx}(\\sin(u)) = \\cos(u) \\cdot \\frac{du}{dx} \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Final Derivative:} \\\\\n" +
                        "& \\mathbf{\\cos(x)}\n" +
                        "\\end{aligned}";
            }

            // General fallback
            return "\\begin{aligned}\n" +
                    "& \\text{Mathematical Solution:} \\\\\n" +
                    "& " + clean + " \\\\\n" +
                    "& \\\\\n" +
                    "& \\text{Note: Connect Google Gemini API Key in AI Settings for real-time AI solutions.} \\\\\n" +
                    "& \\mathbf{\\text{Expression: } " + clean + "}\n" +
                    "\\end{aligned}";
        });
    }
}
