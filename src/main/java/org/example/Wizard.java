package org.example;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.stage.Window;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.example.model.Student;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

public class Wizard {
    Stage stage = new Stage();
    GridPane grid = new GridPane();
    Controller con = new Controller();

    ObservableList<File> pdfNamesList = FXCollections.observableArrayList();
    File studentList = null;

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
        Button importBtn = new Button("import pdf");
        EventHandler<ActionEvent> importEvent = this::importpdfs;
        importBtn.setOnAction(importEvent);

        //button for import of Matrikelnummern
        Label labelMatrikel = new Label("Import Matrikelnummern:");
        Button matrikelBtn = new Button("import Matrikelnummern");
        EventHandler<ActionEvent> matrikelEvent = e -> importMatrikel();
        matrikelBtn.setOnAction(matrikelEvent);

        //page count
        Label labelPageCount = new Label("Page count: ");
        TextField textPagecnt = new TextField();
        textPagecnt.setPromptText("Page count: ");
        textPagecnt.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));

        Button createBtn = new Button();
        createBtn.setText("Create");
        createBtn.isDefaultButton();
        createBtn.setOnAction(event -> {
            String projectName = textName.getText();
            String pageCount = textPagecnt.getText();
            handleInputData(projectName, pageCount);
            stage.close();
        });

        Button cancelBtn = new Button();
        cancelBtn.setText("Cancel");
        cancelBtn.isCancelButton();
        cancelBtn.setOnAction(event -> stage.close());

        ListView<File> pdfListView = new ListView<>(pdfNamesList);
        pdfListView.setFixedCellSize(24);
        int maxVisibleRows = 10;
        IntegerBinding visibleRowCount = Bindings.createIntegerBinding(
                () -> Math.min(pdfNamesList.size(), maxVisibleRows),
                pdfNamesList
        );
        pdfListView.prefHeightProperty().bind(visibleRowCount.multiply(pdfListView.getFixedCellSize()));

        grid.add(labelName, 0, 0);
        grid.add(textName, 1, 0);
        grid.add(labelImportPdf, 0, 1);
        grid.add(importBtn, 1, 1);
        grid.add(pdfListView, 1, 2);
        grid.add(labelMatrikel, 0, 3);
        grid.add(matrikelBtn, 1, 3);
        grid.add(labelPageCount, 0, 5);
        grid.add(textPagecnt, 1, 5);
        grid.add(createBtn, 1, 6);
        grid.add(cancelBtn, 0, 6);

        Scene scene = new Scene(grid, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    public void importpdfs(ActionEvent event){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(SAM.getBase_dir().toFile());
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(extFilter);

        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null) {
            pdfNamesList.addAll(files);
        }
    }

    public void importMatrikel(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(SAM.getBase_dir().toFile());
        File file = fileChooser.showOpenDialog(stage);

        studentList = file;
        Label pdfName;
        if (file != null) {
            pdfName = new Label(studentList.getName());
            grid.add(pdfName, 1,4);
        }
    }

    public void handleInputData(String projectName, String pageCount) {
        List<File> importedPdfNames = pdfNamesList;
        StringBuilder message = new StringBuilder();
        message.append("Projektname: ").append(projectName).append("\n");
        message.append("Seitenanzahl: ").append(pageCount).append("\n");
        message.append("Importierte PDFs:\n");

        for (File pdfName : importedPdfNames) {
            message.append("- ").append(pdfName.getName()).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Eingabedaten");
        alert.setHeaderText("Eingaben des Projekts:");
        alert.setContentText(message.toString());

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

        //merge pdfs to one
        File allPages = importedPdfNames.getFirst();
        try {
            PDFMergerUtility pdfMerge = new PDFMergerUtility();
            for(File pdf : importedPdfNames){
                pdfMerge.addSource(pdf);
            }
            pdfMerge.setDestinationFileName(allPages.getAbsolutePath());
            pdfMerge.mergeDocuments();
        } catch (Exception e){
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText(e.getMessage());
            errorAlert.showAndWait();
        }

        //runLater() put update in queue, GUI Thread will handle
        Thread thread = new Thread(() -> {
            try {
                PDFTools.splitPDF(allPages, numpages);
                con.list_students = (ObservableList<Student>) studentList;
            } catch (IOException e) {
                Platform.runLater(() -> {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setContentText(e.getMessage());
                    errorAlert.showAndWait();
                });
            }
            Platform.runLater(() -> con.initialize());
        });

        thread.setDaemon(true);
        thread.start();

        alert.showAndWait();
    }
}
