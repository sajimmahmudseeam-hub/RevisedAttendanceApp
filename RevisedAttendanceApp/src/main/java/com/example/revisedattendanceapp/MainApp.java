package com.example.revisedattendanceapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AttendanceView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            if (getClass().getResource("style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            }

            primaryStage.setTitle("Seeam's Attendance Sheet - Southwest University of Science and Technology");
            primaryStage.setScene(scene);

            // Auto-fit setup for responsive rendering on all display resolutions
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(650);
            primaryStage.setMaximized(true); // Default to maximized for full view
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Error loading AttendanceView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}