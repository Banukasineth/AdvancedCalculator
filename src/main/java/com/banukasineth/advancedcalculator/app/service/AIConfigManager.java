package com.banukasineth.advancedcalculator.app.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Manages configuration and persistent storage for AI API keys (Google Gemini).
 * Checks System Environment, local config.properties, and Java Preferences.
 */
public class AIConfigManager {

    private static final String PREF_KEY_GEMINI = "gemini_api_key";
    private static final String PREF_KEY_MODEL = "gemini_model_name";
    private static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
    private static final String CONFIG_FILE_NAME = "config.properties";

    private static final Preferences prefs = Preferences.userNodeForPackage(AIConfigManager.class);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Gets the currently active Gemini API key from environment, properties file, or preferences.
     */
    public static String getApiKey() {
        // 1. Environment variables
        String envKey = System.getenv("GEMINI_API_KEY");
        if (isValidKey(envKey)) return envKey.trim();

        String googleEnvKey = System.getenv("GOOGLE_API_KEY");
        if (isValidKey(googleEnvKey)) return googleEnvKey.trim();

        // 2. Local config.properties in project directory
        File localFile = new File(CONFIG_FILE_NAME);
        if (localFile.exists()) {
            String fileKey = readKeyFromFile(localFile);
            if (isValidKey(fileKey)) return fileKey.trim();
        }

        // 3. User home config directory
        File homeFile = new File(System.getProperty("user.home"), ".advancedcalculator/" + CONFIG_FILE_NAME);
        if (homeFile.exists()) {
            String fileKey = readKeyFromFile(homeFile);
            if (isValidKey(fileKey)) return fileKey.trim();
        }

        // 4. Java Preferences
        String prefKey = prefs.get(PREF_KEY_GEMINI, null);
        if (isValidKey(prefKey)) return prefKey.trim();

        return null;
    }

    /**
     * Checks if a valid non-placeholder API key is configured.
     */
    public static boolean hasApiKey() {
        String key = getApiKey();
        return isValidKey(key);
    }

    private static boolean isValidKey(String key) {
        return key != null && !key.trim().isEmpty() && !key.trim().equals("YOUR_API_KEY_HERE");
    }

    /**
     * Saves the Gemini API key to Java Preferences and local config.properties.
     */
    public static void saveApiKey(String key) {
        String clean = key != null ? key.trim() : "";
        if (clean.isEmpty()) {
            prefs.remove(PREF_KEY_GEMINI);
        } else {
            prefs.put(PREF_KEY_GEMINI, clean);
        }

        // Also save to config.properties in project directory
        try {
            File localFile = new File(CONFIG_FILE_NAME);
            Properties props = new Properties();
            if (localFile.exists()) {
                try (FileInputStream fis = new FileInputStream(localFile)) {
                    props.load(fis);
                }
            }
            if (clean.isEmpty()) {
                props.remove("gemini.api.key");
            } else {
                props.setProperty("gemini.api.key", clean);
            }
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                props.store(fos, "AdvancedCalculator Configuration");
            }
        } catch (Exception e) {
            System.err.println("Could not save to local config.properties: " + e.getMessage());
        }
    }

    /**
     * Gets the Gemini model name (default: gemini-3.5-flash-lite).
     */
    public static String getModelName() {
        File localFile = new File(CONFIG_FILE_NAME);
        if (localFile.exists()) {
            try (FileInputStream fis = new FileInputStream(localFile)) {
                Properties props = new Properties();
                props.load(fis);
                String model = props.getProperty("gemini.model.name");
                if (model != null && !model.trim().isEmpty()) {
                    return model.trim();
                }
            } catch (Exception ignored) {}
        }
        return prefs.get(PREF_KEY_MODEL, DEFAULT_MODEL);
    }

    public static void setModelName(String model) {
        if (model != null && !model.trim().isEmpty()) {
            String clean = model.trim();
            prefs.put(PREF_KEY_MODEL, clean);
            try {
                File localFile = new File(CONFIG_FILE_NAME);
                Properties props = new Properties();
                if (localFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(localFile)) {
                        props.load(fis);
                    }
                }
                props.setProperty("gemini.model.name", clean);
                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    props.store(fos, "AdvancedCalculator Configuration");
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Tests the provided API key with a minimal live request to Google Gemini API.
     */
    public static CompletableFuture<TestResult> testApiKey(String apiKey) {
        if (!isValidKey(apiKey)) {
            return CompletableFuture.completedFuture(new TestResult(false, "API key cannot be empty."));
        }

        String model = getModelName();
        return executeTestRequest(apiKey, model).thenCompose(result -> {
            if (result.success) {
                return CompletableFuture.completedFuture(result);
            }
            // If primary model failed due to high demand (503/429) or 404, test fallback
            if (!model.equals("gemini-3.5-flash-lite")) {
                return executeTestRequest(apiKey, "gemini-3.5-flash-lite").thenApply(fallbackRes -> {
                    if (fallbackRes.success) {
                        return new TestResult(true, "Connected! (" + model + " busy; fallback gemini-3.5-flash-lite ready)");
                    }
                    return result; // return original error
                });
            }
            return CompletableFuture.completedFuture(result);
        });
    }

    private static CompletableFuture<TestResult> executeTestRequest(String apiKey, String model) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject userContent = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", "Respond with only the word OK");
        parts.put(part);
        userContent.put("parts", parts);
        contents.put(userContent);
        payload.put("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    if (status == 200) {
                        return new TestResult(true, "Successfully connected to Google Gemini (" + model + ")!");
                    } else {
                        String body = response.body();
                        String errorDetails = "HTTP " + status;
                        try {
                            JSONObject errJson = new JSONObject(body);
                            if (errJson.has("error")) {
                                errorDetails = errJson.getJSONObject("error").optString("message", errorDetails);
                            }
                        } catch (Exception ignored) {}
                        return new TestResult(false, "Connection failed: " + errorDetails);
                    }
                })
                .exceptionally(ex -> new TestResult(false, "Network error: " + ex.getMessage()));
    }

    private static String readKeyFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            String val = props.getProperty("gemini.api.key");
            if (val == null) {
                val = props.getProperty("GEMINI_API_KEY");
            }
            return val;
        } catch (Exception e) {
            return null;
        }
    }

    public static class TestResult {
        public final boolean success;
        public final String message;

        public TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
