package com.banukasineth.advancedcalculator.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CalculatorApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/Main.fxml")
        );

        Scene scene = new Scene(loader.load());

        scene.getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        stage.setTitle("Advanced Calculator");
        stage.setScene(scene);

        // -----------------------------
        // Window size limits
        // -----------------------------
        stage.setMinWidth(320);
        stage.setMinHeight(650);

        // Optional maximum size
        // stage.setMaxWidth(900);
        // stage.setMaxHeight(1000);

        stage.show();

        scene.getRoot().requestFocus();
    }
}