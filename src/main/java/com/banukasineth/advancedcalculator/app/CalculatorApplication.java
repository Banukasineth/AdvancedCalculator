package com.banukasineth.advancedcalculator.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CalculatorApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Main.fxml"));

        Scene scene = new Scene(loader.load(), 760, 520);

        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("Advanced Calculator");
        stage.setScene(scene);

        // -----------------------------
        // Window size limits (Initial size = minimum limit)
        // -----------------------------
        stage.setMinWidth(760);
        stage.setMinHeight(660);

        // Optional maximum size
        // stage.setMaxWidth(900);
        // stage.setMaxHeight(1000);

        stage.show();

        scene.getRoot().requestFocus();
    }
}