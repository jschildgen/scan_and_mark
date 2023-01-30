package org.example;

import javafx.util.Pair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.example.model.Answer;
import org.example.model.Exercise;
import org.example.model.Student;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

public class CSVExporter {
    public void exportCSV(Path path) throws SQLException, IOException {
        FileWriter writer = new FileWriter(path.toString());
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        for (Map.Entry<Student, BigDecimal> entry : SAM.db.getStudentsWithPoints().entrySet()) {
            Student student = entry.getKey();
            BigDecimal points = entry.getValue();
            csvPrinter.printRecord(
                    student.getMatno(),
                    student.getName1(),
                    student.getName2(),
                    points);
        }

        csvPrinter.flush();
        csvPrinter.close();
    }
}
