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
import javafx.scene.layout.Region;
import javafx.util.Duration;


public class CalculatorController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label expressionLabel;

    @FXML
    private TextField display;

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

        display.setText("0");
        expressionLabel.setText("");

        firstNumber = 0;
        operator = "";
        startNewNumber = true;
    }

    @FXML
    private void backspace() {

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

        if (event.getCode() == KeyCode.DIGIT8 && event.isShiftDown()) {
            selectOperator("×");
            return;
        }

        switch (event.getCode()) {

            // Numbers
            case DIGIT0:
            case NUMPAD0:
                inputNumber("0");
                break;

            case DIGIT1:
            case NUMPAD1:
                inputNumber("1");
                break;

            case DIGIT2:
            case NUMPAD2:
                inputNumber("2");
                break;

            case DIGIT3:
            case NUMPAD3:
                inputNumber("3");
                break;

            case DIGIT4:
            case NUMPAD4:
                inputNumber("4");
                break;

            case DIGIT5:

                if (event.isShiftDown()) {
                    percentClicked();
                } else {
                    inputNumber("5");
                }

                break;

            case NUMPAD5:
                inputNumber("5");
                break;

            case DIGIT6:
            case NUMPAD6:
                inputNumber("6");
                break;

            case DIGIT7:
            case NUMPAD7:
                inputNumber("7");
                break;

            case DIGIT8:
            case NUMPAD8:
                inputNumber("8");
                break;

            case DIGIT9:
            case NUMPAD9:
                inputNumber("9");
                break;

            // Operators (Numeric Keypad)
            case ADD:
                selectOperator("+");
                break;

            case SUBTRACT:
            case MINUS:
                selectOperator("-");
                break;

            case MULTIPLY:
                selectOperator("×");
                break;

            case DIVIDE:
            case SLASH:
                selectOperator("÷");
                break;

            // Main Keyboard "=" key
            case EQUALS:

                if (event.isShiftDown()) {
                    selectOperator("+");
                } else {
                    equalsClicked();
                }

                break;

            // Enter
            case ENTER:
                equalsClicked();
                break;

            // Decimal
            case DECIMAL:
            case PERIOD:
                decimalClicked();
                break;

            // Editing
            case BACK_SPACE:
                backspace();
                break;

            case DELETE:
                clearDisplay();
                break;

            default:
                break;
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

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/NavigationDrawer.fxml")
            );
            loader.setController(this);
            drawerContent = loader.load();
            drawerContent.setMinWidth(280);
            drawerContent.setPrefWidth(280);
            drawerContent.setMaxWidth(280);

            drawerContainer.setMinWidth(280);
            drawerContainer.setPrefWidth(280);
            drawerContainer.setMaxWidth(280);

            drawerContainer.getChildren().add(drawerContent);

            drawerContainer.setVisible(false);

        } catch (IOException e) {
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