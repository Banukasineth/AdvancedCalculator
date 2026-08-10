package com.banukasineth.advancedcalculator.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.stage.StageStyle;

public class CalculatorApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Main.fxml"));

        // Increase default size for new advanced layout
        Scene scene = new Scene(loader.load(), 1050, 750);

        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        // Set transparent scene to avoid white background around the rounded corners and drop shadow
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        // Use TRANSPARENT style to remove OS window chrome and support transparent scene background
        stage.initStyle(StageStyle.TRANSPARENT);
        
        stage.setTitle("Advanced Calculator");
        stage.setScene(scene);

        // Window size limits 
        stage.setMinWidth(900);
        stage.setMinHeight(700);

        // Optional maximum size
        // stage.setMaxWidth(900);
        // stage.setMaxHeight(1000);

        stage.show();

        scene.getRoot().requestFocus();
    }
}