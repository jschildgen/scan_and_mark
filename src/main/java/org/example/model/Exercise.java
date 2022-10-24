package org.example.model;

import org.example.QRexam;

import java.math.BigDecimal;
import java.sql.SQLException;

public class Exercise implements Comparable<Exercise> {
    private Integer id;
    private String label;
    private String pageNo;
    private BigDecimal points;
    private double[][] pos;

    public Exercise(Integer id, String label, String pageNo, double[][] pos) {
        this.id = id;
        this.label = label;
        this.pageNo = pageNo;
        this.pos = pos;
    }

    public Exercise(String label, String pageNo, double[][] pos) {
        this.label = label;
        this.pageNo = pageNo;
        this.pos = pos;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }

    public double[][] getPos() {
        return pos;
    }

    public void setPos(double[][] pos) {
        this.pos = pos;
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

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Exercise other) {
            return this.id == other.id;
        }
        return false;
    }
}
