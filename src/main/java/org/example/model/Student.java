package org.example.model;

import javafx.scene.image.Image;
import org.example.QRexam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Student implements Comparable<Student> {
    private Integer id;
    private String matno;
    private String name1;
    private String name2;
    private Map<String, Page> pages = null;

    public Student(Integer id) {
        this(id, null);
    }

    public Student(Integer id, String matno) {
        this.id = id;
        this.matno = matno;

        /* set student's pages */
        this.pages = new LinkedHashMap<>();
        try {
            Files.newDirectoryStream(QRexam.getBase_dir().resolve(""+this.getId())).forEach((Path p) -> {
                Page page = new Page(p);
                this.pages.put(page.getPageNo(), page);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMatno() {
        return matno;
    }

    public void setMatno(String matno) {
        this.matno = matno;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }

    public String getName() {
        if(name1 == null) { return null; }
        if(name2 == null) { return name1; }
        return name1+" "+name2;
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
        if(this.matno == null) {
            return "#"+id;
        }
        if(this.getName() == null || this.getName().isBlank()) {
            return this.matno;
        }
        return String.format("%s (%s)", this.matno, this.getName());
    }

    @Override
    public int compareTo(Student that) {
        try {
            int thisMatNo = Integer.parseInt(this.matno);
            int thatMatNo = Integer.parseInt(that.matno);
            return thisMatNo - thatMatNo;
        } catch (Exception e) {
            return this.matno.compareTo(that.matno);
        }
    }
}
