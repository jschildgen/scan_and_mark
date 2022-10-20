package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JavaFX App
 */
public class QRexam extends Application {
    public static Path base_dir;

    @Override
    public void start(Stage stage) throws IOException {
        /*DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);
        base_dir = Paths.get(selectedDirectory.getAbsolutePath());*/
        base_dir = Paths.get("/media/tmpfs/qrtest");

        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        stage.setTitle("QRexam "+base_dir);
        stage.setScene(new Scene(root, 1400, 900));
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}