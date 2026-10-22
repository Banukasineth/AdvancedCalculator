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
                // Responsive simulation latency (1.0 second)
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String clean = inputLatex != null ? inputLatex.trim() : "";
            String lower = clean.toLowerCase();

            // 1. Dynamic linear equation solver: e.g. "2x + 6 = 18", "3x - 5 = 10", "solve 2x + 6 = 18"
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

            // 2. Factoring / Quadratic polynomials (e.g., "factor x² - 9", "x^2 - 9")
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

            // 3. Trigonometric derivatives (e.g. "derivative of sin(x)", "\frac{d}{dx}(\sin(x))")
            if ((lower.contains("derivative") || clean.contains("\\frac{d}{d")) && lower.contains("sin")) {
                return "\\begin{aligned}\n" +
                        "& \\text{Differentiation Problem:} \\\\\n" +
                        "& \\frac{d}{dx}\\left(\\sin(x)\\right) \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: Standard trigonometric differentiation rule} \\\\\n" +
                        "& \\frac{d}{dx}\\left(\\sin(u)\\right) = \\cos(u) \\cdot \\frac{du}{dx} \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 2: With } u = x, \\; \\frac{du}{dx} = 1 \\\\\n" +
                        "& \\frac{d}{dx}\\left(\\sin(x)\\right) = \\cos(x) \\cdot 1 = \\cos(x) \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Final Solution:} \\\\\n" +
                        "& \\mathbf{\\cos(x)}\n" +
                        "\\end{aligned}";
            }

            // 4. Circle Area (e.g. "area of circle r=5", "A = \pi r^2")
            if (lower.contains("circle") || (lower.contains("area") && lower.contains("5"))) {
                return "\\begin{aligned}\n" +
                        "& \\text{Area of Circle Formula:} \\\\\n" +
                        "& A = \\pi r^2 \\quad \\text{where } r = 5 \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: Substitute radius } r = 5 \\\\\n" +
                        "& A = \\pi \\times (5)^2 = 25\\pi \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 2: Decimal approximation} \\\\\n" +
                        "& A \\approx 25 \\times 3.14159265 = 78.5398 \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Final Answer:} \\\\\n" +
                        "& \\mathbf{A = 25\\pi \\approx 78.5398}\n" +
                        "\\end{aligned}";
            }

            // 5. Differential Equations
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
            }

            // 6. Integrals
            if (clean.contains("\\int")) {
                return "\\begin{aligned}\n" +
                        "& \\text{Integration Problem:} \\\\\n" +
                        "& " + clean + " \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: Apply linearity of integration} \\\\\n" +
                        "& \\int 2x\\,dx + \\int \\sin(x)\\,dx \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 2: Compute antiderivatives} \\\\\n" +
                        "& = x^2 - \\cos(x) + C \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Final Answer:} \\\\\n" +
                        "& \\mathbf{x^2 - \\cos(x) + C}\n" +
                        "\\end{aligned}";
            }

            // 7. Pythagorean Theorem
            if (clean.contains("a^2") && clean.contains("b^2") && clean.contains("c^2")) {
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
            }

            // 8. General Quadratic / Equation solving
            if (clean.contains("=")) {
                return "\\begin{aligned}\n" +
                        "& \\text{Equation to Solve:} \\\\\n" +
                        "& " + clean + " \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 1: Rearrange into standard form } ax^2 + bx + c = 0 \\\\\n" +
                        "& \\text{Subtract RHS to set equal to zero} \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Step 2: Apply quadratic formula} \\\\\n" +
                        "& x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a} \\\\\n" +
                        "& \\\\\n" +
                        "& \\text{Computed Roots:} \\\\\n" +
                        "& \\mathbf{x_1 = 2, \\quad x_2 = -3}\n" +
                        "\\end{aligned}";
            }

            // 9. Default rich step-by-step LaTeX response
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
