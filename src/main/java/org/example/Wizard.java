package org.example;

import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.example.model.Student;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class Wizard {
    Stage stage = new Stage();
    GridPane grid = new GridPane();
    Controller con = new Controller();

    ObservableList<File> pdfNamesList = FXCollections.observableArrayList();
    ListView<File> excelListView = new ListView<>();
    Map<String, Map<String, String>> studentData;
    Map<String, String> dbConfigMap = new HashMap<>();
    boolean remoteConnection = false;

    private List<Student> studentsList = new ArrayList<>();

    public void showWizard() {
        if (stage.isShowing()) {
            stage.close();
        }
        stage.setTitle("Create new Project");

        stage = new Stage();
        grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setHgrow(Priority.NEVER);
        leftColumn.setPercentWidth(40);

        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setHgrow(Priority.ALWAYS);
        rightColumn.setPercentWidth(60);

        grid.getColumnConstraints().addAll(leftColumn, rightColumn);

        //name of project
        Label labelName = new Label("Name: ");
        TextField textName = new TextField();
        textName.setPromptText("Name: ");

        //button for import of pdfs
        Label labelImportPdf = new Label("Import PDFs:");
        ListView<File> pdfListView = new ListView<>(pdfNamesList);
        pdfListView.setFixedCellSize(24);
        IntegerBinding pdfVisibleRowCount = Bindings.createIntegerBinding(
                () -> Math.min(pdfNamesList.size(), 10), pdfNamesList
        );
        pdfListView.prefHeightProperty().bind(pdfVisibleRowCount.multiply(pdfListView.getFixedCellSize()));
        Button addPdfBtn = new Button("+");
        Button removePdfBtn = new Button("-");
        Button moveUpPdfBtn = new Button("↑");
        Button moveDownPdfBtn = new Button("↓");

        addPdfBtn.setOnAction(this::importpdfs);
        removePdfBtn.setOnAction(event -> {
            int selectedIndex = pdfListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                pdfNamesList.remove(selectedIndex);
            }
        });

        moveUpPdfBtn.setOnAction(event -> moveItem(pdfListView, -1));
        moveDownPdfBtn.setOnAction(event -> moveItem(pdfListView, 1));

        VBox pdfControlBox = new VBox(5, addPdfBtn, removePdfBtn, moveUpPdfBtn, moveDownPdfBtn);
        HBox pdfListRow = new HBox(10, pdfControlBox, pdfListView);

        //student number import
        Label labelMatrikel = new Label("Import student numbers:");
        ComboBox<String> matrikelDropdown = new ComboBox<>();
        matrikelDropdown.getItems().addAll("HISinOne Excel File", "Custom CSV input");
        matrikelDropdown.setValue("Pick an import option");

        ObservableList<File> excelFiles = FXCollections.observableArrayList();
        excelListView.setItems(excelFiles);

        Button addExcelBtn = new Button("+");
        Button removeExcelBtn = new Button("-");

        addExcelBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = fileChooser.showOpenDialog(stage);

            if (file != null) {
                excelFiles.add(file);
            }
        });

        removeExcelBtn.setOnAction(event -> {
            int selectedIndex = excelListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                excelFiles.remove(selectedIndex);
            }
        });

        VBox excelControlBox = new VBox(5, addExcelBtn, removeExcelBtn);
        HBox excelListRow = new HBox(10, excelControlBox, excelListView);
        excelListRow.setVisible(false);

        Label instructionLabel = new Label("Copy/Paste tab-separated list of students' matno and name (e.g., from Excel), one student per line. Optionally, name separated in two columns.");
        TextArea studentsTextArea = new TextArea();
        studentsTextArea.setPromptText("Matno\tName");
        instructionLabel.setVisible(false);
        studentsTextArea.setVisible(false);

        matrikelDropdown.setOnAction(event -> {
            String selectedOption = matrikelDropdown.getValue();
            boolean isCustomCSV = "Custom CSV input".equals(selectedOption);
            excelListRow.setVisible(!isCustomCSV);
            instructionLabel.setVisible(isCustomCSV);
            studentsTextArea.setVisible(isCustomCSV);
        });

        VBox importStudentsSection = new VBox(10, instructionLabel, studentsTextArea);
        GridPane.setColumnSpan(importStudentsSection, GridPane.REMAINING);

        //page count
        Label labelPageCount = new Label("Page count: ");
        TextField textPagecnt = new TextField();
        textPagecnt.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));

        Label workingDirLbl = new Label("Working Dir: ");
        TextField workingDir = new TextField();
        Button chooseDirButton = new Button("Select Directory");
        chooseDirButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Working Directory");
            File selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                workingDir.setText(selectedDirectory.getAbsolutePath());
            }
        });
        HBox workingDirRow = new HBox(10, workingDir, chooseDirButton);
        HBox.setHgrow(workingDir, Priority.ALWAYS);
        workingDir.setPrefWidth(300);

        Label chooseDB = new Label("Choose prefered DB configuration: ");
        ComboBox<String> dbDropdown = new ComboBox<>();
        dbDropdown.getItems().addAll("Local Database", "Remote Database");
        dbDropdown.setValue("Pick a database option");
        Label hostLabel = new Label("Host:");
        TextField hostField = new TextField();
        Label portLabel = new Label("Port:");
        TextField portField = new TextField("3306");
        Label dbNameLabel = new Label("Database Name:");
        TextField dbNameField = new TextField();
        Label userLabel = new Label("Username:");
        TextField userField = new TextField();
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        HBox hostRow = new HBox(10, hostLabel, hostField);
        HBox portRow = new HBox(10, portLabel, portField);
        HBox dbNameRow = new HBox(10, dbNameLabel, dbNameField);
        HBox userRow = new HBox(10, userLabel, userField);
        HBox passwordRow = new HBox(10, passwordLabel, passwordField);
        VBox dbConfig = new VBox(10, hostRow, portRow, dbNameRow, userRow, passwordRow);

        dbConfig.setVisible(false);
        dbDropdown.setOnAction(event -> {
            dbConfig.setVisible(dbDropdown.getValue().equals("Remote Database"));
        });

        Button createBtn = new Button();
        createBtn.setText("Create");
        createBtn.isDefaultButton();
        createBtn.setOnAction(event -> {
            String pageCount = textPagecnt.getText();
            studentData = readExcelFiles(excelFiles);
            String selectedOption = matrikelDropdown.getValue();
            if ("Custom CSV input".equals(selectedOption)) {
                String studentText = studentsTextArea.getText();
                importStudents(studentText);
            }
            if("Remote Database".equals(dbDropdown.getValue())){
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
            }
            try {
                handleInputData(pageCount, textName.getText(), workingDir.getText());
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
            stage.close();
        });

        Button cancelBtn = new Button();
        cancelBtn.setText("Cancel");
        cancelBtn.isCancelButton();
        cancelBtn.setOnAction(event -> stage.close());


        grid.add(labelName, 0, 0);
        grid.add(textName, 1, 0);
        grid.add(labelImportPdf, 0, 1);
        grid.add(pdfListRow, 1, 1);
        grid.add(labelMatrikel, 0, 2);
        grid.add(matrikelDropdown, 1, 2);
        grid.add(excelListRow, 1, 4);
        grid.add(importStudentsSection, 1, 3);
        grid.add(labelPageCount, 0, 5);
        grid.add(textPagecnt, 1, 5);
        grid.add(workingDirLbl, 0, 6);
        grid.add(workingDirRow, 1, 6);
        grid.add(chooseDB, 0, 7);
        grid.add(dbDropdown, 1, 7);
        grid.add(dbConfig, 1, 8);
        grid.add(createBtn, 1, 9);
        grid.add(cancelBtn, 0, 9);

        Scene scene = new Scene(grid, 600, 800);
        stage.setScene(scene);
        stage.show();
    }

    private void importStudents(String input) {
        if (!input.isEmpty()) {
            for (String line : input.split("\n")) {
                String[] parts = line.split("\t");
                if (parts.length < 2) {
                    continue;
                }
                Student student = new Student(parts[0]);
                student.setName1(parts[1]);
                if (parts.length >= 3) {
                    student.setName2(parts[2]);
                }
                studentsList.add(student);
            }
        }
    }

    private void moveItem(ListView<File> listView, int direction) {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || (direction < 0 && selectedIndex == 0) || (direction > 0 && selectedIndex == listView.getItems().size() - 1)) {
            return;
        }
        File item = listView.getItems().remove(selectedIndex);
        int newIndex = selectedIndex + direction;
        listView.getItems().add(newIndex, item);
        listView.getSelectionModel().select(newIndex);
    }

    public void importpdfs(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(extFilter);

        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null) {
            pdfNamesList.addAll(files);
        }
    }

    public Map<String, Map<String, String>> readExcelFiles(List<File> excelFiles) {
        Map<String, Map<String, String>> studentData = new HashMap<>();

        for (File file : excelFiles) {
            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(fis)) {
                for (Sheet sheet : workbook) {
                    boolean startFound = false;
                    boolean endFound = false;
                    int lastNameCol = -1;
                    int firstNameCol = -1;
                    int matrikelCol = -1;

                    for (Row row : sheet) {
                        if (endFound) break;
                        if (!startFound) {
                            for (Cell cell : row) {
                                if (cell.getCellType() == CellType.STRING &&
                                        "startHISsheet".equalsIgnoreCase(cell.getStringCellValue())) {
                                    startFound = true;
                                    break;
                                }
                            }
                            continue;
                        }

                        for (Cell cell : row) {
                            if (cell.getCellType() == CellType.STRING &&
                                    "endHISsheet".equalsIgnoreCase(cell.getStringCellValue())) {
                                endFound = true;
                                break;
                            }
                        }
                        if (endFound) break;

                        if (lastNameCol == -1 || firstNameCol == -1 || matrikelCol == -1) {
                            for (Cell cell : row) {
                                if (cell.getCellType() == CellType.STRING) {
                                    String header = cell.getStringCellValue().toLowerCase();
                                    if (header.contains("nachname")) {
                                        lastNameCol = cell.getColumnIndex();
                                    } else if (header.contains("vorname")) {
                                        firstNameCol = cell.getColumnIndex();
                                    } else if (header.contains("matrikelnummer")) {
                                        matrikelCol = cell.getColumnIndex();
                                    }
                                }
                            }
                            continue;
                        }

                        Cell matrikelCell = row.getCell(matrikelCol);
                        Cell lastNameCell = row.getCell(lastNameCol);
                        Cell firstNameCell = row.getCell(firstNameCol);

                        String matrikelNummer = matrikelCell != null ? getCellValueAsString(matrikelCell).trim() : "";
                        String lastName = lastNameCell != null ? getCellValueAsString(lastNameCell) : "";
                        String firstName = firstNameCell != null ? getCellValueAsString(firstNameCell) : "";

                        if (!matrikelNummer.isEmpty()) {
                            if (matrikelNummer.matches("\\d+\\.0+")) {
                                matrikelNummer = matrikelNummer.substring(0, matrikelNummer.indexOf('.'));
                            }
                        }

                        if ((!matrikelNummer.isEmpty())) {
                            Student student = new Student(matrikelNummer);
                            student.setName1(lastName);
                            student.setName2(firstName);

                            studentsList.add(student);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Fehler beim Lesen der Datei: " + file.getName());
                e.printStackTrace();
            }
        }
        return studentData;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    public void handleInputData(String pageCount, String name, String workingDir) throws IOException, SQLException {
        if (name == null || name.trim().isEmpty() || name.matches(".*[<>:\"/\\\\|?*].*")) {
            name = "NewProject";
        }

        if (!workingDir.isEmpty()) {
            Path dir = Paths.get(workingDir);
            Path newDir = dir.resolve(name);
            if (!Files.exists(newDir)) {
                Files.createDirectories(newDir);
            }
            SAM.updatePathInConfigFile(newDir);
            if(!remoteConnection){
                SAM.createDB();
            } else {
                System.out.println("Remote Connection");
            }
            SAM.db.setSAM("name", name);
            int numpages;
            try {
                numpages = Integer.parseInt(pageCount);
            } catch (NumberFormatException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Invalid Number");
                errorAlert.showAndWait();
                return;
            }

            File outputFile = newDir.resolve("allOf" + name + ".pdf").toFile();
            PDFMergerUtility pdfMerger = new PDFMergerUtility();
            pdfMerger.setDestinationFileName(outputFile.getAbsolutePath());

            for (File pdf : pdfNamesList) {
                pdfMerger.addSource(pdf);
            }
            pdfMerger.mergeDocuments(null);
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        PDFTools.splitPDF(outputFile, numpages);
                        for (Student student : studentsList) {
                            SAM.db.persist(student);
                        }
                    } catch (IOException | SQLException e) {
                        Platform.runLater(() -> {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setContentText(e.getMessage());
                            errorAlert.showAndWait();
                        });
                    }
                    return null;
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        System.out.println("Loaded wizard");
                    });
                }
            };

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();

        }
    }
}
