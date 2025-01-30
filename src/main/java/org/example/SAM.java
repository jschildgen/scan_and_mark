package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JavaFX App
 */
public class SAM extends Application {
    public static final String SAM_VERSION = "0.1.1";
    private static Path base_dir;
    public static DB db;
    private static Application applicationInstance;

    @Override
    public void start(Stage stage) throws IOException {
        applicationInstance = this;

        getPathFromConfigFile();
        setBase_dir(Paths.get(System.getProperty("user.dir")));

        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        stage.setTitle("SAM - Scan and Mark");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("sam80x80.png")));
        VBox vBox = new VBox(root);
        Scene scene = new Scene(vBox, 1400, 900);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static Path getPathFromConfigFile() throws IOException {
        Path conf_file = Paths.get(System.getProperty("user.dir"), "dir.conf");
        if (!Files.exists(conf_file)) {
            Files.createFile(conf_file);
            Files.write(conf_file, System.getProperty("user.dir").getBytes(), StandardOpenOption.APPEND);
        }
        return Paths.get(new String(Files.readAllBytes(conf_file)));
    }

    protected static void updatePathInConfigFile(Path path) throws IOException {
        Path conf_file = Paths.get(System.getProperty("user.dir"), "dir.conf");
        Files.write(conf_file, path.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static Path getBase_dir() {
        return base_dir;
    }

    public static void setBase_dir(Path base_dir) {
        SAM.base_dir = base_dir;
    }

    public static void createDB(){
        try {
            Path dbFile = getPathFromConfigFile().resolve("db.sqlite3");
            if (!Files.exists(dbFile)) {
                db = new DB(dbFile);
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    public static void open_browser(String url) {
        applicationInstance.getHostServices().showDocument(url);
    }

}