package org.example;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.importexport.HISinOneExcelStudentsImporter;
import org.example.importexport.StudentImporter;
import org.example.importexport.TextCSVStudentImporter;
import org.example.model.Student;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class NewProjectController {

    static NewProjectController instance = null;

    @FXML
    public ComboBox<String> dbDropdown;

    @FXML
    public TextArea studentsTextArea;
    public ProgressBar progress;
    public TextField db_host;
    public TextField db_db;
    public TextField db_user;
    public TextField db_password;


    @FXML
    private TextField projectName;

    @FXML
    private ListView<File> pdfListView;

    @FXML
    private ListView<File> excelListView;

    @FXML
    private TextField textPageCount;

    @FXML
    private TextField workingDir;

    @FXML
    private Label numpages;
    private int num_pages;

    private final ObservableList<File> pdfNamesList = FXCollections.observableArrayList();

    private File previous_choice_directory = null;

    @FXML
    public void initialize() {
        if(instance == null) {
            instance = this;
        }
        pdfListView.setItems(pdfNamesList);
    }

    @FXML
    private void importPDFs(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        if(previous_choice_directory != null) {
            fileChooser.setInitialDirectory(previous_choice_directory);
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files == null || files.isEmpty()) {
            return;
        }

        pdfNamesList.addAll(files);

        previous_choice_directory = files.getFirst().getParentFile();
        this.refreshNumPages(event);
    }


    @FXML
    private void removePDF(ActionEvent event) {
        int selectedIndex = pdfListView.getSelectionModel().getSelectedIndex();
        this.refreshNumPages(event);
        if (selectedIndex >= 0) {
            pdfNamesList.remove(selectedIndex);
        }
    }



    @FXML
    private void moveUpPDF(ActionEvent event) {
        int selectedIndex = pdfListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex > 0) {
            File file = pdfNamesList.remove(selectedIndex);
            pdfNamesList.add(selectedIndex - 1, file);
            pdfListView.getSelectionModel().select(selectedIndex - 1);
        }
    }

    @FXML
    private void moveDownPDF(ActionEvent event) {
        int selectedIndex = pdfListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < pdfNamesList.size() - 1) {
            File file = pdfNamesList.remove(selectedIndex);
            pdfNamesList.add(selectedIndex + 1, file);
            pdfListView.getSelectionModel().select(selectedIndex + 1);
        }
    }


    @FXML
    private void addExcel(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        if(previous_choice_directory != null) {
            fileChooser.setInitialDirectory(previous_choice_directory);
        }

        List<File> files = fileChooser.showOpenMultipleDialog(null);

        if (files == null || files.isEmpty()) {
            return;
        }

        for (File file : files) {
            if (!HISinOneExcelStudentsImporter.format_is_valid(file)) {
                Controller.showError("Invalid Excel file format: " + file.getName());
                continue;
            }
            excelListView.getItems().add(file);
        }
        previous_choice_directory = files.getFirst().getParentFile();
    }

    @FXML
    private void removeExcel(ActionEvent event) {
        int selectedIndex = excelListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            excelListView.getItems().remove(selectedIndex);
        }
    }

    @FXML
    private void chooseDirectory(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            workingDir.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void createProject(ActionEvent event) {
        // Handle project creation logic here
        System.out.println("Project created: " + projectName.getText());

        String pageCount = textPageCount.getText();

        List<Student> students = new ArrayList<Student>();
        if (!excelListView.getItems().isEmpty()) {
            StudentImporter studentImporter = new HISinOneExcelStudentsImporter(excelListView.getItems());
            try {
                students.addAll(studentImporter.import_students());
            } catch (IOException e) {
                e.printStackTrace();
                Controller.showError(e.getMessage());
            }
        }

        if (!studentsTextArea.getText().isEmpty()) {
            StudentImporter studentImporter = new TextCSVStudentImporter(studentsTextArea.getText());
            try {
                students.addAll(studentImporter.import_students());
            } catch (IOException e) {
                e.printStackTrace();
                Controller.showError(e.getMessage());
            }
        }
        boolean remoteConnection = false;
        // TODO
        /*if("Remote Database".equals(dbDropdown.getValue())){
            dbConfigMap.put("Host", hostField.getText());
            dbConfigMap.put("Port", portField.getText());
            dbConfigMap.put("Database Name", dbNameField.getText());
            dbConfigMap.put("Username", userField.getText());
            dbConfigMap.put("Password", passwordField.getText());
            try {
                Connection connection = DB.remoteConnection(dbConfigMap);
                remoteConnection = true;
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }*/

        // Create the new project
        String name = projectName.getText();
        if (name == null || name.trim().isEmpty() || name.matches(".*[<>:\"/\\\\|?*].*")) {
            name = "NewProject";
        }

        if (workingDir.getText().isEmpty()) {
            Controller.showError("Working Directiory must be set!");
            return;
        }

        try {
            Path dir = Paths.get(workingDir.getText());
            Path newDir = dir.resolve(name);
            if (!Files.exists(newDir)) {
                Files.createDirectories(newDir);
            }
            SAM.updatePathInConfigFile(newDir);
            if (!remoteConnection) {
                SAM.createDB();
            } else {
                System.out.println("Remote Connection");
            }
            SAM.db.setSAM("name", name);
            int numpages;
            try {
                numpages = Integer.parseInt(pageCount);
            } catch (NumberFormatException e) {
                Controller.showError("Invalid number of pages.");
                return;
            }

            File outputFile = newDir.resolve("allOf" + name + ".pdf").toFile();
            PDFTools.mergePDFs(pdfNamesList, outputFile);

            Thread thread = new Thread(() -> {
                try {
                    PDFTools.splitPDF(outputFile, numpages, NewProjectController::setProgress);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Platform.runLater(() -> {
                            stage.close();
                            Controller.refresh();
                        }
                );
            });
            thread.start();

            for (Student student : students) {
                Controller.student_matno_autocomplete.put(student.getMatno(), student);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Controller.showError(e.getMessage());
        }
    }

    @FXML
    private void cancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    // TODO
    public static void setProgress(double progress) {
        NewProjectController.instance.progress.setProgress(progress);
    }

    public void refreshNumPages(Event keyEvent) {
        num_pages = 0;
        try {
            for (File file : pdfListView.getItems()) {
                num_pages += PDFTools.numPDFpages(file);
            }
        } catch(IOException e) {
            num_pages = 0;
        }

        try {
            int pages_per_exam = Integer.parseInt(textPageCount.getText());
            if((num_pages % pages_per_exam) == 0) {
                numpages.setText(String.format("Total: %d pages => %d exams", num_pages, num_pages / pages_per_exam));
            } else {
                numpages.setText(String.format("Total: %d pages => Not divisible by %d", num_pages, pages_per_exam));
            }
        } catch (Exception e) {
            numpages.setText(String.format("Total: %d pages", num_pages));
        }
    }

    public void changeDBType(ActionEvent actionEvent) {
        if("Local Database (SQLite)".equals(dbDropdown.getValue())){
            db_host.setDisable(true);
            db_db.setDisable(true);
            db_user.setDisable(true);
            db_password.setDisable(true);
        } else {
            db_host.setDisable(false);
            db_db.setDisable(false);
            db_user.setDisable(false);
            db_password.setDisable(false);
        }
    }

    public void changeStudentsTextArea(KeyEvent keyEvent) {
        if(TextCSVStudentImporter.format_is_valid(studentsTextArea.getText())) {
            studentsTextArea.setStyle("-fx-control-inner-background: lightgreen;");
        } else {
            studentsTextArea.setStyle("-fx-control-inner-background: red;");
        }
    }
}
