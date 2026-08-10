package com.banukasineth.advancedcalculator.app.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.util.Duration;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import com.banukasineth.advancedcalculator.app.service.AISolverService;


public class CalculatorController {

    @FXML
    private BorderPane rootPane;

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
    public void closeWindow(ActionEvent event) {
        Platform.exit();
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
                break;
            case "⌫":
                backspace();
                return; // backspace handles the update
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

    @FXML private VBox workingArea;
    @FXML private Region verticalDivider;
    @FXML private VBox solutionArea;

    private void showSolutionArea() {
        if (solutionArea.getPrefWidth() > 0) return;
        
        verticalDivider.setVisible(true);
        verticalDivider.setManaged(true);
        
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(350),
                        new KeyValue(solutionArea.prefWidthProperty(), 3000, Interpolator.EASE_BOTH),
                        new KeyValue(solutionArea.minWidthProperty(), 3000, Interpolator.EASE_BOTH),
                        new KeyValue(solutionArea.maxWidthProperty(), 3000, Interpolator.EASE_BOTH),
                        new KeyValue(workingArea.prefWidthProperty(), 0, Interpolator.EASE_BOTH),
                        new KeyValue(workingArea.minWidthProperty(), 0, Interpolator.EASE_BOTH),
                        new KeyValue(workingArea.maxWidthProperty(), 0, Interpolator.EASE_BOTH)
                )
        );
        timeline.play();
    }

    private void hideSolutionArea() {
        if (solutionArea.getPrefWidth() == 0) return;
        
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(350),
                        new KeyValue(solutionArea.prefWidthProperty(), 0, Interpolator.EASE_BOTH),
                        new KeyValue(solutionArea.minWidthProperty(), 0, Interpolator.EASE_BOTH),
                        new KeyValue(solutionArea.maxWidthProperty(), 0, Interpolator.EASE_BOTH),
                        new KeyValue(workingArea.prefWidthProperty(), 3000, Interpolator.EASE_BOTH),
                        new KeyValue(workingArea.minWidthProperty(), 3000, Interpolator.EASE_BOTH),
                        new KeyValue(workingArea.maxWidthProperty(), 3000, Interpolator.EASE_BOTH)
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
        if (currentLatexExpression.length() > 0) {
            String expr = currentLatexExpression.toString();
            
            if (expr.endsWith(" \\div ")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 6);
            } else if (expr.endsWith(" \\times ")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 8);
            } else if (expr.endsWith(" - ")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 3);
            } else if (expr.endsWith(" + ")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 3);
            } else if (expr.endsWith(" \\bmod ")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 7);
            } else if (expr.endsWith("^{n}")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 4);
            } else if (expr.endsWith("_{n}")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 4);
            } else if (expr.endsWith("\\pi")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 3);
            } else if (expr.endsWith("\\sqrt{")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 6);
            } else if (expr.endsWith("\\times 10^{")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 11);
            } else if (expr.endsWith("^{")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 2);
            } else if (expr.endsWith("^2")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 2);
            } else if (expr.endsWith("^{-1}")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 5);
            } else if (expr.endsWith("\\sin(") || expr.endsWith("\\cos(") || expr.endsWith("\\tan(") ||
                       expr.endsWith("\\csc(") || expr.endsWith("\\sec(") || expr.endsWith("\\cot(") ||
                       expr.endsWith("\\log(") || expr.endsWith("\\arg(")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 5);
            } else if (expr.endsWith("\\sinh(") || expr.endsWith("\\cosh(") || expr.endsWith("\\tanh(")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 6);
            } else if (expr.endsWith("\\ln(")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 4);
            } else if (expr.endsWith("\\operatorname{Re}(") || expr.endsWith("\\operatorname{Im}(")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 18);
            } else if (expr.endsWith("^{\\circ}")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 8);
            } else if (expr.endsWith("f(")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 2);
            } else if (expr.endsWith("\\overline{") || expr.endsWith("\\text{rad}")) {
                currentLatexExpression.setLength(currentLatexExpression.length() - 10);
            } else {
                // Simplified backspace: just removes the last character.
                currentLatexExpression.deleteCharAt(currentLatexExpression.length() - 1);
                
                // If the last character deleted leaves an exposed backslash (e.g. after deleting '%' from '\%'),
                // delete the backslash as well so it doesn't show up on screen.
                if (currentLatexExpression.length() > 0 && currentLatexExpression.charAt(currentLatexExpression.length() - 1) == '\\') {
                    currentLatexExpression.deleteCharAt(currentLatexExpression.length() - 1);
                }
            }
            
            setMathExpression(currentLatexExpression.toString());
        }

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

        Platform.runLater(() -> rootPane.requestFocus());

        if (workingArea != null && solutionArea != null) {
            Rectangle workingClip = new Rectangle();
            workingClip.widthProperty().bind(workingArea.widthProperty());
            workingClip.heightProperty().bind(workingArea.heightProperty());
            workingArea.setClip(workingClip);

            Rectangle solutionClip = new Rectangle();
            solutionClip.widthProperty().bind(solutionArea.widthProperty());
            solutionClip.heightProperty().bind(solutionArea.heightProperty());
            solutionArea.setClip(solutionClip);
        }

        try {
            // New Advanced Calculator doesn't use the old NavigationDrawer
            /*
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/NavigationDrawer.fxml")
            );
            ...
            */
            
            // Initialize KaTeX WebView for mathematical rendering
            if (mathWebView != null) {
                WebEngine webEngine = mathWebView.getEngine();
                String url = getClass().getResource("/html/math_display.html").toExternalForm();
                webEngine.load(url);
                
                // Wait for page to load before setting initial expression
                webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        setMathExpression("\\\\frac{d}{dx}(dy) + \\\\frac{d}{dy}(3y) = 5");
                    }
                });
            }
            
            // Initialize KaTeX WebView for AI solution
            if (solutionWebView != null) {
                WebEngine solEngine = solutionWebView.getEngine();
                String url = getClass().getResource("/html/solution_display.html").toExternalForm();
                solEngine.load(url);
            }
            
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