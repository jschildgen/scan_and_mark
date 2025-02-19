package org.example;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
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

public class FeedbackExporter {

    public void exportFeedback(Path path) throws IOException, TemplateException, URISyntaxException, SQLException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);

        Template template;
        try {
            // For JAR execution
            cfg.setClassForTemplateLoading(this.getClass(), "/org/example");
            cfg.setDefaultEncoding("UTF-8");
            cfg.setLocale(Locale.US);
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            template = cfg.getTemplate("feedback.ftl");
        } catch (Exception e) {
            // For IDE execution
            cfg.setTemplateLoader(new FileTemplateLoader(new File("src/main/java/org/example/")));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setLocale(Locale.US);
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            template = cfg.getTemplate("feedback.ftl");
        }

        Map<String, Object> input = new HashMap<String, Object>();

        input.put("exam_path", SAM.getBase_dir().toString());

        List<Object> students_obj = new ArrayList<>();

        List<Exercise> exercises = SAM.db.getExercises();

        for(Student student : SAM.db.getStudents()) {
            if(student.getPages().isEmpty()) {
                continue;
            }
            Map<String, Object> student_obj = new LinkedHashMap<>();
            student_obj.put("student", student);
            BigDecimal student_points = BigDecimal.ZERO;

            List<Object> exercises_obj = new ArrayList<>();
            List<Object> sub_exercises_obj = new ArrayList<>();
            String current_exercise = null;
            Map<String, Object> exercise_obj = null;
            BigDecimal sum_points = BigDecimal.ZERO;    // total points of the full exercise (1a + 1b + ...)
            BigDecimal max_points = BigDecimal.ZERO;    // max points of the full exercise (1a + 1b + ...)
            BigDecimal exam_max_points = BigDecimal.ZERO; // max points of the exam (1 + 2 + ...)
            for(Exercise exercise : exercises) {
                String label_number = exercise.getLabel().replaceAll("\\D", "");
                if(!label_number.equals(current_exercise)) { // next exercise
                    current_exercise = label_number;
                    if(exercise_obj != null) {
                        exercise_obj.put("sub_exercises", sub_exercises_obj);
                        exercise_obj.put("sum_points", sum_points);
                        exercise_obj.put("max_points", max_points);
                        exercises_obj.add(exercise_obj);
                    }
                    exercise_obj = new LinkedHashMap<>();
                    exercise_obj.put("label_number", label_number);
                    sub_exercises_obj = new ArrayList<>();
                    sum_points = BigDecimal.ZERO;
                    max_points = BigDecimal.ZERO;
                }
                Map<String, Object> sub_exercise_obj = new LinkedHashMap<>();
                sub_exercise_obj.put("sub_exercise", exercise);
                Answer answer = SAM.db.getAnswer(student, exercise);
                sub_exercise_obj.put("answer", answer);
                sum_points = sum_points.add(answer.getPoints() == null ? BigDecimal.ZERO : answer.getPoints());
                max_points = max_points.add(exercise.getPoints() == null ? BigDecimal.ZERO : exercise.getPoints());
                student_points = student_points.add(answer.getPoints() == null ? BigDecimal.ZERO : answer.getPoints());
                if(!input.containsKey("exam_max_points")) {
                    exam_max_points = exam_max_points.add(exercise.getPoints() == null ? BigDecimal.ZERO : exercise.getPoints());
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

            if(!input.containsKey("exam_max_points")) {
                input.put("exam_max_points", exam_max_points);
            }
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
