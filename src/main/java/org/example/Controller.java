package org.example;

import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

import com.opencsv.CSVReader;

import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.*;
import org.example.elements.MarkingPane;
import org.example.model.Answer;
import org.example.model.Exercise;
import org.example.model.Page;
import org.example.model.Student;

import javax.swing.*;

public class Controller {
    private static boolean FULL_WIDTH_EXERCISES = true;
    public SplitPane examsSplitPane;

    private static enum AnswerFilter {ALL, NOT_MARKTED, COMPLETED}

    ;
    private AnswerFilter answerFilter = AnswerFilter.ALL;

    private static Controller controllerInstance = null;

    public ToggleGroup filter_answers;
    @FXML
    ProgressBar progress;
    private static Tooltip progress_tooltip = null;
    @FXML
    VBox answers_list;

    @FXML
    TextField working_dir;
    @FXML
    ListView listView_students;
    @FXML
    ListView listView_pages;
    @FXML
    ListView listView_exercises;
    @FXML
    BorderPane fullPageBorderPane;
    @FXML
    ScrollPane answersScrollPane;
    @FXML
    Pane fullPageImagePane;
    @FXML
    ImageView fullPageImageView;
    @FXML
    TextField studentMatno;
    @FXML
    Label studentName;
    @FXML
    TextField exerciseLabel;
    @FXML
    TextField exercisePoints;
    @FXML
    Text total_points;
    @FXML
    RadioButton filter_answers_all;
    @FXML
    RadioButton filter_answers_notmarked;
    @FXML
    RadioButton filter_answers_completed;
    @FXML
    CheckBox limit_show_answers;
    @FXML
    MenuBar menuBar = new MenuBar();

    ObservableList<Student> list_students = FXCollections.observableArrayList();
    ObservableList<Page> list_pages = FXCollections.observableArrayList();
    ObservableList<Exercise> list_exercises = FXCollections.observableArrayList();

    public Map<String, Student> student_matno_autocomplete = new HashMap<>();

    private double[] image_click = new double[2];

    public void initialize() {
        initializeMenu();

        listView_students.setItems(list_students);
        listView_pages.setItems(list_pages);
        listView_exercises.setItems(list_exercises);
        listView_exercises.setCellFactory(cell -> new ListCell<Exercise>() {
            @Override
            protected void updateItem(Exercise exercise, boolean empty) {
                super.updateItem(exercise, empty);
                if (empty || exercise == null) {
                    setStyle(null);
                    setText(null);
                    return;
                }

                if (exercise.getPoints() != null) {  // max points already set
                    setText(String.format("%s (%s P.)", exercise.getLabel(), exercise.getPoints()));
                } else {
                    setText(exercise.getLabel());   // max points not yet set
                    setStyle("-fx-font-style: italic;");
                }

                long numGraded = list_students.stream().map(s -> {
                    try {
                        return SAM.db.getAnswer(s, exercise);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(a -> a != null && a.getPoints() != null).count();
                if (numGraded == list_students.size()) {     // all graded
                    setStyle("-fx-font-weight: bold");
                }
            }
        });

        if (controllerInstance == null) {
            controllerInstance = this;
        }

        try{
            Path dbFile = SAM.getPathFromConfigFile().resolve("db.sqlite3");
            SAM.db = new DB(dbFile);
            if (Files.exists(dbFile)) {
                working_dir.setText(SAM.getPathFromConfigFile().toString());
                list_students.clear();
                try {
                    for (Student student : SAM.db.getStudents()) {
                        if (student.getPdfpage()!=0) {
                            list_students.add(student);
                        }
                        student_matno_autocomplete.put(student.getMatno(), student);
                    }
                } catch (SQLException e) {
                    showError("DB Error: getStudents");
                }

                if (list_students.size() > 0) {
                    listView_students.getSelectionModel().select(0);
                    clickStudent(null);
                }

                list_exercises.clear();
                try {
                    for (Exercise exercise : SAM.db.getExercises()) {
                        list_exercises.add(exercise);
                    }
                } catch (SQLException e) {
                    showError("DB Error: getExercises");
                }
                FXCollections.sort(list_exercises, (a, b) -> a.compareTo(b));

                fullPageImagePane.widthProperty().addListener((obs, oldVal, newVal) -> {
                    fullPageImageView.setFitWidth(newVal.doubleValue());
                    refreshRectangles();
                });
                refreshTotalPoints();
                refreshProgress();

            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void initializeMenu() {
        Menu fileMenu = new Menu("File");
        menuBar.getMenus().addAll(fileMenu);
        MenuItem newProject = new MenuItem("New Project");
        fileMenu.getItems().add(newProject);

        Wizard wizard = new Wizard();
        newProject.setOnAction(event -> wizard.showWizard());

        Menu importMenu = new Menu("Import");
        menuBar.getMenus().addAll(importMenu);
        MenuItem importStudents = new MenuItem("Import Students");
        importMenu.getItems().add(importStudents);

        Menu exportMenu = new Menu("Export");
        menuBar.getMenus().addAll(exportMenu);
        MenuItem exportStudents = new MenuItem("Export Students");
        exportMenu.getItems().add(exportStudents);
        MenuItem exportFeedback = new MenuItem("Export Feedback");
        exportMenu.getItems().add(exportFeedback);

        exportStudents.setOnAction(this::exportStudents);
        exportMenu.setOnAction(this::exportFeedback);

        Menu moodleMenu = new Menu("Moodle");
        menuBar.getMenus().addAll(moodleMenu);
        MenuItem processMoodle = new MenuItem("Export student point to Moodle");
        moodleMenu.getItems().add(processMoodle);
        try{
            processMoodle.setOnAction(actionEvent -> {
                try {
                    importELOStudents();
                } catch (IOException | CsvException | SQLException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void importELOStudents() throws IOException, CsvException, SQLException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(SAM.getBase_dir().toFile());
        File selectedFile = fileChooser.showOpenDialog(fullPageBorderPane.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }
        String outputFile = SAM.getPathFromConfigFile() + "/studentsData.csv";
        try (CSVReader reader = new CSVReader(new FileReader(selectedFile));
             CSVWriter writer = new CSVWriter(new FileWriter(outputFile),
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.NO_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {
            List<Student> students = SAM.db.getStudents();
            List<Exercise> exercises = SAM.db.getExercises();
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                return;
            }
            String[] header = rows.get(0);
            int emailIndex = -1;
            int antwort1Index = -1;

            for (int i = 0; i < header.length; i++) {
                if (header[i].equalsIgnoreCase("E-Mail-Adresse")) {
                    emailIndex = i;
                } else if (header[i].equalsIgnoreCase("Antwort 1")) {
                    antwort1Index = i;
                }
            }
            if (emailIndex == -1 || antwort1Index == -1) {
                return;
            }

            String[] newHeader = {"email", "points", "feedback_text"};
            writer.writeNext(newHeader);

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                String email = row[emailIndex];
                String antwort1 = row[antwort1Index];
                Student matchedStudent = students.stream()
                        .filter(student -> student.getMatno().equals(antwort1))
                        .findFirst()
                        .orElse(null);
                if (matchedStudent != null) {
                    int totalPoints = 0;
                    StringBuilder feedbackBuilder = new StringBuilder();
                    for (Exercise exercise : exercises) {
                        Answer answer = SAM.db.getAnswer(matchedStudent, exercise);
                        if (answer.getPoints() != null) {
                            totalPoints += answer.getPoints().intValue();
                        }
                        if (answer.getFeedback() != null && !answer.getFeedback().isEmpty()) {
                            feedbackBuilder.append(answer.getFeedback()).append(" ");
                        }
                    }
                    String feedback = feedbackBuilder.toString().trim();
                    String[] newRow = {email, String.valueOf(totalPoints), feedback};
                    writer.writeNext(newRow);
                }
            }
            JOptionPane.showMessageDialog(null,
                    "The CSV file was created successfully: " + outputFile,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void clickStudent(MouseEvent mouseEvent) {
        Student student = (Student) listView_students.getSelectionModel().getSelectedItem();
        Page page_was = (Page) listView_pages.getSelectionModel().getSelectedItem();

        if (student == null) {
            return;
        }
        studentMatno.setText(student.getMatno() != null ? student.getMatno() : "");
        studentName.setText(student.getName() != null ? " " + student.toString() : "");

        /* fill pages list */
        list_pages.clear();
        for (Page page : student.getPages().values()) {
            list_pages.add(page);
            /* was this page selected before changing student? */
            if (page_was != null && page_was.getPageNo().equals(page.getPageNo())) {
                listView_pages.getSelectionModel().select(page);
            }
        }

        FXCollections.sort(list_pages, (a, b) -> a.compareTo(b));
        clickPage(null);
    }

    public void clickPage(MouseEvent mouseEvent) {
        Page page = (Page) listView_pages.getSelectionModel().getSelectedItem();
        if (page == null) {
            return;
        }

        /* show page image */
        try {
            fullPageImageView.setImage(page.getImage());
            //fullPageImageView.setFitHeight(fullPageBorderPane.getHeight()-50);
            fullPageImageView.setPreserveRatio(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        fullPageImageView.setFitWidth(fullPageImagePane.getWidth());

        /* show highlighted exercise boxes */
        refreshRectangles();
    }


    public void clickExercise(MouseEvent mouseEvent) {
        Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
        if (exercise == null) {
            return;
        }

        exerciseLabel.setText(exercise.getLabel());
        exercisePoints.setText(exercise.getPoints() == null ? "" : "" + exercise.getPoints());

        /* show exam page */
        if (listView_students.getSelectionModel().isEmpty()
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
            feedback_map = SAM.db.getFeedback(exercise);
            feedback_list = FXCollections.observableArrayList();
            feedback_map.forEach((feedback, points) -> feedback_list.add(feedback));
        } catch (SQLException e) {
            showError("DB Error: feedback for exercise " + exercise);
            return;
        }

        int num_students = 0;

        long random_seed = exercise.getId();
        List<Student> list_students_random = new ArrayList<>(list_students);
        Collections.shuffle(list_students_random, new Random(random_seed));

        for (Student student : list_students_random) {
            try {
                Answer answer = SAM.db.getAnswer(student, exercise);
                if (answerFilter == AnswerFilter.NOT_MARKTED && answer.getPoints() != null) {
                    continue;
                }
                if (answerFilter == AnswerFilter.COMPLETED && answer.getPoints() == null) {
                    continue;
                }

                num_students++;
                if (limit_show_answers.isSelected() && num_students > 10) {
                    break;
                }

                MarkingPane marking_pane = new MarkingPane(answer, feedback_map, feedback_list);
                marking_pane.setOnAnswer(student_answer -> refreshProgress());
                answers_list.getChildren().add(marking_pane);
            } catch (SQLException e) {
                showError("DB Error: Answer " + student + " / " + exercise);
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
            refreshProgress();
        }
    }

    public void changeExercise(KeyEvent keyEvent) {
        Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
        if (exercise == null) {
            return;
        }

        exercise.setLabel(exerciseLabel.getText());
        try {
            BigDecimal points = new BigDecimal(exercisePoints.getText());
            if (points.compareTo(BigDecimal.ZERO) >= 0) {
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
            SAM.db.persist(exercise);
        } catch (SQLException e) {
            showError("DB Error: " + exercise);
        }
        listView_exercises.refresh();
        FXCollections.sort(list_exercises, (a, b) -> a.compareTo(b));
    }

    private void refreshTotalPoints() {
        BigDecimal total = list_exercises.stream()
                .map(e -> e.getPoints())
                .filter(p -> p != null)
                .reduce((p1, p2) -> p1.add(p2))
                .orElse(BigDecimal.ZERO);
        total_points.setText(total.toString());
    }

    private void refreshProgress() {
        if (progress_tooltip == null) {
            progress_tooltip = new Tooltip();
            Tooltip.install(Controller.controllerInstance.progress, progress_tooltip);
        }

        try {
            progress.setProgress(1.0 * SAM.db.num_marked_answers()
                    / (list_students.size() * list_exercises.size()));
            progress_tooltip.setText(String.format("%.1f%%", progress.getProgress() * 100));
        } catch (SQLException e) {
            showError("DB error: refreshProgress");
        }

        int all = list_students.size();
        filter_answers_all.setText(filter_answers_all.getText().replaceFirst("\\(\\d*\\)", "(" + all + ")"));

        Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
        if (exercise == null) {
            return;
        }

        try {
            int completed = SAM.db.num_marked_answers(exercise);
            filter_answers_completed.setText(filter_answers_completed.getText().replaceFirst("\\(\\d*\\)", "(" + completed + ")"));
            int not_marked = all - completed;
            filter_answers_notmarked.setText(filter_answers_notmarked.getText().replaceFirst("\\(\\d*\\)", "(" + not_marked + ")"));
        } catch (SQLException e) {
            showError("DB error: refreshProgress");
        }

    }

    public void deleteExercise(ActionEvent actionEvent) {
        Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
        if (exercise == null) {
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Are you sure?");
        alert.setContentText("Do you want to delete exercise " + exercise + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() != ButtonType.OK) {
            return;
        }

        try {
            SAM.db.delete(exercise);
        } catch (SQLException e) {
            showError("DB Error: Deleting exercise " + exercise);
        }
        list_exercises.remove(exercise);
        listView_exercises.getSelectionModel().clearSelection();
        exerciseLabel.setText("");
        answers_list.getChildren().clear();
        refreshRectangles();
        refreshProgress();
    }


    public void fullPageImageDragStart(MouseEvent e) {
        image_click[0] = e.getX();
        image_click[1] = e.getY();
        e.setDragDetect(true);
    }

    private Rectangle dragRect;

    public void fullPageImageDragEvent(MouseEvent dragEvent) {
        double[] point1 = {image_click[0], image_click[1]};
        double[] point2 = {dragEvent.getX(), dragEvent.getY()};
        final double[][] pos = {point1, point2};
        if (FULL_WIDTH_EXERCISES) {
            pos[0][0] = 0;
            pos[1][0] = fullPageImageView.getImage().getWidth();
        }
        if (dragRect != null) {
            fullPageImagePane.getChildren().remove(dragRect);
        }
        dragRect = new Rectangle(point1[0], point1[1], point2[0] - point1[0], point2[1] - point1[1]);
        dragRect.setFill(Color.web("#8DD5F2", 0.2));
        fullPageImagePane.getChildren().add(dragRect);
    }

    public void fullPageImageDragStop(MouseEvent mouseEvent) {
        double[] point1 = {image_click[0], image_click[1]};
        double[] point2 = {mouseEvent.getX(), mouseEvent.getY()};
        point1 = windowPosToImagePos(point1);
        point2 = windowPosToImagePos(point2);
        final double[][] pos = {point1, point2};

        if (pos[0][0] == pos[1][0] && pos[0][1] == pos[1][1]) {
            /* no drag, just click (on rectangle?) */
            Page page = (Page) listView_pages.getSelectionModel().getSelectedItem();
            list_exercises.stream()
                    .filter(e -> e.containsPoint(pos[0]) && e.getPageNo().equals(page.getPageNo()))
                    .forEach(e -> listView_exercises.getSelectionModel().select(e));
            clickExercise(null);
            return;
        }

        if (FULL_WIDTH_EXERCISES) {
            pos[0][0] = 0;
            pos[1][0] = fullPageImageView.getImage().getWidth();
        }

        if (pos[1][0] <= pos[0][0] || pos[1][1] <= pos[0][1]) {
            /* negative rectangle size */
            return;
        }

        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Exercise");
        textInputDialog.setContentText("Exercise:");
        textInputDialog.showAndWait();
        String input = textInputDialog.getEditor().getText();
        if (dragRect != null) {
            fullPageImagePane.getChildren().remove(dragRect);
        }

        Page page = (Page) listView_pages.getSelectionModel().getSelectedItem();

        Exercise e = new Exercise(input, page.getPageNo(), pos);
        try {
            SAM.db.persist(e);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("DB Error: " + e);
        }
        list_exercises.add(e);
        FXCollections.sort(list_exercises, (a, b) -> a.compareTo(b));

        fullPageImagePane.getChildren().add(getRectangle(e));

        listView_exercises.getSelectionModel().select(e);
        clickExercise(null);
    }


    public void changeStudent(KeyEvent keyEvent) throws SQLException {
        Student student = (Student) listView_students.getSelectionModel().getSelectedItem();

        student.setMatno(studentMatno.getText().isBlank() ? "" : studentMatno.getText());

        if (student_matno_autocomplete.containsKey(student.getMatno())) {
            Student existingStudent = student_matno_autocomplete.get(student.getMatno());
            listView_students.getItems().removeIf(s -> s.equals(existingStudent));
            SAM.db.delete(existingStudent);
            student.fusion(existingStudent);
            new Thread(() -> TextToSpeech.speak(student.getName())).start();
        }

        try {
            SAM.db.persist(student);
        } catch (SQLException e) {
            showError("DB Error: " + student);
        }
        FXCollections.sort(list_students, (a, b) -> a.compareTo(b));
        listView_students.refresh();
    }

    public void chooseDir(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(fullPageBorderPane.getScene().getWindow());
        working_dir.setText(selectedDirectory.getAbsolutePath());
        loadDir(actionEvent);
    }

    public void loadDir(ActionEvent actionEvent) {
        Path newDir = Paths.get(working_dir.getText());
        if (working_dir.getText().isBlank() || !Files.exists(newDir) || !Files.isDirectory(newDir)) {
            showError("Invalid directory");
            return;
        }
        try {
            SAM.updatePathInConfigFile(newDir);
        } catch (IOException e) {
            System.out.println("[WARN] Could not store directory in config file!");
        }
        SAM.setBase_dir(newDir);
        //initialize();
    }

    public void importPDF(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(SAM.getBase_dir().toFile());
        File selectedFile = fileChooser.showOpenDialog(fullPageBorderPane.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }
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
        Thread thread = new Thread(() -> {
            try {
                PDFTools.splitPDF(selectedFile, numpages);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //Platform.runLater(() -> initialize());
        });
        thread.start();
    }

    private void refreshRectangles() {
        fullPageImagePane.getChildren().removeIf(n -> n instanceof Rectangle || n instanceof Text);

        Page page = (Page) listView_pages.getSelectionModel().getSelectedItem();
        if (page != null) {
            list_exercises.stream()
                    .filter(e -> e.getPageNo().equals(page.getPageNo()))
                    .forEach(exercise -> {
                        fullPageImagePane.getChildren().add(getRectangle(exercise));
                        Text pointsText = getPointsText(exercise);
                        if (pointsText != null) {
                            fullPageImagePane.getChildren().add(pointsText);
                        }
                    });
        }
    }


    public Rectangle getRectangle(Exercise e) {
        double[] point1 = {e.getPos()[0][0], e.getPos()[0][1]};
        double[] point2 = {e.getPos()[1][0], e.getPos()[1][1]};
        point1 = imagePosToWindowPos(point1);
        point2 = imagePosToWindowPos(point2);
        Rectangle rectangle = new Rectangle(point1[0], point1[1], point2[0] - point1[0], point2[1] - point1[1]);
        if (e.equals(listView_exercises.getSelectionModel().getSelectedItem())) {
            rectangle.setFill(Color.web("#0583F2", 0.2));
        } else {
            rectangle.setFill(Color.web("#8DD5F2", 0.2));
        }
        return rectangle;
    }

    public Text getPointsText(Exercise exercise) {
        // point2 is the rectangle point at the bottom right
        double[] point2 = {exercise.getPos()[1][0], exercise.getPos()[1][1]};
        point2 = imagePosToWindowPos(point2);

        Student student = (Student) listView_students.getSelectionModel().getSelectedItem();
        if (student == null) {
            return null;
        }

        Answer answer;
        try {
            answer = SAM.db.getAnswer(student, exercise);
        } catch (SQLException e) {
            return null;
        }

        BigDecimal points = answer.getPoints();
        if (points == null) {
            return null;
        }

        Text text = new Text(point2[0] - 20, point2[1] - 5, "" + points);
        text.setFill(Color.RED);
        text.setFont(Font.font(null, FontWeight.BOLD, 14));
        return text;
    }

    private double[] windowPosToImagePos(double[] pos) {
        double factor = fullPageImageView.getImage().getWidth() / fullPageImageView.getFitWidth();
        double[] imagePos = {pos[0] * factor, pos[1] * factor};
        return imagePos;
    }

    private double[] imagePosToWindowPos(double[] pos) {
        double factor = fullPageImageView.getFitWidth() / fullPageImageView.getImage().getWidth();
        double[] windowPos = {pos[0] * factor, pos[1] * factor};
        return windowPos;
    }

    private void showError(String msg) {
        Alert a = new Alert(AlertType.ERROR);
        a.setContentText(msg);
        a.show();
    }

    public void imageKeyReleased(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.DELETE) {
            Page page = (Page) listView_pages.getSelectionModel().getSelectedItem();
            Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
            if (page != null && exercise != null) {
                deleteExercise(null);
            }
        }
    }

    public void resizeExercise(ActionEvent actionEvent) {
        if (!(actionEvent.getSource() instanceof Button)) {
            return;
        }

        Exercise exercise = (Exercise) listView_exercises.getSelectionModel().getSelectedItem();
        if (exercise == null) {
            return;
        }

        double pos[][] = exercise.getPos();
        final double step = 10;

        String action = ((Button) actionEvent.getSource()).getText();
        switch (action) {
            case "↥+":
                pos[0][1] -= step;
                break;
            case "↥-":
                pos[0][1] += step;
                break;
            case "↧+":
                pos[1][1] += step;
                break;
            case "↧-":
                pos[1][1] -= step;
                break;
            case "↤+":
                pos[0][0] -= step;
                break;
            case "↤-":
                pos[0][0] += step;
                break;
            case "↦+":
                pos[1][0] += step;
                break;
            case "↦-":
                pos[1][0] -= step;
                break;
        }
        exercise.setPos(pos);
        try {
            SAM.db.persist(exercise);
        } catch (SQLException e) {
            showError("Error resizing exercise: " + exercise);
        }
        refreshRectangles();
    }

    public void importStudents(ActionEvent actionEvent) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Import Students");
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(new Label("Copy/Paste tab-separated list of students' matno and name (e.g. from Excel), one student per line. Optionally name separated in two columns."));
        TextArea students_textarea = new TextArea();
        borderPane.setCenter(students_textarea);

        ButtonType importButtonType = new ButtonType("Import Students", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(importButtonType);

        dialog.setResultConverter(dialogButton -> students_textarea.getText());

        dialog.getDialogPane().setContent(borderPane);
        Platform.runLater(() -> students_textarea.requestFocus());

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            for (String line : result.get().split("\n")) {
                String[] parts = line.split("\t");
                if (parts.length < 2) {
                    continue;
                }
                Student student = new Student(parts[0]); // matno
                student.setName1(parts[1]); // name1
                if (parts.length >= 3) {
                    student.setName2(parts[2]);
                }
                student_matno_autocomplete.put(student.getMatno(), student);
                list_students.add(student);
            }

            for (Student student : list_students) {
                if (student_matno_autocomplete.containsKey(student.getMatno())) {
                    student.fusion(student_matno_autocomplete.get(student.getMatno()));
                    try {
                        SAM.db.persist(student);
                    } catch (SQLException e) {
                        showError("Error setting student name: " + student);
                    }
                }
                listView_students.refresh();
            }
        }
    }

    public void exportStudents(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName("grades.csv");
        fileChooser.setInitialDirectory(SAM.getBase_dir().toFile());

        File selectedFile = fileChooser.showSaveDialog(fullPageBorderPane.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        URI uri = selectedFile.toURI();
        if (!uri.toString().endsWith(".csv") && !uri.toString().endsWith(".txt")) {
            try {
                uri = new URI(uri.toString() + ".csv");
            } catch (URISyntaxException e) {
            }
        }
        CSVExporter csvExporter = new CSVExporter();
        try {
            csvExporter.exportCSV(Paths.get(uri));
        } catch (Exception e) {
            showError("CSV-Export Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void exportFeedback(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("HTML files (*.html)", "*.html");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName("feedback.html");
        fileChooser.setInitialDirectory(SAM.getBase_dir().toFile());

        File selectedFile = fileChooser.showSaveDialog(fullPageBorderPane.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        URI uri = selectedFile.toURI();
        if (!uri.toString().endsWith(".html") && !uri.toString().endsWith(".htm")) {
            try {
                uri = new URI(uri.toString() + ".html");
            } catch (URISyntaxException e) {
            }
        }

        FeedbackExporter feedbackExporter = new FeedbackExporter();
        try {
            feedbackExporter.exportFeedback(Paths.get(uri));
        } catch (Exception e) {
            showError("Feedback-Export Error: " + e.getMessage());
            e.printStackTrace();
        }

        SAM.open_browser(uri.toString());
    }

    public void changeExerciseSettings(ActionEvent actionEvent) {
        clickExercise(null);
    }

    public void filterAnswers(ActionEvent actionEvent) {
        RadioButton radioButton = (RadioButton) filter_answers.getSelectedToggle();
        this.answerFilter = switch (radioButton.getId()) {
            case "filter_answers_notmarked" -> AnswerFilter.NOT_MARKTED;
            case "filter_answers_completed" -> AnswerFilter.COMPLETED;
            default -> AnswerFilter.ALL;
        };
        clickExercise(null);
    }

    public static void setProgress(double progress) {
        Controller.controllerInstance.progress.setProgress(progress);
    }

}
