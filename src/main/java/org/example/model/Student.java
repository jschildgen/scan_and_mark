package org.example.model;

import javafx.scene.image.Image;
import org.example.SAM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Student implements Comparable<Student> {
    private Integer id;
    private String matno;
    private String name1;
    private String name2;
    private Map<String, Page> pages = null;
    private Integer pdfpage;
    private Integer prcnt;
    private String qrcode;

    public Student(Integer id) {
        this(id, null);
    }

    public Student(String matno) {
        this.matno = matno;
    }

    public Student(Integer id, String matno) {
        this.id = id;
        this.matno = matno;

        /* set student's pages */
        this.pages = new LinkedHashMap<>();

        try {
            Files.newDirectoryStream(SAM.getPathFromConfigFile().resolve(""+this.getId())).forEach((Path p) -> {
                Page page = new Page(p);
                this.pages.put(page.getPageNo(), page);
            });
        } catch (IOException e) {
            //System.out.println("Number of students and number of imported pages doesnt match. File " + e.getMessage() + " doesnt match any student");
            //it's ok, if not for all students a folder exists
            //that's the case for imported students before matching them with their exams
        }
    }

    public Student(int examId, Integer pdfpage, Integer prcnt) {
        this(examId);
        this.pdfpage = pdfpage;
        this.prcnt = prcnt;
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

    public Integer getPdfpage() {
        return pdfpage;
    }

    public void setPdfpage(Integer pdfpage) {
        this.pdfpage = pdfpage;
    }

    public Integer getPrcnt() {
        return prcnt;
    }

    public void setPrcnt(Integer prcnt) {
        this.prcnt = prcnt;
    }

    public String getQrcode() {
        return qrcode;
    }

    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
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
        if(SAM.anonymous_mode) {
            return "***";
        }
        if(this.matno == null) {
            return "#"+id;
        }
        if(this.getName() == null || this.getName().isBlank()) {
            return this.matno;
        }
        return String.format("%s (%s)", this.matno, this.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return id.equals(student.id) && Objects.equals(matno, student.matno) && Objects.equals(name1, student.name1) && Objects.equals(name2, student.name2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, matno, name1, name2);
    }

    @Override
    public int compareTo(Student that) {
        if(this.matno == null && that.matno == null) { return this.id.compareTo(that.id); }
        if(this.matno == null) { return -1; }
        if(that.matno == null) { return 1; }
        try {
            int thisMatNo = Integer.parseInt(this.matno);
            int thatMatNo = Integer.parseInt(that.matno);
            return thisMatNo - thatMatNo;
        } catch (Exception e) {
            return this.matno.compareTo(that.matno);
        }
    }

    public void fusion(Student other) {
        this.matno = other.matno;
        this.name1 = other.name1;
        this.name2 = other.name2;
    }
}
