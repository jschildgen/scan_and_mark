package org.example.model;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Exercise implements Comparable<Exercise> {
    private String label;
    private String pageNo;
    private double[][] pos;

    public Exercise(String label, String pageNo, double[][] pos) {
        this.label = label;
        this.pageNo = pageNo;
        this.pos = pos;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPageNo() {
        return this.pageNo;
    }

    public double[][] getPos() {
        return pos;
    }

    @Override
    public String toString() {
        return this.label;
    }

    @Override
    public int compareTo(Exercise that) {
        try {
            int thisId = Integer.parseInt(this.label);
            int thatId = Integer.parseInt(that.label);
            return thisId - thatId;
        } catch (Exception e) {
            return this.label.compareTo(that.label);
        }
    }

    public boolean containsPoint(double[] point) {
        return     this.pos[0][0] <= point[0] && this.pos[1][0] >= point[0]
                && this.pos[0][1] <= point[1] && this.pos[1][1] >= point[1];
    }
}
