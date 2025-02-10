package org.example.importexport;

import org.example.model.Student;

import java.io.IOException;
import java.util.List;

public interface StudentImporter {
    List<Student> import_students() throws IOException;

    static boolean format_is_valid(Object o) {
        return false;
    }
}