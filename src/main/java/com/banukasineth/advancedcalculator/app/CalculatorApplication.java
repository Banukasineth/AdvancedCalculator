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

        stage.setTitle("Advanced Calculator");

        stage.setScene(scene);

        stage.show();
    }
}