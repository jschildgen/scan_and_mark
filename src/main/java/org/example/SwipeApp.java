package org.example;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import javafx.scene.image.Image;
import org.example.model.Answer;
import org.example.model.Exercise;
import org.example.model.Student;
import spark.ModelAndView;
import spark.Spark;
import spark.template.freemarker.FreeMarkerEngine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;
import javafx.embed.swing.SwingFXUtils;


public class SwipeApp {
    public static <WriteablePixelFromat> void startServer() {
        try {
            Spark.staticFiles.externalLocation(Paths.get(SwipeApp.class.getResource("swipeapp/static").toURI()).toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        /* Freemarker configuration */
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);

        try {
            cfg.setDirectoryForTemplateLoading(new File(Paths.get(SwipeApp.class.getResource("swipeapp/swipeapp.ftl").toURI()).getParent().toUri()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.US);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        Spark.get("/", (req, res) -> {
            Map<String, Object> attributes = new HashMap<>();

            Exercise exercise = SAM.db.getExercises().get(0);

            attributes.put("exercise", exercise);

            List<Object> students_obj = new ArrayList<>();

            for(Student student : SAM.db.getStudents()) {
                Answer answer = SAM.db.getAnswer(student, exercise);

                Map<String, Object> student_obj = new LinkedHashMap<>();
                student_obj.put("student", student);
                student_obj.put("answer", answer);

                students_obj.add(student_obj);
            }
            attributes.put("students", students_obj);

            return new ModelAndView(attributes, "swipeapp.ftl");
        }, new FreeMarkerEngine(cfg));

        Spark.get("/answer_img/:student/:exercise", (req, res) -> {
            res.type("image/jpg");

            Student student;
            Exercise exercise;

            try {
                student = SAM.db.getStudent(Integer.parseInt(req.params(":student")));
                exercise = SAM.db.getExercise(Integer.parseInt(req.params(":exercise")));
            } catch (NumberFormatException e) {
                return null;
            }
            if(student == null || exercise == null) {
                return null;
            }

            Answer answer = SAM.db.getAnswer(student, exercise);
            Image image = student.getAnswerImage(exercise);

            BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            ImageIO.write(bImage, "png", s);
            byte[] res_img  = s.toByteArray();
            s.close();

            return res_img;
        });

        System.out.printf("Swipe App: http://%s:%d\n", SwipeApp.getIpAddress(), Spark.port());
    }




    public static void stopServer() {
        Spark.stop();
    }

    public static String getIpAddress() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                    if(ip.length() > 15) { continue; } // filter out IP V6
                    return ip;
                }
            }
        } catch (SocketException e) { }
        return null;
    }
}
