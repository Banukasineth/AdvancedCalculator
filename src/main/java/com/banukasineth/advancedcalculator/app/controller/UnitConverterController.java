package com.banukasineth.advancedcalculator.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class UnitConverterController {

    @FXML private ListView<String> menuList;
    @FXML private Label titleLabel;
    @FXML private TextField inputValueField;
    @FXML private TextField outputValueField;
    @FXML private ComboBox<String> fromUnitBox;
    @FXML private ComboBox<String> toUnitBox;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        String[] menuItems = {
            "💱 Currency", "🧊 Volume", "📏 Length", "⚖️ Weight and mass", 
            "🌡️ Temperature", "⚡ Energy", "📐 Area", "🏃 Speed", 
            "⏱️ Time", "🔋 Power", "💾 Data", "🎈 Pressure"
        };
        menuList.getItems().addAll(menuItems);

        menuList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Strip the emoji (first two characters usually, or split by space)
                String title = newVal.substring(newVal.indexOf(' ') + 1);
                titleLabel.setText(title);
                updateUnits(title);
            }
        });

        // Listeners for conversion
        inputValueField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("-?\\d*(\\.\\d*)?")) {
                inputValueField.setText(oldVal);
            } else {
                convertUnits();
            }
        });
        
        fromUnitBox.valueProperty().addListener((obs, oldVal, newVal) -> convertUnits());
        toUnitBox.valueProperty().addListener((obs, oldVal, newVal) -> convertUnits());

        // Select Length by default
        menuList.getSelectionModel().select("📏 Length");
    }

    private void updateUnits(String category) {
        fromUnitBox.getItems().clear();
        toUnitBox.getItems().clear();
        
        switch (category) {
            case "Length":
                String[] lengths = {"Meters (m)", "Centimeters (cm)", "Millimeters (mm)", "Kilometers (km)", "Inches (in)", "Feet (ft)", "Yards (yd)", "Miles (mi)"};
                fromUnitBox.getItems().addAll(lengths);
                toUnitBox.getItems().addAll(lengths);
                break;
            case "Weight and mass":
                String[] masses = {"Kilograms (kg)", "Grams (g)", "Milligrams (mg)", "Pounds (lb)", "Ounces (oz)"};
                fromUnitBox.getItems().addAll(masses);
                toUnitBox.getItems().addAll(masses);
                break;
            case "Temperature":
                String[] temps = {"Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"};
                fromUnitBox.getItems().addAll(temps);
                toUnitBox.getItems().addAll(temps);
                break;
            case "Data":
                String[] data = {"Bytes (B)", "Kilobytes (KB)", "Megabytes (MB)", "Gigabytes (GB)", "Terabytes (TB)"};
                fromUnitBox.getItems().addAll(data);
                toUnitBox.getItems().addAll(data);
                break;
            default:
                fromUnitBox.getItems().addAll("Coming Soon", "Not Available");
                toUnitBox.getItems().addAll("Coming Soon", "Not Available");
                break;
        }
        
        if (!fromUnitBox.getItems().isEmpty()) {
            fromUnitBox.setValue(fromUnitBox.getItems().get(0));
            toUnitBox.setValue(toUnitBox.getItems().get(1 < toUnitBox.getItems().size() ? 1 : 0));
        }
        convertUnits();
    }

    private void convertUnits() {
        if (inputValueField == null || outputValueField == null || titleLabel == null) return;
        
        String inputStr = inputValueField.getText();
        if (inputStr == null || inputStr.trim().isEmpty() || inputStr.equals("-")) {
            outputValueField.setText("");
            return;
        }
        
        try {
            double input = Double.parseDouble(inputStr);
            String type = titleLabel.getText();
            String from = fromUnitBox.getValue();
            String to = toUnitBox.getValue();
            
            if (type == null || from == null || to == null) return;
            
            double result = 0;
            
            if (type.equals("Length")) {
                double meters = input;
                switch (from) {
                    case "Centimeters (cm)": meters = input / 100.0; break;
                    case "Millimeters (mm)": meters = input / 1000.0; break;
                    case "Kilometers (km)": meters = input * 1000.0; break;
                    case "Inches (in)": meters = input * 0.0254; break;
                    case "Feet (ft)": meters = input * 0.3048; break;
                    case "Yards (yd)": meters = input * 0.9144; break;
                    case "Miles (mi)": meters = input * 1609.344; break;
                }
                
                switch (to) {
                    case "Meters (m)": result = meters; break;
                    case "Centimeters (cm)": result = meters * 100.0; break;
                    case "Millimeters (mm)": result = meters * 1000.0; break;
                    case "Kilometers (km)": result = meters / 1000.0; break;
                    case "Inches (in)": result = meters / 0.0254; break;
                    case "Feet (ft)": result = meters / 0.3048; break;
                    case "Yards (yd)": result = meters / 0.9144; break;
                    case "Miles (mi)": result = meters / 1609.344; break;
                }
            } else if (type.equals("Weight and mass")) {
                double kg = input;
                switch (from) {
                    case "Grams (g)": kg = input / 1000.0; break;
                    case "Milligrams (mg)": kg = input / 1000000.0; break;
                    case "Pounds (lb)": kg = input * 0.45359237; break;
                    case "Ounces (oz)": kg = input * 0.02834952; break;
                }
                
                switch (to) {
                    case "Kilograms (kg)": result = kg; break;
                    case "Grams (g)": result = kg * 1000.0; break;
                    case "Milligrams (mg)": result = kg * 1000000.0; break;
                    case "Pounds (lb)": result = kg / 0.45359237; break;
                    case "Ounces (oz)": result = kg / 0.02834952; break;
                }
            } else if (type.equals("Temperature")) {
                double celsius = input;
                if (from.equals("Fahrenheit (°F)")) celsius = (input - 32) * 5.0/9.0;
                else if (from.equals("Kelvin (K)")) celsius = input - 273.15;
                
                if (to.equals("Celsius (°C)")) result = celsius;
                else if (to.equals("Fahrenheit (°F)")) result = (celsius * 9.0/5.0) + 32;
                else if (to.equals("Kelvin (K)")) result = celsius + 273.15;
            } else if (type.equals("Data")) {
                double bytes = input;
                switch (from) {
                    case "Kilobytes (KB)": bytes = input * 1024; break;
                    case "Megabytes (MB)": bytes = input * 1024 * 1024; break;
                    case "Gigabytes (GB)": bytes = input * 1024 * 1024 * 1024; break;
                    case "Terabytes (TB)": bytes = input * 1024 * 1024 * 1024L * 1024L; break;
                }
                
                switch (to) {
                    case "Bytes (B)": result = bytes; break;
                    case "Kilobytes (KB)": result = bytes / 1024.0; break;
                    case "Megabytes (MB)": result = bytes / (1024.0 * 1024.0); break;
                    case "Gigabytes (GB)": result = bytes / (1024.0 * 1024.0 * 1024.0); break;
                    case "Terabytes (TB)": result = bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0); break;
                }
            } else {
                result = 0; // Not implemented yet
            }
            
            String res = String.valueOf(result);
            if (res.endsWith(".0")) {
                res = res.substring(0, res.length() - 2);
            }
            outputValueField.setText(res);
            
        } catch (NumberFormatException e) {
            outputValueField.setText("Invalid Input");
        }
    }

    @FXML
    private void handleTitleBarPressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleTitleBarDragged(MouseEvent event) {
        Stage stage = (Stage) menuList.getScene().getWindow();
        if (!stage.isMaximized()) {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        }
    }

    @FXML
    private void minimizeWindow() {
        Stage stage = (Stage) menuList.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = (Stage) menuList.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) menuList.getScene().getWindow();
        stage.close();
    }
}
