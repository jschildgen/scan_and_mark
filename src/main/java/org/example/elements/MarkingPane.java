package org.example.elements;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.example.QRexam;
import org.example.model.Answer;
import org.example.model.Exercise;
import org.example.model.Student;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MarkingPane extends BorderPane {
    private Consumer<Answer> onAnswer;

    public MarkingPane(Student student, Exercise exercise, Map<String, BigDecimal> feedback_map, ObservableList<String> feedback_list) throws SQLException {
        Answer answer = QRexam.db.getAnswer(student, exercise);

        //ObservableList<String> feedback_list = FXCollections.observableArrayList();
        //feedback_items.forEach((feedback, points) -> feedback_list.add(feedback));

        Label student_label = new Label(" "+student.toString()+" ");
        student_label.setStyle("-fx-font-weight: bold");
        this.setLeft(student_label);
        BorderPane.setAlignment(student_label, Pos.CENTER_LEFT);

        TextField points_field = new TextField();
        points_field.setText(answer.getPoints() != null ? ""+answer.getPoints() : "");
        points_field.setPrefWidth(40);

        points_field.setOnMouseClicked(e -> points_field.selectAll());

        ComboBox<String> feedback_field = new ComboBox<>();

        EventHandler pointsChangedHandler = e -> {
            if(points_field.getText().contains(",")) {
                points_field.setText(points_field.getText().replaceFirst(",","."));
                points_field.positionCaret(points_field.getText().length());
            }
            try {
                BigDecimal points = new BigDecimal(points_field.getText());
                if(points.compareTo(BigDecimal.ZERO) < 0            // negative points
                || points.compareTo(exercise.getPoints()) > 0) {    // more points than max. for this exercise
                    points_field.setText("");
                    points = null;
                }

                answer.setPoints(points);

            } catch (NumberFormatException ex) {
                answer.setPoints(null);
                points_field.setText("");
            }

            if(!feedback_field.getValue().isBlank()) {
                answer.setFeedback(feedback_field.getValue());
            } else {
                answer.setFeedback(null);
            }

            if(e != null) {
                System.out.println(e.getSource());
            }

            if(e.getSource() instanceof ComboBox   /* feedback was changed => autofill points */
               &&     answer.getPoints() == null && feedback_map.containsKey(answer.getFeedback())
                                        && feedback_map.get(answer.getFeedback()) != null) {
                answer.setPoints(feedback_map.get(answer.getFeedback()));
                points_field.setText(""+answer.getPoints());
            }

            try {
                QRexam.db.persist(answer);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }

            this.onAnswer.accept(answer);
        };
        points_field.setOnKeyReleased(e -> {
            pointsChangedHandler.handle(e);
            feedback_map.put(answer.getFeedback(), answer.getPoints());
        });

        List<Button> points_buttons = new ArrayList<>();
        if(exercise.getPoints() != null) {
            for (BigDecimal i = BigDecimal.ZERO; i.compareTo(exercise.getPoints()) < 0; i = i.add(BigDecimal.ONE)) {
                Button button = new Button("" + i);
                button.setOnAction(e -> {
                    points_field.setText(button.getText());
                    pointsChangedHandler.handle(e);
                    feedback_map.put(answer.getFeedback(), answer.getPoints());
                });
                points_buttons.add(button);
            }
            Button button = new Button("" + exercise.getPoints());
            button.setOnAction(e -> {
                points_field.setText(button.getText());
                pointsChangedHandler.handle(e);
            });
            points_buttons.add(button);
        }

        Separator separator = new Separator();
        separator.setPrefWidth(18);
        HBox points_pane = new HBox();
        points_pane.getChildren().addAll(points_buttons);
        points_pane.getChildren().add(points_field);
        points_pane.getChildren().add(separator);
        this.setRight(points_pane);

        feedback_field.setValue(answer.getFeedback() != null ? answer.getFeedback() : "");
        feedback_field.setEditable(true);
        feedback_field.setMaxWidth(Double.MAX_VALUE);
        feedback_field.setItems(feedback_list);
        feedback_field.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            pointsChangedHandler.handle(new Event(feedback_field, null, null));
        });
        feedback_field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                pointsChangedHandler.handle(new Event(feedback_field, null, null));
                if(answer.getFeedback() != null
                        && !feedback_list.contains(answer.getFeedback())) {
                    feedback_list.add(answer.getFeedback());
                    feedback_map.put(answer.getFeedback(), answer.getPoints());
                }
            }
        });
        this.setCenter(feedback_field);
    }

    public void setOnAnswer(Consumer<Answer> onAnswer) {
        this.onAnswer = onAnswer;
    }
}
