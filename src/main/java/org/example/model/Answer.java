package org.example.model;

import java.math.BigDecimal;

public class Answer {
    private Student student;
    private Exercise exercise;
    private BigDecimal points;
    private String feedback;

    public Answer(Student student, Exercise exercise) {
        this.student = student;
        this.exercise = exercise;
    }

    public Student getStudent() {
        return student;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
