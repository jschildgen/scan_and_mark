package org.example.importexport;

import org.example.model.Student;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextCSVStudentImporter implements StudentImporter {
    private String csvdata;

    public TextCSVStudentImporter(String csvdata) {
        this.csvdata = csvdata;
    }

    @Override
    public List<Student> import_students() throws IOException {
        List<Student> students = new ArrayList<>();
        if (!csvdata.isEmpty()) {
            for (String line : csvdata.split("\n")) {
                String[] parts = line.split("\t");
                if (parts.length < 2) {
                    continue;
                }
                Student student = new Student(parts[0]);
                student.setName1(parts[1]);
                if (parts.length >= 3) {
                    student.setName2(parts[2]);
                }
                students.add(student);
            }
        }
        System.out.println(this.getClass().getName()+": Imported "+students.size()+" students.");
        return students;
    }

    public static boolean format_is_valid(Object o) {
        if (!(o instanceof String csvdata)) {
            return false; // Ensure the input is a String
        }

        if (csvdata.isEmpty()) {
            return false;
        }

        String[] lines = csvdata.split("\n");
        for (String line : lines) {
            String[] parts = line.split("\t");
            if (parts.length < 2) {
                return false;
            }
        }

        return true;
    }
}
