package org.example;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import org.example.model.Answer;
import org.example.model.Exercise;
import org.example.model.Student;

import java.io.*;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class FeedbackExporter {

    public void exportFeedback(Path path) throws IOException, TemplateException, URISyntaxException, SQLException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);

        cfg.setDirectoryForTemplateLoading(new File(Paths.get(this.getClass().getResource("feedback.ftl").toURI()).getParent().toUri()));

        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.US);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        Template template = cfg.getTemplate("feedback.ftl");

        Map<String, Object> input = new HashMap<String, Object>();

        List<Object> students_obj = new ArrayList<>();

        List<Exercise> exercises = QRexam.db.getExercises();

        BigDecimal exam_max_points = BigDecimal.ZERO;

        for(Student student : QRexam.db.getStudents()) {
            Map<String, Object> student_obj = new LinkedHashMap<>();
            student_obj.put("student", student);
            BigDecimal student_points = BigDecimal.ZERO;

            List<Object> exercises_obj = new ArrayList<>();
            List<Object> sub_exercises_obj = new ArrayList<>();
            String current_exercise = null;
            Map<String, Object> exercise_obj = null;
            BigDecimal sum_points = BigDecimal.ZERO;
            BigDecimal max_points = BigDecimal.ZERO;
            for(Exercise exercise : exercises) {
                String label_number = exercise.getLabel().replaceAll("\\D", "");
                if(!label_number.equals(current_exercise)) { // next exercise
                    current_exercise = label_number;
                    if(exercise_obj != null) {
                        exercise_obj.put("sub_exercises", sub_exercises_obj);
                        exercise_obj.put("sum_points", sum_points);
                        exercise_obj.put("max_points", max_points);
                        exercises_obj.add(exercise_obj);
                        if(!input.containsKey("exam_max_points")) {
                            input.put("exam_max_points", exam_max_points);
                        }
                    }
                    exercise_obj = new LinkedHashMap<>();
                    exercise_obj.put("label_number", label_number);
                    sub_exercises_obj = new ArrayList<>();
                    sum_points = BigDecimal.ZERO;
                    max_points = BigDecimal.ZERO;
                }
                Map<String, Object> sub_exercise_obj = new LinkedHashMap<>();
                sub_exercise_obj.put("sub_exercise", exercise);
                Answer answer = QRexam.db.getAnswer(student, exercise);
                sub_exercise_obj.put("answer", answer);
                sum_points = sum_points.add(answer.getPoints());
                max_points = max_points.add(exercise.getPoints());
                student_points = student_points.add(answer.getPoints());
                if(!input.containsKey("exam_max_points")) {
                    exam_max_points = exam_max_points.add(exercise.getPoints());
                }
                sub_exercises_obj.add(sub_exercise_obj);
            }
            exercise_obj.put("sub_exercises", sub_exercises_obj);
            exercise_obj.put("sum_points", sum_points);
            exercise_obj.put("max_points", max_points);
            exercises_obj.add(exercise_obj);

            student_obj.put("exercises", exercises_obj);
            student_obj.put("points", student_points);
            students_obj.add(student_obj);
        }

        if(!input.containsKey("exam_max_points")) {
            input.put("exam_max_points", exam_max_points);
        }

        input.put("students", students_obj);

        /*Writer consoleWriter = new OutputStreamWriter(System.out);
        template.process(input, consoleWriter);*/

        Writer fileWriter = new FileWriter(path.toFile());
        try {
            template.process(input, fileWriter);
        } finally {
            fileWriter.close();
        }
    }
}
