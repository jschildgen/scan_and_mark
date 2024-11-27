package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

        try {
            setBase_dir(getPathFromConfigFile());
        } catch (Exception e) {
            setBase_dir(Paths.get(System.getProperty("user.dir")));
        }

        MenuBar menu = new MenuBar();
        Menu fileMenu = new Menu("File");
        menu.getMenus().addAll(fileMenu);
        MenuItem newProject = new MenuItem("New Project");
        fileMenu.getItems().add(newProject);
        MenuItem clear = new MenuItem("New Window");
        fileMenu.getItems().add(clear);

        Wizard wizard = new Wizard();
        newProject.setOnAction(event -> wizard.showWizard());
        clear.setOnAction(event -> {
            try {
                clearPathConfigFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        stage.setTitle("SAM - Scan and Mark");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("sam80x80.png")));
        VBox vBox = new VBox(menu, root);
        Scene scene = new Scene(vBox, 1400, 900);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private static Path getPathFromConfigFile() throws IOException {
        Path conf_file = Paths.get(System.getProperty("user.dir"), "dir.conf");
        if(!Files.exists(conf_file)) {
            System.out.println("Creating new config file: "+conf_file);
            Files.createFile(conf_file);
            Files.write(conf_file, System.getProperty("user.dir").getBytes(), StandardOpenOption.APPEND);
        }
        return Paths.get(new String(Files.readAllBytes(conf_file)));
    }

    protected static void updatePathInConfigFile(Path path) throws IOException {
        Path conf_file = Paths.get(System.getProperty("user.dir"), "dir.conf");
        Files.write(conf_file, path.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    protected static void clearPathConfigFile() throws IOException {
        //Todo: db clearen
        /*Path dbPath = SAM.getBase_dir().resolve("db.sqlite3");
        try {
            if (Files.exists(dbPath)) {
                Files.delete(dbPath);
                System.out.println("Datenbank gelöscht: " + dbPath.toString());
            } else {
                System.out.println("Datenbank existiert nicht: " + dbPath.toString());
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Löschen der Datenbank: " + e.getMessage());
            e.printStackTrace();
        }
        Path conf_file = Paths.get(System.getProperty("user.dir"), "dir.conf");
        Files.write(conf_file, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
        */
    }

    public static Path getBase_dir() {
        return base_dir;
    }

    public static void setBase_dir(Path base_dir) {
        SAM.base_dir = base_dir;
        try {
            db = new DB(base_dir.resolve("db.sqlite3"));
        } catch (SQLException|IOException e) {
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