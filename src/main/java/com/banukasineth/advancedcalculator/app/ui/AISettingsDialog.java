package com.banukasineth.advancedcalculator.app.ui;

import com.banukasineth.advancedcalculator.app.service.AIConfigManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;

/**
 * Modern glassmorphic dialog for configuring and testing the Google Gemini AI API Key.
 */
public class AISettingsDialog {

    public static Optional<String> show(Stage owner) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("AI Mathematical Solver Settings");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1A1726; -fx-padding: 24; -fx-background-radius: 14; -fx-border-color: #353148; -fx-border-radius: 14; -fx-border-width: 1;");

        VBox root = new VBox(16);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPrefWidth(480);

        // Header with AI Sparkle icon
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath sparkIcon = new SVGPath();
        sparkIcon.setContent("M12 2L9.5 8.5L3 11l6.5 2.5L12 20l2.5-6.5L21 11l-6.5-2.5L12 2zm7 13l-1.25 3.25L14.5 19.5l3.25 1.25L19 24l1.25-3.25L23.5 19.5l-3.25-1.25L19 15z");
        sparkIcon.setFill(Color.web("#E82E88"));
        sparkIcon.setScaleX(1.2);
        sparkIcon.setScaleY(1.2);

        VBox titleBox = new VBox(3);
        Label titleLabel = new Label("Google Gemini AI Integration");
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: -apple-system, 'Segoe UI', Roboto, sans-serif;");

        Label subtitleLabel = new Label("Real-time mathematical AI for equations, calculus, and step-by-step solutions.");
        subtitleLabel.setStyle("-fx-text-fill: #9D9AA8; -fx-font-size: 12px;");
        subtitleLabel.setWrapText(true);

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        header.getChildren().addAll(sparkIcon, titleBox);

        // Status Card
        HBox statusCard = new HBox(10);
        statusCard.setAlignment(Pos.CENTER_LEFT);
        statusCard.setStyle("-fx-background-color: #232033; -fx-padding: 10 14; -fx-background-radius: 8; -fx-border-color: #38344D; -fx-border-radius: 8; -fx-border-width: 1;");

        boolean hasKey = AIConfigManager.hasApiKey();
        Circle statusDot = new Circle(5);
        statusDot.setFill(hasKey ? Color.web("#4cd137") : Color.web("#e1b12c"));

        Label statusText = new Label(hasKey ? "Status: Connected to Google Gemini (" + AIConfigManager.getModelName() + ")" : "Status: No API key configured (using local fallback)");
        statusText.setStyle("-fx-text-fill: " + (hasKey ? "#A8E6CF" : "#F8D279") + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        statusCard.getChildren().addAll(statusDot, statusText);

        // Model Selector
        VBox modelBox = new VBox(6);
        Label modelLabel = new Label("AI Model:");
        modelLabel.setStyle("-fx-text-fill: #E0DEEC; -fx-font-size: 13px; -fx-font-weight: bold;");

        ComboBox<String> modelCombo = new ComboBox<>();
        modelCombo.getItems().addAll(
                "gemini-3.5-flash-lite (Recommended - Instant & Stable)",
                "gemini-3.5-flash (High Precision Mathematical)",
                "gemini-3.8-flash (Next-Gen Flash)"
        );
        String currentModel = AIConfigManager.getModelName();
        if (currentModel.contains("3.5-flash-lite")) {
            modelCombo.setValue("gemini-3.5-flash-lite (Recommended - Instant & Stable)");
        } else if (currentModel.contains("3.5-flash")) {
            modelCombo.setValue("gemini-3.5-flash (High Precision Mathematical)");
        } else if (currentModel.contains("3.8")) {
            modelCombo.setValue("gemini-3.8-flash (Next-Gen Flash)");
        } else {
            modelCombo.setValue("gemini-3.5-flash-lite (Recommended - Instant & Stable)");
        }
        modelCombo.setMaxWidth(Double.MAX_VALUE);
        modelCombo.setStyle("-fx-background-color: #262432; -fx-text-fill: #FFFFFF; -fx-background-radius: 6;");

        modelBox.getChildren().addAll(modelLabel, modelCombo);

        // API Key Input
        VBox keyBox = new VBox(6);
        Label keyLabel = new Label("Gemini API Key:");
        keyLabel.setStyle("-fx-text-fill: #E0DEEC; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Paste your Gemini API key here (AIzaSy...)");
        passwordField.setStyle("-fx-background-color: #262432; -fx-text-fill: #FFFFFF; -fx-prompt-text-fill: #6E6B7C; -fx-background-radius: 6; -fx-padding: 8 12; -fx-font-size: 13px;");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        TextField plainTextField = new TextField();
        plainTextField.setPromptText("Paste your Gemini API key here (AIzaSy...)");
        plainTextField.setStyle("-fx-background-color: #262432; -fx-text-fill: #FFFFFF; -fx-prompt-text-fill: #6E6B7C; -fx-background-radius: 6; -fx-padding: 8 12; -fx-font-size: 13px;");
        plainTextField.setVisible(false);
        plainTextField.setManaged(false);
        HBox.setHgrow(plainTextField, Priority.ALWAYS);

        // Sync text between password and plain text fields
        passwordField.textProperty().bindBidirectional(plainTextField.textProperty());

        String existingKey = AIConfigManager.getApiKey();
        if (existingKey != null) {
            passwordField.setText(existingKey);
        }

        // Toggle Eye Button (Show / Hide Key)
        Button toggleEyeBtn = new Button();
        toggleEyeBtn.setStyle("-fx-background-color: #2C293B; -fx-text-fill: #C4C2D0; -fx-background-radius: 6; -fx-padding: 7 10; -fx-cursor: hand;");
        toggleEyeBtn.setTooltip(new Tooltip("Show/Hide Key"));
        SVGPath eyeIcon = new SVGPath();
        eyeIcon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
        eyeIcon.setFill(Color.web("#C4C2D0"));
        eyeIcon.setScaleX(0.75);
        eyeIcon.setScaleY(0.75);
        toggleEyeBtn.setGraphic(eyeIcon);

        final boolean[] isPlainVisible = {false};
        toggleEyeBtn.setOnAction(e -> {
            isPlainVisible[0] = !isPlainVisible[0];
            if (isPlainVisible[0]) {
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                plainTextField.setVisible(true);
                plainTextField.setManaged(true);
                eyeIcon.setFill(Color.web("#E82E88"));
            } else {
                plainTextField.setVisible(false);
                plainTextField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                eyeIcon.setFill(Color.web("#C4C2D0"));
            }
        });

        // Paste Button
        Button pasteBtn = new Button("Paste");
        pasteBtn.setStyle("-fx-background-color: #2C293B; -fx-text-fill: #E0DEEC; -fx-background-radius: 6; -fx-padding: 8 12; -fx-cursor: hand; -fx-font-size: 12px;");
        pasteBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                passwordField.setText(clipboard.getString().trim());
            }
        });

        inputRow.getChildren().addAll(passwordField, plainTextField, toggleEyeBtn, pasteBtn);

        // Link to Google AI Studio
        Hyperlink keyLink = new Hyperlink("Get a free Gemini API key at Google AI Studio (aistudio.google.com)");
        keyLink.setStyle("-fx-text-fill: #E82E88; -fx-font-size: 12px; -fx-underline: true; -fx-padding: 2 0 0 0;");
        keyLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://aistudio.google.com/app/apikey"));
                }
            } catch (Exception ex) {
                System.err.println("Could not open browser: " + ex.getMessage());
            }
        });

        keyBox.getChildren().addAll(keyLabel, inputRow, keyLink);

        // Feedback Label for live testing
        Label feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-font-size: 13px;");
        feedbackLabel.setVisible(false);

        // Test Connection Button
        Button testBtn = new Button("Test Connection");
        testBtn.setStyle("-fx-background-color: #353248; -fx-text-fill: #FFFFFF; -fx-background-radius: 6; -fx-padding: 7 14; -fx-cursor: hand; -fx-font-size: 13px;");
        testBtn.setOnAction(e -> {
            String enteredKey = passwordField.getText().trim();
            if (enteredKey.isEmpty()) {
                feedbackLabel.setText("Please enter an API key first.");
                feedbackLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 13px;");
                feedbackLabel.setVisible(true);
                return;
            }

            testBtn.setDisable(true);
            feedbackLabel.setText("Connecting to Google Gemini...");
            feedbackLabel.setStyle("-fx-text-fill: #E1B12C; -fx-font-size: 13px;");
            feedbackLabel.setVisible(true);

            // Apply selected model for test
            String selectedModel = parseSelectedModel(modelCombo.getValue());
            AIConfigManager.setModelName(selectedModel);

            AIConfigManager.testApiKey(enteredKey).thenAccept(result -> {
                Platform.runLater(() -> {
                    testBtn.setDisable(false);
                    if (result.success) {
                        feedbackLabel.setText("✓ " + result.message);
                        feedbackLabel.setStyle("-fx-text-fill: #4cd137; -fx-font-size: 13px; -fx-font-weight: bold;");
                        statusDot.setFill(Color.web("#4cd137"));
                        statusText.setText("Status: Verified & Connected (" + selectedModel + ")");
                        statusText.setStyle("-fx-text-fill: #A8E6CF; -fx-font-size: 13px; -fx-font-weight: bold;");
                    } else {
                        feedbackLabel.setText("✗ " + result.message);
                        feedbackLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 13px;");
                    }
                });
            });
        });

        HBox testRow = new HBox(12);
        testRow.setAlignment(Pos.CENTER_LEFT);
        testRow.getChildren().addAll(testBtn, feedbackLabel);

        root.getChildren().addAll(header, statusCard, modelBox, keyBox, testRow);
        pane.setContent(root);

        // Dialog Action Buttons
        ButtonType saveBtnType = new ButtonType("Save & Enable AI", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearBtnType = new ButtonType("Clear / Offline Mode", ButtonBar.ButtonData.OTHER);
        ButtonType cancelBtnType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        pane.getButtonTypes().addAll(saveBtnType, clearBtnType, cancelBtnType);

        Button saveBtn = (Button) pane.lookupButton(saveBtnType);
        if (saveBtn != null) {
            String defaultSave = "-fx-background-color: linear-gradient(to bottom, #E82E88, #C4166E); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;";
            String hoverSave = "-fx-background-color: linear-gradient(to bottom, #FF3E9E, #D61B78); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;";
            saveBtn.setStyle(defaultSave);
            saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(hoverSave));
            saveBtn.setOnMouseExited(e -> saveBtn.setStyle(defaultSave));
        }

        Button clearBtn = (Button) pane.lookupButton(clearBtnType);
        if (clearBtn != null) {
            String defaultClear = "-fx-background-color: #2C293A; -fx-text-fill: #E2DFEE; -fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #5A5474; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 14; -fx-cursor: hand;";
            String hoverClear = "-fx-background-color: #3E3952; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #7D769E; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 14; -fx-cursor: hand;";
            clearBtn.setStyle(defaultClear);
            clearBtn.setOnMouseEntered(e -> clearBtn.setStyle(hoverClear));
            clearBtn.setOnMouseExited(e -> clearBtn.setStyle(defaultClear));
        }

        Button cancelBtn = (Button) pane.lookupButton(cancelBtnType);
        if (cancelBtn != null) {
            String defaultCancel = "-fx-background-color: #2C293A; -fx-text-fill: #E2DFEE; -fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #5A5474; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 14; -fx-cursor: hand;";
            String hoverCancel = "-fx-background-color: #3E3952; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #7D769E; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 14; -fx-cursor: hand;";
            cancelBtn.setStyle(defaultCancel);
            cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(hoverCancel));
            cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(defaultCancel));
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                String key = passwordField.getText().trim();
                String selectedModel = parseSelectedModel(modelCombo.getValue());
                AIConfigManager.setModelName(selectedModel);
                AIConfigManager.saveApiKey(key);
                return key;
            } else if (dialogButton == clearBtnType) {
                AIConfigManager.saveApiKey("");
                return "";
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static String parseSelectedModel(String comboValue) {
        if (comboValue == null) return "gemini-3.5-flash-lite";
        if (comboValue.contains("3.5-flash-lite")) return "gemini-3.5-flash-lite";
        if (comboValue.contains("3.5-flash")) return "gemini-3.5-flash";
        if (comboValue.contains("3.8")) return "gemini-3.8-flash";
        return "gemini-3.5-flash-lite";
    }
}
