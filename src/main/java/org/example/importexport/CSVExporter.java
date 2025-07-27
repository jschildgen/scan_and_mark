package org.example.importexport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.example.SAM;
import org.example.model.Answer;
import org.example.model.Exercise;
import org.example.model.Student;

import java.io.FileWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

public class CSVExporter {
    public void exportCSV(Path path) throws Exception {
        FileWriter writer = new FileWriter(path.toString());
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        List<Exercise> exercises = SAM.db.getExercises();
        List<String> header_row = new ArrayList<>();
        header_row.add("matno");
        header_row.add("name1");
        header_row.add("name2");

        List<String> exercise_label_numbers = new ArrayList<>();   // 1, 2, 3, ... (not 1a, etc.)

        int i = 0;
        for (Student student : SAM.db.getStudents()) {
            List<String> row = new ArrayList<>();
            row.add(student.getMatno());
            row.add(student.getName1());
            row.add(student.getName2());
            String current_exercise = null;
            Map<String,BigDecimal> exercise_points = new LinkedHashMap<>();  // total points for each full exercise (1 = 1a + 1b + ...)
            BigDecimal total_points = BigDecimal.ZERO;  // total points of the whole exam
            for(Exercise exercise : exercises) {
                String label_number = exercise.getLabel().replaceAll("\\D", "");
                if (!label_number.equals(current_exercise)) {  // next exercise
                    current_exercise = label_number;
                    if(i == 0) {
                        exercise_label_numbers.add(label_number);
                    }
                    exercise_points.put(label_number, BigDecimal.ZERO);
                }

                if (i == 0) {
                    header_row.add(exercise.getLabel());
                }
                Answer answer = SAM.db.getAnswer(student, exercise);
                if (answer == null || answer.getPoints() == null) {
                    throw new Exception("Incomplete! Answer not found for student " + student + " and exercise " + exercise);
                }
                row.add(answer.getPoints().toString());
                exercise_points.put(label_number, exercise_points.get(label_number).add(answer.getPoints() == null ? BigDecimal.ZERO : answer.getPoints()));
                total_points = total_points.add(answer.getPoints() == null ? BigDecimal.ZERO : answer.getPoints());
            }
            if (i == 0) {
                header_row.add("");
                header_row.addAll(exercise_label_numbers);
                header_row.add("");
                header_row.add("total");
                header_row.add("");
                header_row.add("");
                csvPrinter.printRecord(header_row);
            }
            row.add("");
            for (String label_number : exercise_label_numbers) {
                row.add(exercise_points.get(label_number).toString());
            }
            row.add("");
            row.add(total_points.toString());
            row.add("");
            row.add(student.getQrcode() == null ? "" : student.getQrcode());
            csvPrinter.printRecord(row);
            i++;
        }

        csvPrinter.flush();
        csvPrinter.close();
    }
}
