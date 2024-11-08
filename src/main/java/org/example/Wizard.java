package org.example;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class Wizard {

    Stage stage = new Stage();
    GridPane grid = new GridPane();

    public void showWizard() {
        stage.setTitle("Create new Project");

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
        EventHandler<ActionEvent> importEvent = this::importpdfs;;
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
            // Action Handler for data, ...
            stage.close();
        });

        Button cancelBtn = new Button();
        cancelBtn.setText("Cancel");
        cancelBtn.isCancelButton();
        cancelBtn.setOnAction(event -> stage.close());


        grid.add(labelName, 0, 0);
        grid.add(textName, 1, 0);
        grid.add(labelImportPdf, 0, 1);
        grid.add(importBtn, 1, 1);
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

        File file = fileChooser.showOpenDialog(stage);

        Label pdfName;
        if (file != null) {
            pdfName = new Label(file.getAbsolutePath());
            grid.add(pdfName, 1,2);
        }
    }

    public void importMatrikel(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(SAM.getBase_dir().toFile());
        File file = fileChooser.showOpenDialog(stage);

        Label pdfName;
        if (file != null) {
            pdfName = new Label(file.getAbsolutePath());
            grid.add(pdfName, 1,4);
        }
    }

}
