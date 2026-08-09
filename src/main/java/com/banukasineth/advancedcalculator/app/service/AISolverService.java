package com.banukasineth.advancedcalculator.app.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AISolverService {

    private static final String GEMINI_API_KEY = "YOUR_API_KEY_HERE";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

    // Set this to true for testing without a real API key.
    private static final boolean USE_MOCK = true;

    private final HttpClient httpClient;

    public AISolverService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Solves the given LaTeX math expression and returns a step-by-step LaTeX solution asynchronously.
     */
    public CompletableFuture<String> solveMathExpression(String latexExpression) {
        if (USE_MOCK) {
            return simulateMockResponse(latexExpression);
        }

        String prompt = "You are an advanced mathematical AI. Solve the following mathematical expression step-by-step. "
                + "Return ONLY the raw LaTeX string for the solution without any markdown wrappers (do not include ```latex). "
                + "Format it beautifully using \\begin{aligned} ... \\end{aligned} so it can be rendered by KaTeX. "
                + "Expression to solve: " + latexExpression;

        String requestBody = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\":[{\"text\": \"" + prompt.replace("\"", "\\\"").replace("\\", "\\\\") + "\"}]\n" +
                "  }]\n" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        // Very naive JSON parsing just to extract the text block
                        String body = response.body();
                        try {
                            int textStart = body.indexOf("\"text\": \"") + 9;
                            int textEnd = body.indexOf("\"", textStart);
                            if (textStart > 8 && textEnd > textStart) {
                                String latexResult = body.substring(textStart, textEnd)
                                        .replace("\\n", "\n")
                                        .replace("\\\\", "\\");
                                return latexResult;
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to parse AI response: " + e.getMessage());
                        }
                    } else {
                        System.err.println("API Request failed with status code: " + response.statusCode());
                        System.err.println("Response body: " + response.body());
                    }
                    return "Error: Could not solve the equation.";
                });
    }

    private CompletableFuture<String> simulateMockResponse(String inputLatex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network latency (2 seconds)
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // A beautiful step-by-step mock LaTeX response
            return "\\begin{aligned}\n" +
                    "& \\text{Analyzing Expression:} \\\\\n" +
                    "& " + inputLatex + " \\\\\n" +
                    "& \\\\\n" +
                    "& \\text{Step 1: Simplify terms} \\\\\n" +
                    "& \\Rightarrow f'(x) = 2x + \\sin(x) \\\\\n" +
                    "& \\\\\n" +
                    "& \\text{Step 2: Apply chain rule} \\\\\n" +
                    "& \\Rightarrow \\int (2x + \\sin(x)) dx \\\\\n" +
                    "& \\\\\n" +
                    "& \\text{Final Answer:} \\\\\n" +
                    "& \\mathbf{x^2 - \\cos(x) + C}\n" +
                    "\\end{aligned}";
        });
    }
}
