package org.example.importexport;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.example.SAM;
import org.example.model.Answer;
import org.example.model.Exercise;
import org.example.model.Student;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoodleFeedbackExporter {

    private final File inputFile;

    public MoodleFeedbackExporter(File inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * Export feedback for Moodle.
     * Reads a CSV file with student data and generates a new CSV file
     * with feedback information for each student.
     *
     * @return the output file name
     */
    public String exportFeedback() throws IOException {

        try (CSVReader reader = new CSVReader(new FileReader(inputFile));
             CSVWriter writer = new CSVWriter(new FileWriter(getOutputFileName()),
                     ',', '"', '\\', "\n")) {

            List<Exercise> exercises = SAM.db.getExercises();
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                throw new IOException("Input file contains no data.");
            }
            String[] header = rows.get(0);
            int username_index = 2;
            int matno_index = header.length-1;

            String[] newHeader = {"username", "points", "feedback_text"};
            writer.writeNext(newHeader);

            Set<String> usernames = new HashSet<>();  // avoid duplicate emails

            // For each student in input CSV file
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                String username = row[username_index];
                String matno = row[matno_index];
                Student student = SAM.db.getStudentByMatno(matno);
                if (student != null) {
                    if (usernames.contains(username)) { continue; }
                    usernames.add(username);
                    int totalPoints = 0;

                    StringBuilder feedback = new StringBuilder();

                    BigDecimal student_points = BigDecimal.ZERO;
                    String current_exercise = null;
                    boolean exercise_incomplete = false;
                    boolean exam_incomplete = false;
                    BigDecimal sum_points = BigDecimal.ZERO;    // total points of the full exercise (1a + 1b + ...)
                    BigDecimal max_points = BigDecimal.ZERO;    // max points of the full exercise (1a + 1b + ...)
                    BigDecimal exam_max_points = BigDecimal.ZERO; // max points of the exam (1 + 2 + ...)
                    for(Exercise exercise : exercises) {
                        String label_number = exercise.getLabel().replaceAll("\\D", "");
                        if (!label_number.equals(current_exercise)) { // next exercise
                            if (current_exercise != null) {
                                feedback.append(current_exercise).append(": ").append(exercise_incomplete ? "?" : sum_points).append("/").append(max_points).append("\n");
                            }

                            current_exercise = label_number;
                            sum_points = BigDecimal.ZERO;
                            max_points = BigDecimal.ZERO;
                            exercise_incomplete = false;
                        }
                        Answer answer = SAM.db.getAnswer(student, exercise);
                        if (answer == null || answer.getPoints() == null) {
                            exercise_incomplete = true;
                            exam_incomplete = true;
                        }
                        sum_points = sum_points.add(answer.getPoints() == null ? BigDecimal.ZERO : answer.getPoints());
                        max_points = max_points.add(exercise.getPoints() == null ? BigDecimal.ZERO : exercise.getPoints());
                        exam_max_points = exam_max_points.add(exercise.getPoints() == null ? BigDecimal.ZERO : exercise.getPoints());
                        student_points = student_points.add(answer.getPoints() == null ? BigDecimal.ZERO : answer.getPoints());
                    }

                    if(current_exercise != null) {
                        feedback.append(current_exercise).append(": ").append(exercise_incomplete ? "?" : sum_points).append("/").append(max_points).append("\n");
                    }

                    feedback.append("=> ").append(exam_incomplete ? "?" : student_points).append("/").append(exam_max_points);

                    String[] newRow = {username, String.valueOf(totalPoints), feedback.toString()};
                    writer.writeNext(newRow);
                }
            }
            writer.flush();

            return getOutputFileName();

        } catch (SQLException e) {
            throw new IOException("Error accessing database", e);
        } catch (CsvException e) {
            throw new IOException("Error reading CSV file", e);
        }
    }

    /**
     * Get the output file name based on the input file name.
     * e.g. /this/path/a21123.csv => /this/path/feedback_a21123.csv
     * @return the output file name
     */
    private String getOutputFileName() {
        final String PREFIX = "feedback_";

        String inputFileName = inputFile.getName();
        String inputFilePath = inputFile.getAbsolutePath();
        String inputFileDir = inputFilePath.substring(0, inputFilePath.lastIndexOf(File.separator));
        String inputFileBaseName = inputFileName.substring(0, inputFileName.lastIndexOf('.'));
        String inputFileExtension = inputFileName.substring(inputFileName.lastIndexOf('.'));
        String outputFileName = PREFIX + inputFileBaseName + inputFileExtension;
        return inputFileDir + File.separator + outputFileName;
    }
}
