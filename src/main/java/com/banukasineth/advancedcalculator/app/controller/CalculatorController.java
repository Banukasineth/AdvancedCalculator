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
import javafx.scene.control.Label;

public class CalculatorController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label expressionLabel;

    @FXML
    private TextField display;

    private double firstNumber = 0;
    private String operator = "";
    private boolean startNewNumber = true;
    private double lastSecondNumber = 0;
    private String lastOperator = "";

    // =========================
    // Helper Methods
    // =========================

    private void inputNumber(String number) {

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
                formatNumber(first)
                        + " "
                        + operator
                        + " "
                        + formatNumber(second)
        );
        expression.getStyleClass().add("working-expression");

        // Result
        Label answer = new Label(
                "= " + formatNumber(result)
        );
        answer.getStyleClass().add("working-result");

        // Add labels to the card
        card.getChildren().addAll(expression, answer);

        // Add the card to the Working panel
        workingContainer.getChildren().add(card);
    }

    private void addToHistory(
            double first,
            String operator,
            double second,
            double result) {

        VBox card = new VBox();
        card.getStyleClass().add("history-card");

        Label expression = new Label(
                formatNumber(first)
                        + " "
                        + operator
                        + " "
                        + formatNumber(second)
        );

        expression.getStyleClass().add("history-expression");

        Label answer = new Label(
                "= " + formatNumber(result)
        );

        answer.getStyleClass().add("history-result");

        card.getChildren().addAll(expression, answer);

        historyContainer.getChildren().add(card);
    }

    private String formatNumber(double value) {

        if (value == (int) value) {
            return String.valueOf((int) value);
        }

        return String.valueOf(value);
    }

    private void selectOperator(String newOperator) {

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

        if (startNewNumber) {
            display.setText("0.");
            startNewNumber = false;
        } else if (!display.getText().contains(".")) {
            display.setText(display.getText() + ".");
        }
    }

    @FXML
    private void percentClicked() {

        double number = Double.parseDouble(display.getText());

        if (operator.equals("+") || operator.equals("-")) {
            number = firstNumber * number / 100;
        } else {
            number = number / 100;
        }

        updateDisplay(number);
        expressionLabel.setText(number + "%");
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

    @FXML
    public void initialize() {

        rootPane.setFocusTraversable(true);

        Platform.runLater(() -> rootPane.requestFocus());
    }

    @FXML
    private VBox workingContainer;

    @FXML
    private VBox historyContainer;
}