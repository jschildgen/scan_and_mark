package org.example;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.example.elements.MarkingPane;
import org.example.model.Exercise;
import org.example.model.Page;
import org.example.model.Student;

public class Controller {
    private static boolean FULL_WIDTH_EXERCISES = true;
    @FXML VBox answers_list;

    @FXML TextField working_dir;
    @FXML ListView listView_students;
    @FXML ListView listView_pages;
    @FXML ListView listView_exercises;
    @FXML BorderPane fullPageBorderPane;
    @FXML ScrollPane answersScrollPane;
    @FXML Pane fullPageImagePane;
    @FXML ImageView fullPageImageView;
    @FXML TextField studentMatno;
    @FXML Label studentName;
    @FXML TextField exerciseLabel;
    @FXML TextField exercisePoints;
    @FXML Text total_points;

    ObservableList<Student> list_students = FXCollections.observableArrayList();
    ObservableList<Page> list_pages = FXCollections.observableArrayList();
    ObservableList<Exercise> list_exercises = FXCollections.observableArrayList();
    ObservableList<String> student_matno_autocomplete = FXCollections.observableArrayList();

    private double[] image_click = new double[2];

    public void initialize() {
        listView_students.setItems(list_students);
        listView_pages.setItems(list_pages);
        listView_exercises.setItems(list_exercises);

        student_matno_autocomplete.add("3242342");
        student_matno_autocomplete.add("1234567");
        student_matno_autocomplete.add("1234777");
        student_matno_autocomplete.add("1234888");
        student_matno_autocomplete.add("1234889");
        student_matno_autocomplete.add("1234890");
        student_matno_autocomplete.add("1234898");
        student_matno_autocomplete.add("1234898");
        student_matno_autocomplete.add("1234899");
        student_matno_autocomplete.add("1234900");

        working_dir.setText(QRexam.getBase_dir().toString());

        list_students.clear();
        try {
            for(Student student : QRexam.db.getStudents()) {
                list_students.add(student);
            }
        } catch (SQLException e) {
            showError("DB Error: getStudents");
        }

        if(list_students.size() > 0) {
            listView_students.getSelectionModel().select(0);
            clickStudent(null);
        }

        list_exercises.clear();
        try {
            for(Exercise exercise : QRexam.db.getExercises()) {
                list_exercises.add(exercise);
            }
        } catch (SQLException e) {
            showError("DB Error: getExercises");
        }

        refreshTotalPoints();
    }

    public void clickStudent(MouseEvent mouseEvent) {
        Student student = (Student) listView_students.getSelectionModel().getSelectedItem();
        Page page_was = (Page) listView_pages.getSelectionModel().getSelectedItem();

        if(student == null) { return; }
        studentMatno.setText(student.getMatno() != null ? student.getMatno() : "");
        studentName.setText(student.getName() != null ? " "+student.getName() : "");

        /* fill pages list */
        list_pages.clear();
        for(Page page : student.getPages().values()) {
            list_pages.add(page);
            /* was this page selected before changing student? */
            if (page_was != null && page_was.getPageNo().equals(page.getPageNo())) {
                listView_pages.getSelectionModel().select(page);
            }
        }

        FXCollections.sort(list_pages, (a,b)->a.compareTo(b));
        clickPage(null);
    }

    public void clickPage(MouseEvent mouseEvent) {
        Page page = (Page) listView_pages.getSelectionModel().getSelectedItem();
        if(page == null) { return; }

        /* show page image */
        try {
            fullPageImageView.setImage(page.getImage());
            fullPageImageView.setFitHeight(fullPageBorderPane.getHeight()-50);
            fullPageImageView.setPreserveRatio(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* show highlighted exercise boxes */
        refreshRectangles();
    }


    public void clickExercise(MouseEvent mouseEvent) {
        Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
        if(exercise == null) { return; }

        exerciseLabel.setText(exercise.getLabel());
        exercisePoints.setText(exercise.getPoints() == null ? "" : ""+exercise.getPoints());

        /* show exam page */
        if(listView_students.getSelectionModel().isEmpty()
            && listView_students.getItems().size() > 0) {
            listView_students.getSelectionModel().select(0);
        }
        Page page = ((Student) listView_students.getSelectionModel().getSelectedItem())
                .getPage(exercise.getPageNo());
        listView_pages.getSelectionModel().select(page);
        clickPage(null);

        /* show answers list */
        answers_list.getChildren().clear();
        Map<String, BigDecimal> feedback_map;
        ObservableList<String> feedback_list;
        try {
            feedback_map = QRexam.db.getFeedback(exercise);
            feedback_list = FXCollections.observableArrayList();
            feedback_map.forEach((feedback, points) -> feedback_list.add(feedback));
        } catch (SQLException e) {
            showError("DB Error: feedback for exercise "+exercise);
            return;
        }

        for(Student student : list_students) {
            try {
                answers_list.getChildren().add(new MarkingPane(student, exercise, feedback_map, feedback_list));
            } catch (SQLException e) {
                showError("DB Error: Answer "+student+" / "+exercise);
            }
            try {
                ImageView imageView = new ImageView();
                imageView.setImage(student.getAnswerImage(exercise));
                imageView.setFitWidth(answersScrollPane.getWidth());
                imageView.setPreserveRatio(true);
                imageView.setOnMouseClicked(e -> {
                    listView_students.getSelectionModel().select(student);
                    clickStudent(null);
                    listView_pages.getSelectionModel().select(student.getPage(exercise.getPageNo()));
                    clickPage(null);
                });
                answers_list.getChildren().add(imageView);
                answers_list.getChildren().add(new Separator());
            } catch (IOException e) {
                e.printStackTrace();
                Label error_label = new Label("Error loading image");
                answers_list.getChildren().add(error_label);
            }
        }
    }

    public void changeExercise(KeyEvent keyEvent) {
        Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
        if(exercise == null) { return; }

        exercise.setLabel(exerciseLabel.getText());
        try {
            BigDecimal points = new BigDecimal(exercisePoints.getText());
            if(points.compareTo(BigDecimal.ZERO) >= 0) {
                exercise.setPoints(points);
            } else {
                exercise.setPoints(null);
                exercisePoints.setText("");
            }
        } catch (NumberFormatException e) {
            exercise.setPoints(null);
            exercisePoints.setText("");
        }

        refreshTotalPoints();

        try {
            QRexam.db.persist(exercise);
        } catch (SQLException e) {
            showError("DB Error: "+exercise);
        }
        listView_exercises.refresh();
        FXCollections.sort(list_exercises, (a,b)->a.compareTo(b));
    }

    private void refreshTotalPoints() {
        BigDecimal total = list_exercises.stream()
                .map(e -> e.getPoints())
                .filter(p -> p != null)
                .reduce((p1, p2) -> p1.add(p2))
                .orElse(BigDecimal.ZERO);
        total_points.setText(total.toString());
    }

    public void deleteExercise(ActionEvent actionEvent) {
        Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
        if(exercise == null) { return; }
        try {
            QRexam.db.delete(exercise);
        } catch (SQLException e) {
            showError("DB Error: Deleting exercise "+exercise);
        }
        list_exercises.remove(exercise);
        listView_exercises.getSelectionModel().clearSelection();
        exerciseLabel.setText("");
        answers_list.getChildren().clear();
        refreshRectangles();
    }


    public void fullPageImageDragStart(MouseEvent mouseEvent) {
        image_click[0] = mouseEvent.getX();
        image_click[1] = mouseEvent.getY();
    }

    public void fullPageImageDragStop(MouseEvent mouseEvent) {
        double[] point1 = { image_click[0], image_click[1] };
        double[] point2 = { mouseEvent.getX(), mouseEvent.getY() };
        point1 = windowPosToImagePos(point1);
        point2 = windowPosToImagePos(point2);
        final double[][] pos = { point1, point2 };

        if(pos[0][0] == pos[1][0] && pos[0][1] == pos[1][1]) {
            /* no drag, just click (on rectangle?) */
            list_exercises.stream()
                    .filter(e -> e.containsPoint(pos[0]))
                    .forEach(e -> listView_exercises.getSelectionModel().select(e));
            clickExercise(null);
            return;
        }

        if(FULL_WIDTH_EXERCISES) {
            pos[0][0] = 0;
            pos[1][0] = fullPageImageView.getImage().getWidth();
        }

        if(pos[1][0] <= pos[0][0] || pos[1][1] <= pos[0][1] ) {
            /* negative rectangle size */
            return;
        }

        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Exercise");
        textInputDialog.setContentText("Exercise:");
        textInputDialog.showAndWait();
        String input = textInputDialog.getEditor().getText();

        Page page = (Page) listView_pages.getSelectionModel().getSelectedItem();

        Exercise e = new Exercise(input, page.getPageNo(), pos);
        try {
            QRexam.db.persist(e);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("DB Error: "+e);
        }
        list_exercises.add(e);
        FXCollections.sort(list_exercises, (a,b)->a.compareTo(b));

        fullPageImagePane.getChildren().add(getRectangle(e));

        listView_exercises.getSelectionModel().select(e);
        clickExercise(null);
    }


    public void changeStudent(KeyEvent keyEvent) {
        Student student = (Student) listView_students.getSelectionModel().getSelectedItem();
        student.setMatno(studentMatno.getText());
        try {
            QRexam.db.persist(student);
        } catch (SQLException e) {
            showError("DB Error: "+student);
        }
        FXCollections.sort(list_students, (a,b)->a.compareTo(b));
        listView_students.refresh();
    }

    public void chooseDir(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(fullPageBorderPane.getScene().getWindow());
        working_dir.setText(selectedDirectory.getAbsolutePath());
    }

    public void loadDir(ActionEvent actionEvent) {
        Path newDir = Paths.get(working_dir.getText());
        if(working_dir.getText().isBlank() || !Files.exists(newDir) || !Files.isDirectory(newDir)) {
            showError("Invalid directory");
            return;
        }
        QRexam.setBase_dir(newDir);
        initialize();
    }

    public void importPDF(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(fullPageBorderPane.getScene().getWindow());
        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Number of pages");
        textInputDialog.setContentText("Number of pages per exam:");
        textInputDialog.showAndWait();
        String input = textInputDialog.getEditor().getText();
        int numpages;
        try {
            numpages = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            showError("Invalid number");
            return;
        }
        try {
            PDFTools.splitPDF(selectedFile, numpages);
        } catch (IOException e) {
            showError("Error splitting PDF");
            return;
        }
        initialize();
    }

    private void refreshRectangles() {
        fullPageImagePane.getChildren().removeIf(n -> n instanceof Rectangle);

        Page page = (Page) listView_pages.getSelectionModel().getSelectedItem();
        if(page != null) {
            list_exercises.stream()
                    .filter(e -> e.getPageNo().equals(page.getPageNo()))
                    .forEach(e -> fullPageImagePane.getChildren().add(getRectangle(e)));
        }
    }


    public Rectangle getRectangle(Exercise e) {
        double[] point1 = { e.getPos()[0][0], e.getPos()[0][1] };
        double[] point2 = { e.getPos()[1][0], e.getPos()[1][1] };
        point1 = imagePosToWindowPos(point1);
        point2 = imagePosToWindowPos(point2);
        Rectangle rectangle = new Rectangle(point1[0], point1[1], point2[0]-point1[0], point2[1]-point1[1] );
        rectangle.setFill(new Color(255.0/255,230.0/255, 0, 0.2));
        return rectangle;
    }

    private double[] windowPosToImagePos(double[] pos) {
        double factor = fullPageImageView.getImage().getHeight() / fullPageImageView.getFitHeight();
        double[] imagePos = { pos[0] * factor, pos[1] * factor };
        return imagePos;
    }

    private double[] imagePosToWindowPos(double[] pos) {
        double factor = fullPageImageView.getFitHeight() / fullPageImageView.getImage().getHeight();
        double[] windowPos = { pos[0] * factor, pos[1] * factor };
        return windowPos;
    }

    private void showError(String msg) {
        Alert a = new Alert(AlertType.ERROR);
        a.setContentText(msg);
        a.show();
    }

    public void imageKeyReleased(KeyEvent keyEvent) {
        if(keyEvent.getCode() == KeyCode.DELETE) {
            Page page = (Page) listView_pages.getSelectionModel().getSelectedItem();
            Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
            if(page != null && exercise != null) {
                deleteExercise(null);
            }
        }
    }
}
