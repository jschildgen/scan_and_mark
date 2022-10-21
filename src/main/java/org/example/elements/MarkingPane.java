package org.example.elements;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.example.model.Student;

public class MarkingPane extends BorderPane {
    public MarkingPane(Student student) {
        Label student_label = new Label(" "+student.toString()+" ");
        student_label.setStyle("-fx-font-weight: bold");
        this.setLeft(student_label);
        BorderPane.setAlignment(student_label, Pos.CENTER_LEFT);

        Button button0points = new Button("0");
        Button buttonMaxpoints = new Button("4");
        TextField points_field = new TextField();
        points_field.setPrefWidth(35);
        Separator separator = new Separator();
        separator.setPrefWidth(18);
        HBox points_pane = new HBox(button0points, points_field, buttonMaxpoints, separator);
        this.setRight(points_pane);

        ComboBox<String> feedback_field = new ComboBox<>();
        feedback_field.setEditable(true);
        feedback_field.setMaxWidth(Double.MAX_VALUE);
        this.setCenter(feedback_field);
    }
}
