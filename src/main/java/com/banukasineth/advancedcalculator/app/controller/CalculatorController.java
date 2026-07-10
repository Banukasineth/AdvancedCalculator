package com.banukasineth.advancedcalculator.app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

public class CalculatorController {

    @FXML
    private TextField display;

    private double firstNumber = 0;
    private String operator = "";
    private boolean startNewNumber = true;

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

    // =========================
    // Button Events
    // =========================

    @FXML
    private void numberClicked(ActionEvent event) {

        Button button = (Button) event.getSource();
        inputNumber(button.getText());
    }

    @FXML
    private void clearDisplay() {

        display.setText("0");

        firstNumber = 0;
        operator = "";
        startNewNumber = true;
    }

    @FXML
    private void backspace() {

        String text = display.getText();

        if (text.length() > 1) {
            display.setText(text.substring(0, text.length() - 1));
        } else {
            display.setText("0");
        }
    }

    private void selectOperator(String operator) {

        firstNumber = Double.parseDouble(display.getText());

        this.operator = operator;

        startNewNumber = true;
    }

    @FXML
    private void operatorClicked(ActionEvent event) {

        Button button = (Button) event.getSource();

        selectOperator(button.getText());
    }

    @FXML
    private void equalsClicked() {

        double secondNumber = Double.parseDouble(display.getText());

        double result = 0;

        switch (operator) {

            case "+":
                result = firstNumber + secondNumber;
                break;

            case "-":
                result = firstNumber - secondNumber;
                break;

            case "×":
                result = firstNumber * secondNumber;
                break;

            case "÷":
                result = firstNumber / secondNumber;
                break;
        }

        updateDisplay(result);

        startNewNumber = true;
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

        number = number / 100;

        updateDisplay(number);

        startNewNumber = true;
    }

    // =========================
    // Keyboard Events
    // =========================

    @FXML
    private void handleKeyPressed(KeyEvent event) {

        switch (event.getCode()) {

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

            case ADD:
                selectOperator("+");
                break;

            case SUBTRACT:
                selectOperator("-");
                break;

            case MULTIPLY:
                selectOperator("×");
                break;

            case DIVIDE:
                selectOperator("÷");
                break;

            case ENTER:
                equalsClicked();
                break;

            case DECIMAL:
                decimalClicked();
                break;

            case BACK_SPACE:
                backspace();
                break;

            case DELETE:
                clearDisplay();
                break;

            default:
                break;
        }
    }

    @FXML
    private BorderPane rootPane;

    @FXML
    public void initialize() {

        rootPane.setFocusTraversable(true);

        javafx.application.Platform.runLater(() -> rootPane.requestFocus());
    }

}