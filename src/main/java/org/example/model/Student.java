package org.example.model;

import javafx.scene.image.Image;
import org.example.QRexam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Student implements Comparable<Student> {
    private String id;
    private String name;
    private Map<String, Page> pages = null;

    public Student(String id) {
        this.id = id;

        /* set student's pages */
        this.pages = new LinkedHashMap<>();
        try {
            Files.newDirectoryStream(QRexam.base_dir.resolve(this.getId())).forEach((Path p) -> {
                Page page = new Page(p);
                this.pages.put(page.getPageNo(), page);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Page> getPages() {
        return pages;
    }

    public Page getPage(String pageNo) {
        return pages.get(pageNo);
    }

    public Image getAnswerImage(Exercise exercise) throws IOException {
        return pages.get(exercise.getPageNo()).getAnswerImage(exercise);
    }

    @Override
    public String toString() {
        if(this.name == null || this.name.isBlank()) {
            return this.id;
        }
        return String.format("%s (%s)", this.id, this.name);
    }

    @Override
    public int compareTo(Student that) {
        try {
            int thisId = Integer.parseInt(this.id);
            int thatId = Integer.parseInt(that.id);
            return thisId - thatId;
        } catch (Exception e) {
            return this.id.compareTo(that.id);
        }
    }
}
