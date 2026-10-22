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
                        return parseResponse(response.body());
                    } else {
                        System.err.println("API Request failed with status code: " + response.statusCode());
                        System.err.println("Response body: " + response.body());
                        return "Error: Could not solve the equation.";
                    }
                })
                .exceptionally(ex -> "Error: " + ex.getMessage());
    }

    /**
     * Answers a specific question about a mathematical expression asynchronously.
     */
    public CompletableFuture<String> askQuestion(String latexExpression, String question) {
        if (USE_MOCK) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(1500); // Simulate network delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Mock Response for: '" + question + "' regarding " + latexExpression + "\n\nThis is a simulated AI response indicating the properties and features of the requested mathematical expression.";
            });
        }

        String prompt = "You are an advanced mathematical AI assistant. A user is looking at the graph of the function: " 
                + latexExpression 
                + ". They have asked the following question about it: \"" + question + "\". "
                + "Please provide a clear, concise, and accurate answer. Format your response in plain text (no markdown wrapping).";

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
                        return parseResponse(response.body());
                    } else {
                        return "Error: Failed to connect to AI (HTTP " + response.statusCode() + ")";
                    }
                })
                .exceptionally(ex -> "Error: " + ex.getMessage());
    }

    private String parseResponse(String responseBody) {
        try {
            int textStart = responseBody.indexOf("\"text\": \"") + 9;
            int textEnd = responseBody.indexOf("\"", textStart);
            if (textStart > 8 && textEnd > textStart) {
                return responseBody.substring(textStart, textEnd)
                        .replace("\\n", "\n")
                        .replace("\\\\", "\\");
            }
        } catch (Exception e) {
            System.err.println("Failed to parse AI response: " + e.getMessage());
        }
        return "Error parsing response";
    }

    private CompletableFuture<String> simulateMockResponse(String inputLatex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network latency (2 seconds)
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String clean = inputLatex != null ? inputLatex : "";

            if (clean.contains("\\frac{d}{d") || clean.contains("dy")) {
                return "\\begin{aligned}\n" +
                        "& \\text{Differential Equation:} \\\\\n" +
                        "& " + clean + " \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: Compute partial derivatives} \\\\\n" +
                        "& \\frac{\\partial}{\\partial y}(3y) = 3 \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 2: Substitute and simplify} \\\\\n" +
                        "& \\frac{d^2 y}{dx^2} + 3 = 5 \\implies \\frac{d^2 y}{dx^2} = 2 \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 3: Integrate twice with respect to } x \\\\\n" +
                        "& \\frac{dy}{dx} = 2x + C_1 \\\\\n" +
                        "& y(x) = x^2 + C_1 x + C_2 \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Final Solution:} \\\\\n" +
                        "& \\mathbf{y(x) = x^2 + C_1 x + C_2}\n" +
                        "\\end{aligned}";
            } else if (clean.contains("\\int")) {
                return "\\begin{aligned}\n" +
                        "& \\text{Integration Problem:} \\\\\n" +
                        "& " + clean + " \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: Apply integration rules} \\\\\n" +
                        "& \\int 2x\\,dx + \\int \\sin(x)\\,dx \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 2: Compute antiderivatives} \\\\\n" +
                        "& = x^2 - \\cos(x) + C \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Final Answer:} \\\\\n" +
                        "& \\mathbf{x^2 - \\cos(x) + C}\n" +
                        "\\end{aligned}";
            } else if (clean.contains("a^2") && clean.contains("b^2") && clean.contains("c^2")) {
                return "\\begin{aligned}\n" +
                        "& \\text{Pythagorean Theorem:} \\\\\n" +
                        "& a^2 + b^2 = c^2 \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Solve for hypotenuse } c: \\\\\n" +
                        "& c = \\sqrt{a^2 + b^2} \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Solve for side } a: \\\\\n" +
                        "& a = \\sqrt{c^2 - b^2}\n" +
                        "\\end{aligned}";
            } else if (clean.contains("=")) {
                return "\\begin{aligned}\n" +
                        "& \\text{Equation to Solve:} \\\\\n" +
                        "& " + clean + " \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: Isolate variable terms} \\\\\n" +
                        "& \\text{Subtract constants from both sides} \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 2: Solve for unknown} \\\\\n" +
                        "& x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a} \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Roots:} \\\\\n" +
                        "& \\mathbf{x_1 = 2, \\quad x_2 = -3}\n" +
                        "\\end{aligned}";
            }

            // Default rich step-by-step mock LaTeX response
            return "\\begin{aligned}\n" +
                    "& \\text{Analyzing Expression:} \\\\\n" +
                    "& " + clean + " \\\\\n" +
                    "& \\\\\n" +
                    "& \\text{Step 1: Simplify algebraic terms} \\\\\n" +
                    "& \\Rightarrow f'(x) = 2x + \\sin(x) \\\\\n" +
                    "& \\\\\n" +
                    "& \\text{Step 2: Evaluate critical points} \\\\\n" +
                    "& \\Rightarrow \\int (2x + \\sin(x))\\,dx \\\\\n" +
                    "& \\\\\n" +
                    "& \\text{Final Answer:} \\\\\n" +
                    "& \\mathbf{x^2 - \\cos(x) + C}\n" +
                    "\\end{aligned}";
        });
    }
}
