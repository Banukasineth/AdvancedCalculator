package com.banukasineth.advancedcalculator.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class CalculatorController {

    @FXML
    private TextField display;

    private double firstNumber = 0;
    private String operator = "";
    private boolean startNewNumber = true;

    @FXML
    private void numberClicked(javafx.event.ActionEvent event) {

        Button button = (Button) event.getSource();
        String number = button.getText();

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

    @FXML
    private void clearDisplay() {
        display.setText("0");
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

    private void updateDisplay(double value) {

        if (value == (int) value) {
            display.setText(String.valueOf((int) value));
        } else {
            display.setText(String.valueOf(value));
        }
    }

    @FXML
    private void operatorClicked(javafx.event.ActionEvent event) {

        Button button = (Button) event.getSource();

        firstNumber = Double.parseDouble(display.getText());

        operator = button.getText();

        startNewNumber = true;
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

        if (!display.getText().contains(".")) {
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
}