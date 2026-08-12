package com.banukasineth.advancedcalculator.app.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.IOException;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.util.Duration;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import com.banukasineth.advancedcalculator.app.service.AISolverService;


public class CalculatorController {

    @FXML private BorderPane rootPane;

    @FXML private ComboBox<String> modeComboBox;
    @FXML private ComboBox<String> equationLibraryBox;
    @FXML private ComboBox<String> calculusLibraryBox;

    @FXML private GridPane basicKeypad;
    @FXML private GridPane advancedKeypad;
    @FXML private GridPane scientificKeypad;
    @FXML private GridPane programmerKeypad;

    private final Map<String, String> equationMap = new LinkedHashMap<>();
    private final Map<String, String> calculusMap = new LinkedHashMap<>();

    @FXML private HBox titleBar;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void handleTitleBarPressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    public void handleTitleBarDragged(MouseEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    public void minimizeWindow(ActionEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    public void maximizeWindow(ActionEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    public void closeWindow() {
        javafx.application.Platform.exit();
    }

    @FXML
    private VBox equationsListContainer;

    private List<TextField> equationFields = new ArrayList<>();
    private List<TextField> equationNameFields = new ArrayList<>();
    private TextField activeField;
    
    // Graph Colors
    private final String[] graphColors = {"#ff6b6b", "#4cd137", "#0984e3", "#fdcb6e", "#6c5ce7", "#e84393", "#00cec9", "#ff9f43", "#a29bfe", "#ff7675"};

    @FXML
    public void plotAllGraphs() {
        if (graphWebView != null && graphWebView.getEngine().getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
            try {
                graphWebView.getEngine().executeScript("clearGraph()");
            } catch (Exception ignored) {}
            for (int i = 0; i < equationFields.size(); i++) {
                TextField eqField = equationFields.get(i);
                TextField nameField = equationNameFields.get(i);
                if (eqField != null && !eqField.getText().trim().isEmpty()) {
                    String eq = eqField.getText().trim();
                    
                    // Replace unicode characters with math.js syntax
                    eq = eq.replace("²", "^2")
                           .replace("³", "^3")
                           .replace("π", "pi")
                           .replace("√", "sqrt")
                           .replace("∛", "cbrt")
                           .replace("×", "*")
                           .replace("÷", "/")
                           .replace("≤", "<=")
                           .replace("≥", ">=");
                           
                    // Replace |...| with abs(...) using Regex
                    eq = eq.replaceAll("\\|([^|]+)\\|", "abs($1)");
                           
                    String name = nameField != null ? nameField.getText().trim() : ("eq" + i);
                    String color = (String) eqField.getProperties().getOrDefault("color", graphColors[i % graphColors.length]);
                    Boolean hidden = (Boolean) eqField.getProperties().getOrDefault("hidden", false);
                    String style = (String) eqField.getProperties().getOrDefault("style", "solid");
                    
                    String script = String.format("plotEquation('%s', '%s', '%s', %b, '%s');", 
                                                  eq.replace("'", "\\'"), color, name, hidden, style);
                    graphWebView.getEngine().executeScript(script);
                }
            }
            try {
                graphWebView.getEngine().executeScript("renderAll();");
            } catch (Exception ignored) {}
        }
    }

    @FXML
    public void clearGraph() {
        if (graphWebView != null) {
            graphWebView.getEngine().executeScript("clearGraph()");
            graphWebView.getEngine().executeScript("renderAll()");
        }
        if (equationsListContainer != null) {
            equationsListContainer.getChildren().clear();
            equationFields.clear();
            equationNameFields.clear();
            addNewEquationCard();
        }
    }

    private Button createIconButton(String svgData, String tooltipText) {
        javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
        path.setContent(svgData);
        path.setFill(javafx.scene.paint.Color.WHITE);
        path.setScaleX(0.8);
        path.setScaleY(0.8);
        
        Button btn = new Button();
        btn.setGraphic(path);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4px;");
        btn.setFocusTraversable(false);
        btn.setMinSize(32, 32);
        btn.setMaxSize(32, 32);
        
        if (tooltipText != null) {
            javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText);
            tooltip.setShowDelay(javafx.util.Duration.millis(300));
            btn.setTooltip(tooltip);
        }
        
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-cursor: hand; -fx-padding: 4px; -fx-background-radius: 4px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4px;"));
        
        return btn;
    }

    private javafx.stage.Popup showLineOptionsPopup(Button anchor, TextField eqField, TextField nameField) {
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);
        
        VBox content = new VBox(10);
        content.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 10px; -fx-border-color: #555; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        
        HBox headerBox = new HBox();
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label titleLabel = new Label("Line Options");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 4px;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #ff4757; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 4px; -fx-background-radius: 2px;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 4px;"));
        closeBtn.setOnAction(e -> popup.hide());
        
        headerBox.getChildren().addAll(titleLabel, spacer, closeBtn);
        
        Label colorLabel = new Label("Color");
        colorLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        
        String initColorStr = (String) eqField.getProperties().getOrDefault("color", "#0078D7");
        
        javafx.scene.layout.GridPane colorPalette = new javafx.scene.layout.GridPane();
        colorPalette.setHgap(1);
        colorPalette.setVgap(1);
        colorPalette.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 4px; -fx-background-radius: 4px;");
        
        java.util.function.Function<javafx.scene.paint.Color, javafx.scene.shape.Rectangle> makeRect = c -> {
            javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(15, 15);
            rect.setFill(c);
            rect.setStroke(javafx.scene.paint.Color.TRANSPARENT);
            rect.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);
            rect.setStrokeWidth(1);
            rect.setCursor(javafx.scene.Cursor.HAND);
            
            rect.setOnMouseEntered(e -> rect.setStroke(javafx.scene.paint.Color.WHITE));
            rect.setOnMouseExited(e -> rect.setStroke(javafx.scene.paint.Color.TRANSPARENT));
            
            rect.setOnMouseClicked(e -> {
                String hex = String.format("#%02X%02X%02X", 
                    (int)(c.getRed() * 255), 
                    (int)(c.getGreen() * 255), 
                    (int)(c.getBlue() * 255));
                eqField.getProperties().put("color", hex);
                nameField.setStyle("-fx-text-fill: white; -fx-font-family: 'Cambria Math'; -fx-font-size: 14px; -fx-background-color: " + hex + "; -fx-padding: 4px 8px; -fx-background-radius: 4px; -fx-alignment: center; -fx-pref-width: 50px; -fx-min-width: 50px; -fx-max-width: 50px;");
                plotAllGraphs();
            });
            return rect;
        };
        
        // Row 0: Basic Colors
        javafx.scene.paint.Color[] basicColors = {
            javafx.scene.paint.Color.CYAN, javafx.scene.paint.Color.TEAL, javafx.scene.paint.Color.BLUE, javafx.scene.paint.Color.NAVY,
            javafx.scene.paint.Color.MAGENTA, javafx.scene.paint.Color.PURPLE, javafx.scene.paint.Color.RED, javafx.scene.paint.Color.MAROON,
            javafx.scene.paint.Color.YELLOW, javafx.scene.paint.Color.OLIVE, javafx.scene.paint.Color.GREEN, javafx.scene.paint.Color.LIME
        };
        for (int col = 0; col < 12; col++) colorPalette.add(makeRect.apply(basicColors[col]), col, 0);
        
        // Row 1: Grayscale
        for (int col = 0; col < 12; col++) {
            double v = 1.0 - (col / 11.0);
            colorPalette.add(makeRect.apply(javafx.scene.paint.Color.color(v, v, v)), col, 1);
        }
        
        // Rows 2-9: HSB Grid
        double[] gridHues = {180, 210, 240, 270, 0, 15, 30, 45, 60, 90, 120, 150};
        for (int r = 0; r < 8; r++) {
            double brightness = 0.2 + (r / 7.0) * 0.8;
            double saturation = r > 4 ? 1.0 - ((r - 4) / 3.0) * 0.7 : 1.0;
            for (int col = 0; col < 12; col++) {
                colorPalette.add(makeRect.apply(javafx.scene.paint.Color.hsb(gridHues[col], saturation, brightness)), col, r + 2);
            }
        }
        
        Label styleLabel = new Label("Style");
        styleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        
        HBox styleBox = new HBox(5);
        
        String currentStyle = (String) eqField.getProperties().getOrDefault("style", "solid");
        
        Button btnSolid = new Button("Solid");
        Button btnDashed = new Button("Dashed");
        Button btnDotted = new Button("Dotted");
        
        java.util.List<Button> styleBtns = java.util.Arrays.asList(btnSolid, btnDashed, btnDotted);
        
        for (Button btn : styleBtns) {
            String styleName = btn.getText().toLowerCase();
            boolean isSelected = styleName.equals(currentStyle);
            btn.setStyle("-fx-background-color: " + (isSelected ? "#0078D7" : "#3b3b3b") + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4px;");
            
            btn.setOnAction(e -> {
                eqField.getProperties().put("style", styleName);
                for (Button b : styleBtns) {
                    b.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4px;");
                }
                btn.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4px;");
                plotAllGraphs();
            });
        }
        
        styleBox.getChildren().addAll(btnSolid, btnDashed, btnDotted);
        
        content.getChildren().addAll(headerBox, colorLabel, colorPalette, styleLabel, styleBox);
        popup.getContent().add(content);
        
        // Ensure content is laid out to get its width
        content.applyCss();
        content.layout();
        
        javafx.geometry.Point2D location = anchor.localToScreen(0, 0);
        javafx.stage.Window mainWindow = anchor.getScene().getWindow();
        if (location != null && mainWindow != null) {
            double x = location.getX() - content.prefWidth(-1) / 2 + anchor.getWidth() / 2;
            double y = location.getY() + anchor.getHeight() + 5;
            
            // Strictly prevent the popup from going outside the main application window
            double mainX = mainWindow.getX();
            double mainY = mainWindow.getY();
            double mainWidth = mainWindow.getWidth();
            double mainHeight = mainWindow.getHeight();
            
            if (x + content.prefWidth(-1) > mainX + mainWidth - 10) {
                x = mainX + mainWidth - content.prefWidth(-1) - 10;
            }
            if (x < mainX + 10) {
                x = mainX + 10;
            }
            if (y + content.prefHeight(-1) > mainY + mainHeight - 10) {
                y = location.getY() - content.prefHeight(-1) - 5;
            }
            
            popup.setX(x);
            popup.setY(y);
        }
        
        popup.show(anchor.getScene().getWindow());
        return popup;
    }

    private void addNewEquationCard() {
        if (equationsListContainer == null) return;
        
        int index = equationFields.size();
        String defaultName = (index == 0) ? "ƒ(x)" : (index == 1) ? "g(x)" : (index == 2) ? "h(x)" : ("f" + (index+1) + "(x)");
        String initColor = graphColors[index % graphColors.length];
        
        HBox card = new HBox(8);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: transparent; -fx-padding: 6px; -fx-border-color: #333; -fx-border-width: 0 0 1px 0; -fx-margin: 0 0 5px 0;");
        
        TextField nameField = new TextField(defaultName);
        nameField.setStyle("-fx-text-fill: white; -fx-font-family: 'Cambria Math'; -fx-font-size: 14px; -fx-background-color: " + initColor + "; -fx-padding: 4px 8px; -fx-background-radius: 4px; -fx-alignment: center; -fx-pref-width: 50px; -fx-min-width: 50px; -fx-max-width: 50px;");
        
        // Allow renaming to update graph
        nameField.textProperty().addListener((obs, old, newVal) -> plotAllGraphs());
        
        TextField eqField = new TextField();
        eqField.setPromptText("Enter an expression");
        eqField.getStyleClass().add("math-input-field");
        eqField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px;");
        eqField.setPrefWidth(200);
        javafx.scene.layout.HBox.setHgrow(eqField, javafx.scene.layout.Priority.ALWAYS);
        eqField.getProperties().put("color", initColor);
        eqField.getProperties().put("style", "solid");
        
        Button hideBtn = createIconButton("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z", "Show/Hide");
        hideBtn.setOnAction(e -> {
            boolean isHidden = (boolean) eqField.getProperties().getOrDefault("hidden", false);
            eqField.getProperties().put("hidden", !isHidden);
            ((javafx.scene.shape.SVGPath) hideBtn.getGraphic()).setFill(javafx.scene.paint.Color.web(!isHidden ? "#555" : "white"));
            plotAllGraphs();
        });
        
        Button optionsBtn = createIconButton("M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12zm3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8zm5 0c-.83 0-1.5-.67-1.5-1.5S11.67 5 12.5 5s1.5.67 1.5 1.5S13.33 8 12.5 8zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z", "Line options");
        optionsBtn.setOnAction(e -> {
            if (optionsBtn.getUserData() != null) {
                ((javafx.stage.Popup) optionsBtn.getUserData()).hide();
                optionsBtn.setUserData(null);
            } else {
                javafx.stage.Popup popup = showLineOptionsPopup(optionsBtn, eqField, nameField);
                optionsBtn.setUserData(popup);
                popup.setOnHidden(ev -> optionsBtn.setUserData(null));
            }
        });
        
        Button aiBtn = createIconButton("M7.5 5.6L10 7 8.6 4.5 10 2 7.5 3.4 5 2l1.4 2.5L5 7l2.5-1.4zM19.5 15.4L22 14l-1.4 2.5L22 19l-2.5-1.4L17 19l1.4-2.5L17 14l2.5 1.4zM22 2l-2.5 1.4L17 2l1.4 2.5L17 7l2.5-1.4L22 7l-1.4-2.5L22 2zm-7.63 5.29c-.39-.39-1.02-.39-1.41 0L1.29 18.96c-.39.39-.39 1.02 0 1.41l3.34 3.34c.39.39 1.02.39 1.41 0L17.7 12.04c.39-.39.39-1.02 0-1.41l-3.33-3.34zm-1.89 4.31l-3.34-3.34 1.41-1.41 3.34 3.34-1.41 1.41z", "AI Graph Assistant");
        aiBtn.setOnAction(e -> showAIAssistantDialog(eqField.getText()));
        
        Button deleteBtn = createIconButton("M16 9v10H8V9h8m-1.5-6h-5l-1 1H5v2h14V4h-3.5l-1-1zM18 7H6v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7z", "Delete equation");
        ((javafx.scene.shape.SVGPath) deleteBtn.getGraphic()).setFill(javafx.scene.paint.Color.web("#ff6b6b"));
        deleteBtn.setOnAction(e -> {
            equationsListContainer.getChildren().remove(card);
            equationFields.remove(eqField);
            equationNameFields.remove(nameField);
            plotAllGraphs();
        });
        
        card.getChildren().addAll(nameField, eqField, aiBtn, hideBtn, optionsBtn, deleteBtn);
        
        // Track active field
        eqField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                activeField = eqField;
            }
        });
        
        // Auto-add new card and LIVE graph
        eqField.textProperty().addListener((obs, oldVal, newVal) -> {
            plotAllGraphs(); // Live graphing
            if (newVal != null && !newVal.isEmpty()) {
                if (equationsListContainer.getChildren().indexOf(card) == equationsListContainer.getChildren().size() - 1) {
                    addNewEquationCard();
                }
            }
        });
        
        equationFields.add(eqField);
        equationNameFields.add(nameField);
        equationsListContainer.getChildren().add(card);
        Platform.runLater(eqField::requestFocus);
        
        plotAllGraphs();
    }

    private void showAIAssistantDialog(String equation) {
        if (equation == null || equation.trim().isEmpty()) return;

        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("AI Graph Assistant");
        dialog.setHeaderText("Ask anything about: " + equation);

        javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialogPane.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: white; -fx-font-family: 'Segoe UI';");

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 10px;");
        
        TextField questionField = new TextField();
        questionField.setPromptText("E.g., What are the roots? What is the domain?");
        questionField.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white; -fx-prompt-text-fill: #888;");
        questionField.setPrefWidth(350);

        Label answerLabel = new Label();
        answerLabel.setWrapText(true);
        answerLabel.setStyle("-fx-text-fill: #00cec9; -fx-font-size: 14px;");
        answerLabel.setPrefWidth(350);
        answerLabel.setMinHeight(Region.USE_PREF_SIZE);

        Label qLabel = new Label("Your question:");
        qLabel.setStyle("-fx-text-fill: white;");

        content.getChildren().addAll(qLabel, questionField, answerLabel);
        dialogPane.setContent(content);

        Button askButton = (Button) dialogPane.lookupButton(javafx.scene.control.ButtonType.OK);
        askButton.setText("Ask ✨");
        askButton.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white; -fx-cursor: hand;");
        
        askButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String question = questionField.getText();
            if (question.trim().isEmpty()) {
                event.consume();
                return;
            }
            
            askButton.setDisable(true);
            answerLabel.setText("Thinking... ✨");
            event.consume(); 
            
            aiSolverService.askQuestion(equation, question).thenAccept(answer -> {
                Platform.runLater(() -> {
                    answerLabel.setText(answer);
                    askButton.setDisable(false);
                });
            });
        });

        dialog.showAndWait();
    }

    private void updateLiveMathDisplay(String eq, WebView targetWebView) {
    }

    @FXML
    private Label expressionLabel;

    @FXML
    private TextField display;
    
    @FXML
    private WebView mathWebView;

    @FXML
    private WebView solutionWebView;

    @FXML
    private VBox loadingBox;

    private AISolverService aiSolverService = new AISolverService();

    // Phase 3: Track the current LaTeX string for KaTeX
    private StringBuilder currentLatexExpression = new StringBuilder();
    private java.util.Stack<String> history = new java.util.Stack<>();

    /**
     * Updates the KaTeX math expression in the WebView.
     * Use double backslashes for LaTeX (e.g., "\\frac{1}{2}")
     */
    public void setMathExpression(String latex) {
        if (mathWebView != null) {
            String renderLatex = latex;
            // Auto-close open curly braces for KaTeX rendering
            int openCurly = 0;
            for (char c : renderLatex.toCharArray()) {
                if (c == '{') openCurly++;
                else if (c == '}') openCurly--;
            }
            while (openCurly > 0) {
                renderLatex += "}";
                openCurly--;
            }

            // Escape backslashes for JavaScript execution
            String jsSafeLatex = renderLatex.replace("\\", "\\\\");
            Platform.runLater(() -> {
                try {
                    mathWebView.getEngine().executeScript("updateMath('" + jsSafeLatex + "')");
                } catch (Exception e) {
                    System.err.println("Error executing KaTeX script: " + e.getMessage());
                }
            });
        }
    }

    @FXML
    private VBox drawer;

    @FXML
    private void toggleDrawer() {

        if (!drawerOpen) {

            drawerContainer.setVisible(true);

            drawerContent.setTranslateX(-280);
            drawerContent.setOpacity(0.0);

            TranslateTransition slideIn =
                    new TranslateTransition(Duration.millis(140), drawerContent);
            slideIn.setInterpolator(Interpolator.SPLINE(0.1, 0.9, 0.2, 1.0));
            slideIn.setToX(0);

            FadeTransition fadeIn =
                    new FadeTransition(Duration.millis(140), drawerContent);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);
            fadeIn.setToValue(1.0);

            slideIn.play();
            fadeIn.play();

            if (drawerBackdrop != null) {
                drawerBackdrop.setVisible(true);
                drawerBackdrop.setOpacity(0.0);
                FadeTransition backdropFade =
                        new FadeTransition(Duration.millis(140), drawerBackdrop);
                backdropFade.setInterpolator(Interpolator.EASE_OUT);
                backdropFade.setToValue(0.6);
                backdropFade.play();
            }

            if (appTitleLabel != null) {
                FadeTransition fadeTitle =
                        new FadeTransition(Duration.millis(140), appTitleLabel);
                fadeTitle.setInterpolator(Interpolator.EASE_OUT);
                fadeTitle.setToValue(0.0);
                fadeTitle.play();
            }

            drawerOpen = true;

        } else {

            closeDrawer();
        }
    }

    @FXML
    private Pane drawerContainer;

    @FXML
    private Region drawerBackdrop;

    @FXML
    private Label appTitleLabel;

    private double firstNumber = 0;
    private String operator = "";
    private boolean startNewNumber = true;
    private double lastSecondNumber = 0;
    private String lastOperator = "";
    private Region drawerContent;
    private boolean drawerOpen = false;

    // =========================
    // Graphing Logic
    // =========================

    @FXML private ToggleButton trigToggle;
    @FXML private ToggleButton ineqToggle;
    @FXML private ToggleButton funcToggle;
    @FXML private GridPane trigPopup;
    @FXML private GridPane ineqPopup;
    @FXML private GridPane funcPopup;

    @FXML
    public void toggleTrigPopup() {
        boolean selected = trigToggle.isSelected();
        resetPopups();
        if (selected) {
            trigToggle.setSelected(true);
            trigPopup.setVisible(true);
            trigPopup.setManaged(true);
        }
    }

    @FXML
    public void toggleIneqPopup() {
        boolean selected = ineqToggle.isSelected();
        resetPopups();
        if (selected) {
            ineqToggle.setSelected(true);
            ineqPopup.setVisible(true);
            ineqPopup.setManaged(true);
        }
    }

    @FXML
    public void toggleFuncPopup() {
        boolean selected = funcToggle.isSelected();
        resetPopups();
        if (selected) {
            funcToggle.setSelected(true);
            funcPopup.setVisible(true);
            funcPopup.setManaged(true);
        }
    }

    private void resetPopups() {
        if (trigToggle != null) trigToggle.setSelected(false);
        if (ineqToggle != null) ineqToggle.setSelected(false);
        if (funcToggle != null) funcToggle.setSelected(false);
        
        if (trigPopup != null) { trigPopup.setVisible(false); trigPopup.setManaged(false); }
        if (ineqPopup != null) { ineqPopup.setVisible(false); ineqPopup.setManaged(false); }
        if (funcPopup != null) { funcPopup.setVisible(false); funcPopup.setManaged(false); }
    }

    @FXML
    public void handleGraphingKey(ActionEvent event) {
        if (activeField == null && !equationFields.isEmpty()) {
            activeField = equationFields.get(0);
        }
        if (activeField == null) return;
        
        Button btn = (Button) event.getSource();
        String text = btn.getText();
        
        String insertText = "";
        switch (text) {
            case "sin": case "cos": case "tan":
                insertText = text + "(";
                break;
            case "log":
                insertText = "log10(";
                break;
            case "ln":
                insertText = "log(";
                break;
            case "hyp": case "sec": case "csc": case "cot":
                insertText = text + "(";
                break;
            case "√":
                insertText = "√(";
                break;
            case "∛x":
                insertText = "∛(";
                break;
            case "x²":
                insertText = "²";
                break;
            case "xʸ":
            case "10ˣ":
                insertText = "^";
                break;
            case "a/b":
            case "1/x":
                insertText = "/";
                break;
            case "π":
                insertText = "π";
                break;
            case "e":
                insertText = "e";
                break;
            case "÷":
                insertText = "÷";
                break;
            case "×":
                insertText = "×";
                break;
            case "⏎":
                plotAllGraphs();
                return;
            case "⌫":
                String currentText = activeField.getText();
                javafx.scene.control.IndexRange selection = activeField.getSelection();
                if (selection.getLength() > 0) {
                    activeField.replaceSelection("");
                } else {
                    int caretPos = activeField.getCaretPosition();
                    if (caretPos > 0 && currentText.length() > 0) {
                        String newText = currentText.substring(0, caretPos - 1) + currentText.substring(caretPos);
                        activeField.setText(newText);
                        activeField.positionCaret(caretPos - 1);
                    }
                }
                activeField.requestFocus();
                resetPopups();
                return;
            case "C":
                activeField.clear();
                resetPopups();
                return;
            case "±":
            case "+/-":
            case "(-)":
                insertText = "-";
                break;
            case "|x|":
                insertText = "|";
                break;
            case "⌊x⌋":
                insertText = "floor(";
                break;
            case "⌈x⌉":
                insertText = "ceil(";
                break;
            case "<":
                insertText = "<";
                break;
            case "≤":
                insertText = "<=";
                break;
            case "=":
                insertText = "=";
                break;
            case "≥":
                insertText = ">=";
                break;
            case ">":
                insertText = ">";
                break;
            default:
                insertText = text;
                break;
        }
        
        int pos = activeField.getCaretPosition();
        String currentText = activeField.getText();
        activeField.setText(currentText.substring(0, pos) + insertText + currentText.substring(pos));
        
        if (insertText.endsWith("(")) {
            activeField.positionCaret(pos + insertText.length());
        } else {
            activeField.positionCaret(pos + insertText.length());
        }
        
        activeField.requestFocus();
        resetPopups();
    }
    // =========================
    // Keypad Logic (Phase 3)
    // =========================

    @FXML
    public void handleAdvancedKey(ActionEvent event) {
        Button btn = (Button) event.getSource();
        processKeyInput(btn.getText());
    }

    private void processKeyInput(String text) {
        if (!text.equals("=")) {
            hideSolutionArea();
        }

        if (text.equals("⌫")) {
            backspace();
            return; // backspace handles the update
        }

        history.push(currentLatexExpression.toString());

        switch (text) {
            case "sin": case "cos": case "tan":
            case "csc": case "sec": case "cot":
            case "sinh": case "cosh": case "tanh":
            case "log": case "ln":
                currentLatexExpression.append("\\").append(text).append("(");
                break;
            case "π":
                currentLatexExpression.append("\\pi");
                break;
            case "mod":
                currentLatexExpression.append(" \\bmod ");
                break;
            case "↑n":
                currentLatexExpression.append("^{n}");
                break;
            case "n↓":
                currentLatexExpression.append("_{n}");
                break;
            case "√":
                currentLatexExpression.append("\\sqrt{");
                break;
            case "x²":
                currentLatexExpression.append("^2");
                break;
            case "xʸ":
                currentLatexExpression.append("^{");
                break;
            case "x⁻¹":
                currentLatexExpression.append("^{-1}");
                break;
            case "x10ⁿ":
                currentLatexExpression.append("\\times 10^{");
                break;
            case "x!":
                currentLatexExpression.append("!");
                break;
            case "|x|":
                currentLatexExpression.append("|");
                break;
            case "Re":
                currentLatexExpression.append("\\operatorname{Re}(");
                break;
            case "Im":
                currentLatexExpression.append("\\operatorname{Im}(");
                break;
            case "conj":
                currentLatexExpression.append("\\overline{");
                break;
            case "arg":
                currentLatexExpression.append("\\arg(");
                break;
            case "deg":
                currentLatexExpression.append("^{\\circ}");
                break;
            case "rad":
                currentLatexExpression.append("\\text{rad}");
                break;
            case "f(x)":
                currentLatexExpression.append("f(");
                break;
            case "A=B":
                currentLatexExpression.append("=");
                break;
            case "÷":
                currentLatexExpression.append(" \\div ");
                break;
            case "×":
                currentLatexExpression.append(" \\times ");
                break;
            case "-":
                currentLatexExpression.append(" - ");
                break;
            case "+":
                currentLatexExpression.append(" + ");
                break;
            case "%":
                // In LaTeX, % is a comment, so we must escape it as \%
                currentLatexExpression.append("\\%");
                break;
            case ")":
                java.util.Stack<Character> brackets = new java.util.Stack<>();
                for (char c : currentLatexExpression.toString().toCharArray()) {
                    if (c == '(') brackets.push('(');
                    else if (c == '{') brackets.push('{');
                    else if (c == ')') {
                        if (!brackets.isEmpty() && brackets.peek() == '(') brackets.pop();
                    }
                    else if (c == '}') {
                        if (!brackets.isEmpty() && brackets.peek() == '{') brackets.pop();
                    }
                }
                if (!brackets.isEmpty() && brackets.peek() == '{') {
                    currentLatexExpression.append("}");
                } else {
                    currentLatexExpression.append(")");
                }
                break;
            case "=":
                // Phase 4: Ask AI to solve it
                showSolutionArea();
                loadingBox.setVisible(true);
                solutionWebView.setVisible(false);
                aiSolverService.solveMathExpression(currentLatexExpression.toString())
                    .thenAccept(solutionLatex -> {
                        Platform.runLater(() -> {
                            loadingBox.setVisible(false);
                            solutionWebView.setVisible(true);
                            String jsSafeLatex = solutionLatex.replace("\\", "\\\\").replace("\n", "\\n").replace("'", "\\'");
                            try {
                                solutionWebView.getEngine().executeScript("updateSolution('" + jsSafeLatex + "')");
                            } catch (Exception e) {
                                System.err.println("Error rendering solution: " + e.getMessage());
                            }
                        });
                    });
                history.pop(); // Revert the history push since '=' doesn't modify the expression
                break;
            default:
                // For numbers or standard symbols like (, )
                currentLatexExpression.append(text);
                break;
        }
        
        // Instantly update the KaTeX display
        setMathExpression(currentLatexExpression.toString());
    }

    // =========================
    // Helper Methods
    // =========================

    private <T> void setupComboBoxPromptText(ComboBox<T> comboBox) {
        if (comboBox == null) return;
        comboBox.setButtonCell(new javafx.scene.control.ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(comboBox.getPromptText());
                    setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7);"); // Dimmer for prompt
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: white;");
                }
            }
        });
    }

    private void setupWebView(WebView webView, String htmlFile) {
        if (webView != null) {
            WebEngine engine = webView.getEngine();
            String url = getClass().getResource("/html/" + htmlFile).toExternalForm();
            if (htmlFile.equals("graph_display.html")) {
                engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        plotAllGraphs();
                    }
                });
            }
            engine.load(url);
        }
    }

    private void inputNumber(String number) {

        if (display.getText().equals("Error")) {
            display.setText(number);
            startNewNumber = false;
            return;
        }

        if (startNewNumber) {
            display.setText(number);
            startNewNumber = false;
        } else {
            if (display.getText().equals("0")) {
                display.setText(number);
            } else {
                display.setText(display.getText() + number);
            }
        }
    }

    private void updateDisplay(double value) {

        if (value == (int) value) {
            display.setText(String.valueOf((int) value));
        } else {
            display.setText(String.valueOf(value));
        }
    }

    private void addToWorkingArea(
            double first,
            String operator,
            double second,
            double result) {

        // Card container
        VBox card = new VBox();
        card.getStyleClass().add("working-card");

        // Expression
        Label expression = new Label(
                formatExpression(first, operator, second)
        );
        expression.getStyleClass().add("working-expression");

        // Result
        Label answer = new Label(
                "= " + formatNumber(result)
        );
        answer.getStyleClass().add("working-result");

        // Add labels to the card
        card.getChildren().addAll(expression, answer);
        card.setOnMouseClicked(e -> {
            display.setText(formatNumber(result));
            startNewNumber = false;
        });
        card.setStyle("-fx-cursor: hand;");

        // Add the card to the Working panel at top
        workingContainer.getChildren().add(0, card);
    }

    private void addToHistory(
            double first,
            String operator,
            double second,
            double result) {

        VBox card = new VBox();
        card.getStyleClass().add("history-card");

        Label expression = new Label(
                formatExpression(first, operator, second)
        );

        expression.getStyleClass().add("history-expression");

        Label answer = new Label(
                "= " + formatNumber(result)
        );

        answer.getStyleClass().add("history-result");

        card.getChildren().addAll(expression, answer);
        card.setOnMouseClicked(e -> {
            display.setText(formatNumber(result));
            startNewNumber = false;
        });
        card.setStyle("-fx-cursor: hand;");

        historyContainer.getChildren().add(0, card);
    }

    private String formatExpression(double first, String op, double second) {
        switch (op) {
            case "sin":
            case "cos":
            case "tan":
            case "log":
            case "ln":
            case "√":
            case "|x|":
                return op + "(" + formatNumber(first) + ")";
            case "x²":
                return formatNumber(first) + "²";
            case "x³":
                return formatNumber(first) + "³";
            case "1/x":
                return "1/(" + formatNumber(first) + ")";
            case "n!":
                return formatNumber(first) + "!";
            default:
                return formatNumber(first) + " " + op + " " + formatNumber(second);
        }
    }

    private String formatNumber(double value) {

        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "Error";
        }

        double rounded = Math.round(value * 1e11) / 1e11;

        if (rounded == (long) rounded && Math.abs(rounded) < 1e15) {
            return String.valueOf((long) rounded);
        }

        return String.valueOf(rounded);
    }

    private void selectOperator(String newOperator) {

        if (display.getText().equals("Error")) {
            clearDisplay();
            return;
        }

        double currentNumber = Double.parseDouble(display.getText());

        // If an operator is already selected and the user
        // hasn't typed the next number yet, just replace it.
        if (!operator.isEmpty() && startNewNumber) {

            operator = newOperator;

            expressionLabel.setText(
                    formatNumber(firstNumber) + " " + operator
            );

            return;
        }

        // Chain calculations
        if (!operator.isEmpty()) {

            try {

                double result = calculate(firstNumber, currentNumber, operator);

                updateDisplay(result);

                firstNumber = result;

            } catch (ArithmeticException ex) {

                display.setText("Error");
                operator = "";
                startNewNumber = true;
                return;
            }

        } else {

            firstNumber = currentNumber;

        }

        operator = newOperator;

        expressionLabel.setText(
                formatNumber(firstNumber) + " " + operator
        );

        startNewNumber = true;
    }

    private double calculate(double first, double second, String operator) {

        switch (operator) {

            case "+":
                return first + second;

            case "-":
                return first - second;

            case "×":
                return first * second;

            case "÷":

                if (second == 0) {
                    throw new ArithmeticException("Cannot divide by zero");
                }

                return first / second;

            default:
                return second;
        }
    }

    // =========================
    // Dynamic Layout Animation
    // =========================

    @FXML private HBox displaySplitArea;
    @FXML private VBox workingArea;
    @FXML private VBox solutionArea;
    @FXML private Region verticalDivider;
    
    @FXML private VBox standardLayout;
    @FXML private HBox graphingLayout;
    @FXML private WebView graphWebView;
    @FXML private TextField equation1Field;
    @FXML private TextField equation2Field;
    @FXML private ComboBox<String> graphModeComboBox;

    private DoubleProperty workingAreaFraction = new SimpleDoubleProperty(1.0);

    private void showSolutionArea() {
        if (workingAreaFraction.get() == 0.0) return;
        
        verticalDivider.setVisible(true);
        verticalDivider.setManaged(true);
        
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(350),
                        new KeyValue(workingAreaFraction, 0.0, Interpolator.EASE_BOTH)
                )
        );
        timeline.play();
    }

    private void hideSolutionArea() {
        if (workingAreaFraction.get() == 1.0) return;
        
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(350),
                        new KeyValue(workingAreaFraction, 1.0, Interpolator.EASE_BOTH)
                )
        );
        timeline.setOnFinished(e -> {
            verticalDivider.setVisible(false);
            verticalDivider.setManaged(false);
        });
        timeline.play();
    }

    // =========================
    // Button Events
    // =========================

    @FXML
    private void numberClicked(ActionEvent event) {

        Button button = (Button) event.getSource();
        inputNumber(button.getText());
    }

    @FXML
    private void operatorClicked(ActionEvent event) {

        Button button = (Button) event.getSource();
        selectOperator(button.getText());
    }

    @FXML
    private void clearDisplay() {
        hideSolutionArea();
        history.clear();
        currentLatexExpression.setLength(0);
        setMathExpression("");

        display.setText("0");
        expressionLabel.setText("");

        firstNumber = 0;
        operator = "";
        startNewNumber = true;
    }

    @FXML
    private void backspace() {
        if (!history.isEmpty()) {
            currentLatexExpression.setLength(0);
            currentLatexExpression.append(history.pop());
        } else {
            currentLatexExpression.setLength(0);
        }
        setMathExpression(currentLatexExpression.toString());

        String text = display.getText();

        if (text.equals("Error")) {
            clearDisplay();
            return;
        }

        if (text.length() > 1) {
            display.setText(text.substring(0, text.length() - 1));
        } else {
            display.setText("0");
        }
    }

    @FXML
    private void decimalClicked() {

        if (display.getText().equals("Error")) {
            clearDisplay();
            return;
        }

        if (startNewNumber) {
            display.setText("0.");
            startNewNumber = false;
        } else if (!display.getText().contains(".")) {
            display.setText(display.getText() + ".");
        }
    }

    @FXML
    private void percentClicked() {

        if (display.getText().equals("Error")) {
            clearDisplay();
            return;
        }

        double number = Double.parseDouble(display.getText());

        if (operator.equals("+") || operator.equals("-")) {
            number = firstNumber * number / 100;
        } else {
            number = number / 100;
        }

        updateDisplay(number);
        expressionLabel.setText(formatNumber(number) + "%");
    }

    @FXML
    private void equalsClicked() {

        try {

            double secondNumber;

            // If '=' is pressed repeatedly
            if (operator.isEmpty()) {

                if (lastOperator.isEmpty()) {
                    return;
                }

                secondNumber = lastSecondNumber;

                double result = calculate(firstNumber, secondNumber, lastOperator);

                addToWorkingArea(
                        firstNumber,
                        lastOperator,
                        secondNumber,
                        result
                );

                addToHistory(
                        firstNumber,
                        lastOperator,
                        secondNumber,
                        result
                );

                updateDisplay(result);

                firstNumber = result;

                return;
            }

            // First '=' press
            secondNumber = Double.parseDouble(display.getText());

            double result = calculate(firstNumber, secondNumber, operator);

            addToWorkingArea(
                    firstNumber,
                    operator,
                    secondNumber,
                    result
            );

            addToHistory(
                    firstNumber,
                    operator,
                    secondNumber,
                    result
            );

            updateDisplay(result);

            // Save last operation
            lastSecondNumber = secondNumber;
            lastOperator = operator;

            firstNumber = result;
            operator = "";
            expressionLabel.setText("");
            startNewNumber = true;

        } catch (ArithmeticException ex) {

            display.setText("Error");

            operator = "";
            lastOperator = "";
            startNewNumber = true;
        }
    }

    // =========================
    // Keyboard Events
    // =========================

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (event.isShiftDown()) {
            switch (event.getCode()) {
                case DIGIT8: processKeyInput("×"); return;
                case EQUALS: processKeyInput("+"); return;
                case DIGIT5: processKeyInput("%"); return;
                case DIGIT9: processKeyInput("("); return;
                case DIGIT0: processKeyInput(")"); return;
            }
        }

        switch (event.getCode()) {
            case DIGIT0: case NUMPAD0: processKeyInput("0"); break;
            case DIGIT1: case NUMPAD1: processKeyInput("1"); break;
            case DIGIT2: case NUMPAD2: processKeyInput("2"); break;
            case DIGIT3: case NUMPAD3: processKeyInput("3"); break;
            case DIGIT4: case NUMPAD4: processKeyInput("4"); break;
            case DIGIT5: case NUMPAD5: processKeyInput("5"); break;
            case DIGIT6: case NUMPAD6: processKeyInput("6"); break;
            case DIGIT7: case NUMPAD7: processKeyInput("7"); break;
            case DIGIT8: case NUMPAD8: processKeyInput("8"); break;
            case DIGIT9: case NUMPAD9: processKeyInput("9"); break;
            case ADD: processKeyInput("+"); break;
            case SUBTRACT: case MINUS: processKeyInput("-"); break;
            case MULTIPLY: processKeyInput("×"); break;
            case DIVIDE: case SLASH: processKeyInput("÷"); break;
            case EQUALS: case ENTER: processKeyInput("="); break;
            case DECIMAL: case PERIOD: processKeyInput("."); break;
            case BACK_SPACE: processKeyInput("⌫"); break;
            case DELETE: clearDisplay(); break;
            
            // Letters for algebra/variables
            case A: processKeyInput("a"); break;
            case B: processKeyInput("b"); break;
            case C: processKeyInput("c"); break;
            case D: processKeyInput("d"); break;
            case E: processKeyInput("e"); break;
            case F: processKeyInput("f"); break;
            case G: processKeyInput("g"); break;
            case H: processKeyInput("h"); break;
            case I: processKeyInput("i"); break;
            case J: processKeyInput("j"); break;
            case K: processKeyInput("k"); break;
            case L: processKeyInput("l"); break;
            case M: processKeyInput("m"); break;
            case N: processKeyInput("n"); break;
            case O: processKeyInput("o"); break;
            case P: processKeyInput("p"); break;
            case Q: processKeyInput("q"); break;
            case R: processKeyInput("r"); break;
            case S: processKeyInput("s"); break;
            case T: processKeyInput("t"); break;
            case U: processKeyInput("u"); break;
            case V: processKeyInput("v"); break;
            case W: processKeyInput("w"); break;
            case X: processKeyInput("x"); break;
            case Y: processKeyInput("y"); break;
            case Z: processKeyInput("z"); break;
            
            default: break;
        }
        event.consume();
    }

    // =========================
    // Initialize
    // =========================

    private boolean isInitialized = false;

    @FXML
    public void initialize() {
        if (isInitialized) {
            return; // Prevent infinite recursion when FXMLLoader.load() invokes initialize() on this controller again
        }
        isInitialized = true;

        rootPane.setFocusTraversable(true);

        Platform.runLater(() -> {
            rootPane.requestFocus();
            if (rootPane.getScene() != null) {
                rootPane.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown() && "Graphing".equals(modeComboBox.getValue()) && graphWebView != null) {
                        if (event.getCode() == javafx.scene.input.KeyCode.EQUALS || event.getCode() == javafx.scene.input.KeyCode.ADD) {
                            graphWebView.getEngine().executeScript("zoom(0.8)");
                            event.consume();
                        } else if (event.getCode() == javafx.scene.input.KeyCode.MINUS || event.getCode() == javafx.scene.input.KeyCode.SUBTRACT) {
                            graphWebView.getEngine().executeScript("zoom(1.25)");
                            event.consume();
                        } else if (event.getCode() == javafx.scene.input.KeyCode.R || event.getCode() == javafx.scene.input.KeyCode.DIGIT0 || event.getCode() == javafx.scene.input.KeyCode.NUMPAD0) {
                            graphWebView.getEngine().executeScript("resetZoom()");
                            event.consume();
                        }
                    }
                });
            }
        });

        if (workingArea != null && solutionArea != null && displaySplitArea != null) {
            Rectangle workingClip = new Rectangle();
            workingClip.widthProperty().bind(workingArea.widthProperty());
            workingClip.heightProperty().bind(workingArea.heightProperty());
            workingArea.setClip(workingClip);

            Rectangle solutionClip = new Rectangle();
            solutionClip.widthProperty().bind(solutionArea.widthProperty());
            solutionClip.heightProperty().bind(solutionArea.heightProperty());
            solutionArea.setClip(solutionClip);
            
            workingArea.setMinWidth(0);
            solutionArea.setMinWidth(0);
            workingArea.maxWidthProperty().bind(displaySplitArea.widthProperty().multiply(workingAreaFraction));
            workingArea.prefWidthProperty().bind(workingArea.maxWidthProperty());
            
            // For solution area, fraction is (1.0 - workingAreaFraction)
            solutionArea.maxWidthProperty().bind(displaySplitArea.widthProperty().multiply(workingAreaFraction.subtract(1).multiply(-1)));
            solutionArea.prefWidthProperty().bind(solutionArea.maxWidthProperty());
        }

        // Initialize ComboBoxes
        if (modeComboBox != null) {
            modeComboBox.getItems().addAll("Basic", "Advanced", "Scientific", "Programmer", "Graphing");
            modeComboBox.setValue("Advanced");
            
            modeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) return;
                
                if (newVal.equals("Graphing")) {
                    if (standardLayout != null) { standardLayout.setVisible(false); standardLayout.setManaged(false); }
                    if (graphingLayout != null) { graphingLayout.setVisible(true); graphingLayout.setManaged(true); }
                } else {
                    if (graphingLayout != null) { graphingLayout.setVisible(false); graphingLayout.setManaged(false); }
                    if (standardLayout != null) { standardLayout.setVisible(true); standardLayout.setManaged(true); }
                    
                    // Hide all keypads
                    if (basicKeypad != null) { basicKeypad.setVisible(false); basicKeypad.setManaged(false); }
                    if (advancedKeypad != null) { advancedKeypad.setVisible(false); advancedKeypad.setManaged(false); }
                    if (scientificKeypad != null) { scientificKeypad.setVisible(false); scientificKeypad.setManaged(false); }
                    if (programmerKeypad != null) { programmerKeypad.setVisible(false); programmerKeypad.setManaged(false); }
                    
                    // Show selected keypad
                    switch (newVal) {
                        case "Basic":
                            if (basicKeypad != null) { basicKeypad.setVisible(true); basicKeypad.setManaged(true); }
                            break;
                        case "Advanced":
                            if (advancedKeypad != null) { advancedKeypad.setVisible(true); advancedKeypad.setManaged(true); }
                            break;
                        case "Scientific":
                            if (scientificKeypad != null) { scientificKeypad.setVisible(true); scientificKeypad.setManaged(true); }
                            break;
                        case "Programmer":
                            if (programmerKeypad != null) { programmerKeypad.setVisible(true); programmerKeypad.setManaged(true); }
                            break;
                    }
                }
            });
        }
        
        if (graphModeComboBox != null) {
            graphModeComboBox.getItems().addAll("Basic", "Advanced", "Scientific", "Programmer", "Graphing");
            graphModeComboBox.setValue("Graphing");
            graphModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals("Graphing") && modeComboBox != null) {
                    modeComboBox.setValue(newVal);
                }
            });
        }

        // Initialize WebViews
        setupWebView(mathWebView, "math_display.html");
        setupWebView(solutionWebView, "solution_display.html");
        if (graphWebView != null) {
            setupWebView(graphWebView, "graph_display.html");
        }
        
        if (equationsListContainer != null) {
            addNewEquationCard();
        }

        // Set up ComboBox Prompt Text fix
        setupComboBoxPromptText(modeComboBox);
        setupComboBoxPromptText(equationLibraryBox);
        setupComboBoxPromptText(calculusLibraryBox);

        // Hide solution area initially
        hideSolutionArea();

        if (equationLibraryBox != null) {
            equationMap.put("Quadratic Formula", "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}");
            equationMap.put("Pythagorean Theorem", "a^2 + b^2 = c^2");
            equationMap.put("Newton's 2nd Law", "F = ma");
            equationMap.put("Euler's Identity", "e^{i\\pi} + 1 = 0");
            equationMap.put("Area of Circle", "A = \\pi r^2");
            equationMap.put("Volume of Sphere", "V = \\frac{4}{3} \\pi r^3");
            equationMap.put("Trigonometric Identity", "\\sin^2\\theta + \\cos^2\\theta = 1");
            equationMap.put("Mass-Energy Equivalence", "E = mc^2");
            equationMap.put("Schrödinger Equation", "i\\hbar\\frac{\\partial}{\\partial t}\\Psi = \\hat{H}\\Psi");
            equationMap.put("Ideal Gas Law", "PV = nRT");
            equationMap.put("Law of Cosines", "c^2 = a^2 + b^2 - 2ab\\cos(C)");
            
            equationLibraryBox.getItems().addAll(equationMap.keySet());
            equationLibraryBox.setOnAction(e -> {
                String selected = equationLibraryBox.getValue();
                if (selected != null && equationMap.containsKey(selected)) {
                    history.push(currentLatexExpression.toString());
                    currentLatexExpression.setLength(0);
                    currentLatexExpression.append(equationMap.get(selected));
                    setMathExpression(currentLatexExpression.toString());
                    Platform.runLater(() -> {
                        equationLibraryBox.getSelectionModel().clearSelection();
                        equationLibraryBox.setValue(null);
                    });
                }
            });
        }

        if (calculusLibraryBox != null) {
            calculusMap.put("Derivative (d/dx)", "\\frac{d}{dx}(");
            calculusMap.put("2nd Derivative", "\\frac{d^2}{dx^2}(");
            calculusMap.put("Partial Derivative", "\\frac{\\partial}{\\partial x}(");
            calculusMap.put("Indefinite Integral", "\\int dx");
            calculusMap.put("Definite Integral", "\\int_{a}^{b} dx");
            calculusMap.put("Double Integral", "\\iint dx dy");
            calculusMap.put("Limit (x→0)", "\\lim_{x \\to 0}(");
            calculusMap.put("Limit (x→∞)", "\\lim_{x \\to \\infty}(");
            calculusMap.put("Summation", "\\sum_{i=1}^{n}(");
            calculusMap.put("Product", "\\prod_{i=1}^{n}(");
            
            calculusLibraryBox.getItems().addAll(calculusMap.keySet());
            calculusLibraryBox.setOnAction(e -> {
                String selected = calculusLibraryBox.getValue();
                if (selected != null && calculusMap.containsKey(selected)) {
                    history.push(currentLatexExpression.toString());
                    currentLatexExpression.append(calculusMap.get(selected));
                    setMathExpression(currentLatexExpression.toString());
                    Platform.runLater(() -> {
                        calculusLibraryBox.getSelectionModel().clearSelection();
                        calculusLibraryBox.setValue(null);
                    });
                }
            });
        }

        try {
            // New Advanced Calculator doesn't use the old NavigationDrawer
            /*
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/NavigationDrawer.fxml")
            );
            ...
            */
            
            // Wait for page to load before setting initial expression
            mathWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    setMathExpression("\\\\frac{d}{dx}(dy) + \\\\frac{d}{dy}(3y) = 5");
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private VBox workingContainer;

    @FXML
    private VBox historyContainer;

    @FXML
    private StackPane keypadContainer;

    @FXML
    private GridPane buttonGrid;

    @FXML
    private Button standardButton;

    @FXML
    private Button scientificButton;

    @FXML
    private void closeDrawer() {
        if (drawerOpen && drawerContent != null) {
            TranslateTransition slideOut =
                    new TranslateTransition(Duration.millis(120), drawerContent);
            slideOut.setInterpolator(Interpolator.SPLINE(0.4, 0.0, 1.0, 1.0));
            slideOut.setToX(-280);

            FadeTransition fadeOut =
                    new FadeTransition(Duration.millis(120), drawerContent);
            fadeOut.setInterpolator(Interpolator.EASE_IN);
            fadeOut.setToValue(0.0);

            if (drawerBackdrop != null) {
                FadeTransition backdropFadeOut =
                        new FadeTransition(Duration.millis(120), drawerBackdrop);
                backdropFadeOut.setInterpolator(Interpolator.EASE_IN);
                backdropFadeOut.setToValue(0.0);
                backdropFadeOut.setOnFinished(e -> drawerBackdrop.setVisible(false));
                backdropFadeOut.play();
            }

            if (appTitleLabel != null) {
                FadeTransition fadeTitle =
                        new FadeTransition(Duration.millis(120), appTitleLabel);
                fadeTitle.setInterpolator(Interpolator.EASE_IN);
                fadeTitle.setToValue(1.0);
                fadeTitle.play();
            }

            slideOut.setOnFinished(e -> drawerContainer.setVisible(false));
            slideOut.play();
            fadeOut.play();

            drawerOpen = false;
        }
    }

    @FXML
    private void showScientificMode() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ScientificKeypad.fxml")
            );
            loader.setController(this);
            Parent scientificKeypad = loader.load();

            keypadContainer.getChildren().setAll(scientificKeypad);
            closeDrawer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showStandardMode() {

        keypadContainer.getChildren().setAll(buttonGrid);
        closeDrawer();
    }

    @FXML
    private void scientificUnaryClicked(ActionEvent event) {
        Button button = (Button) event.getSource();
        String func = button.getText();

        if (display.getText().equals("Error")) {
            clearDisplay();
            return;
        }

        try {
            double current = Double.parseDouble(display.getText());
            double result = 0;
            switch (func) {
                case "sin":
                    result = Math.sin(Math.toRadians(current));
                    break;
                case "cos":
                    result = Math.cos(Math.toRadians(current));
                    break;
                case "tan":
                    if (Math.abs(current % 180) == 90) {
                        throw new ArithmeticException("Undefined");
                    }
                    result = Math.tan(Math.toRadians(current));
                    break;
                case "log":
                    if (current <= 0) throw new ArithmeticException("Invalid input");
                    result = Math.log10(current);
                    break;
                case "ln":
                    if (current <= 0) throw new ArithmeticException("Invalid input");
                    result = Math.log(current);
                    break;
                case "√":
                    if (current < 0) throw new ArithmeticException("Invalid input");
                    result = Math.sqrt(current);
                    break;
                case "x²":
                    result = current * current;
                    break;
                case "x³":
                    result = current * current * current;
                    break;
                case "1/x":
                    if (current == 0) throw new ArithmeticException("Cannot divide by zero");
                    result = 1.0 / current;
                    break;
                case "|x|":
                    result = Math.abs(current);
                    break;
                case "n!":
                    if (current < 0 || current != (int) current || current > 170) {
                        throw new ArithmeticException("Invalid input");
                    }
                    result = factorial((int) current);
                    break;
                default:
                    result = current;
                    break;
            }

            addToWorkingArea(current, func, 0, result);
            addToHistory(current, func, 0, result);
            updateDisplay(result);
            if (startNewNumber) {
                firstNumber = result;
            }
        } catch (Exception ex) {
            display.setText("Error");
            operator = "";
            startNewNumber = true;
        }
    }

    private double factorial(int n) {
        double fact = 1;
        for (int i = 2; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }

    @FXML
    private void constantClicked(ActionEvent event) {
        Button button = (Button) event.getSource();
        String constant = button.getText();
        double val = 0;
        if (constant.equals("π")) {
            val = Math.PI;
        } else if (constant.equals("e")) {
            val = Math.E;
        }
        updateDisplay(val);
        startNewNumber = false;
    }

}