package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * JavaFX App
 */
public class QRexam extends Application {
    private static Path base_dir;
    public static DB db;
    private static Application applicationInstance;

    @Override
    public void start(Stage stage) throws IOException {
        applicationInstance = this;
        setBase_dir(Paths.get("/home/johannes/qrtest"));

        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        stage.setTitle("QRexam");
        Scene scene = new Scene(root, 1400, 900);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static Path getBase_dir() {
        return base_dir;
    }

    public static void setBase_dir(Path base_dir) {
        QRexam.base_dir = base_dir;
        try {
            db = new DB(base_dir.resolve("db.sqlite3"));
        } catch (SQLException|IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch();
    }

    public static void open_browser(String url) {
        applicationInstance.getHostServices().showDocument(url);
    }

}