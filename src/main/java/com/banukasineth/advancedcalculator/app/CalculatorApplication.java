package com.banukasineth.advancedcalculator.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CalculatorApplication extends Application {

    @Override
    public void start(Stage stage) {

        // Create a label
        Label label = new Label("Welcome to Advanced Calculator!");

        // Create a layout and add the label
        StackPane root = new StackPane();
        root.getChildren().add(label);

        // Create a scene
        Scene scene = new Scene(root, 600, 400);

        // Configure the window (Stage)
        stage.setTitle("Advanced Calculator");
        stage.setScene(scene);

        // Display the window
        stage.show();
    }
}