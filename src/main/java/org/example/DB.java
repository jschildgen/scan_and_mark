package org.example;

import org.example.model.Answer;
import org.example.model.Exercise;
import org.example.model.Student;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class DB {
    private Connection conn;
    public DB(Path dbpath) throws SQLException, IOException {
        if(!Files.exists(dbpath)) {
            Files.createFile(dbpath);
        }
        this.conn = DriverManager.getConnection("jdbc:sqlite:"+dbpath);
        if(!table_exists("students") || !table_exists("exercises") || !table_exists("answers")) {
            createDB();
        }
    }


    private boolean table_exists(String tablename) throws SQLException {
        ResultSet rs = conn.getMetaData().getTables("%","%",tablename,null);
        if(rs.next()) {
            return true;
        }
        return false;
    }

    public void persist(Student student) throws SQLException {
        if(student.getId() == null) {
            student.setId(next_sid());
        }

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO students (sid, matno, name1, name2) VALUES (?, ?, ?, ?)" +
                        " ON CONFLICT(sid) DO UPDATE SET matno = ?, name1 = ?, name2 = ?");
        pstmt.setInt(1, student.getId());
        pstmt.setString(2, student.getMatno());
        pstmt.setString(3, student.getName1());
        pstmt.setString(4, student.getName2());
        pstmt.setString(2+3, student.getMatno());
        pstmt.setString(3+3, student.getName1());
        pstmt.setString(4+3, student.getName2());
        pstmt.executeUpdate();
    }

    public void persist(Exercise exercise) throws SQLException {
        if(exercise.getId() == null) {
            exercise.setId(next_eid());
        }
        PreparedStatement pstmt = conn.prepareStatement(
            "INSERT INTO exercises (eid, label, page, points, pos1x, pos1y, pos2x, pos2y)" +
                              " VALUES (?,   ?,    ?,      ?,     ?,     ?,     ?,     ?)" +
                    " ON CONFLICT(eid) DO UPDATE" +
                    " SET label = ?, page = ?, points = ?, pos1x = ?, pos1y = ?, pos2x = ?, pos2y = ?");
        pstmt.setInt(1, exercise.getId());
        pstmt.setString(2, exercise.getLabel());
        pstmt.setString(3, exercise.getPageNo());
        pstmt.setBigDecimal(4, exercise.getPoints());
        pstmt.setDouble(5, exercise.getPos()[0][0]);
        pstmt.setDouble(6, exercise.getPos()[0][1]);
        pstmt.setDouble(7, exercise.getPos()[1][0]);
        pstmt.setDouble(8, exercise.getPos()[1][1]);
        pstmt.setString(2+7, exercise.getLabel());
        pstmt.setString(3+7, exercise.getPageNo());
        pstmt.setBigDecimal(4+7, exercise.getPoints());
        pstmt.setDouble(5+7, exercise.getPos()[0][0]);
        pstmt.setDouble(6+7, exercise.getPos()[0][1]);
        pstmt.setDouble(7+7, exercise.getPos()[1][0]);
        pstmt.setDouble(8+7, exercise.getPos()[1][1]);
        pstmt.executeUpdate();
    }

    public void persist(Answer answer) throws SQLException {
        if(answer.getStudent().getId() == null || answer.getExercise().getId() == null) {
            return;
        }
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO answers" +
                " (student, exercise, points, feedback) VALUES (?, ?, ?, ?)" +
                " ON CONFLICT(student, exercise) DO UPDATE" +
                " SET points = ?, feedback = ?");
        pstmt.setInt(1, answer.getStudent().getId());
        pstmt.setInt(2, answer.getExercise().getId());
        pstmt.setBigDecimal(3, answer.getPoints());
        pstmt.setString(4, answer.getFeedback());
        pstmt.setBigDecimal(3+2, answer.getPoints());
        pstmt.setString(4+2, answer.getFeedback());
        pstmt.executeUpdate();
    }

    public List<Student> getStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM students ORDER BY matno");
        while(rs.next()) {
            Student student = new Student(rs.getInt("sid"), rs.getString("matno"));
            student.setName1(rs.getString("name1"));
            student.setName2(rs.getString("name2"));
            students.add(student);
        }
        return students;
    }

    public List<Exercise> getExercises() throws SQLException {
        List<Exercise> exercises = new ArrayList<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM exercises ORDER BY label");
        while(rs.next()) {
            double[][] pos = { { rs.getDouble("pos1x"), rs.getDouble("pos1y") }, { rs.getDouble("pos2x"), rs.getDouble("pos2y") }};
            Exercise exercise = new Exercise(rs.getString("label"), rs.getString("page"), pos);
            exercise.setId(rs.getInt("eid"));
            exercise.setPoints(rs.getBigDecimal("points"));
            exercises.add(exercise);
        }
        return exercises;
    }

    private Integer next_sid() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT MAX(sid)+1 FROM students");
        if(rs.next() && rs.getInt(1) > 0) {
            return rs.getInt(1);
        }
        return 1;
    }

    private Integer next_eid() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT MAX(eid)+1 FROM exercises");
        if(rs.next() && rs.getInt(1) > 0) {
            return rs.getInt(1);
        }
        return 1;
    }

    public void createDB() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DROP TABLE IF EXISTS answers");
        stmt.executeUpdate("DROP TABLE IF EXISTS exercises");
        stmt.executeUpdate("DROP TABLE IF EXISTS students");
        stmt.executeUpdate("CREATE TABLE students (sid int primary key, matno varchar(255), name1 varchar(255), name2 varchar(255))");
        stmt.executeUpdate("CREATE TABLE exercises (eid int primary key, label varchar(255), page varchar(255), points decimal(18,2), pos1x double, pos1y double, pos2x double, pos2y double)");
        stmt.executeUpdate("CREATE TABLE answers (student int references students(sid), exercise int references exercises(eid), points decimal(18,2), feedback varchar(2000000), PRIMARY KEY(student, exercise))");
    }

    public void delete(Exercise exercise) throws SQLException {
        if(exercise.getId() == null) { return; }
        PreparedStatement pstmt = conn.prepareStatement("DELETE FROM exercises WHERE eid = ?");
        pstmt.setInt(1, exercise.getId());
        pstmt.executeUpdate();
    }

    public Answer getAnswer(Student student, Exercise exercise) throws SQLException {
        if(student.getId() == null || exercise.getId() == null) { return null; }

        PreparedStatement pstmt = conn.prepareStatement("SELECT points, feedback FROM answers WHERE student = ? AND exercise = ?");
        pstmt.setInt(1, student.getId());
        pstmt.setInt(2, exercise.getId());
        ResultSet rs = pstmt.executeQuery();
        if(rs.next()) {
            Answer answer = new Answer(student, exercise);
            answer.setPoints(rs.getBigDecimal("points"));
            answer.setFeedback(rs.getString("feedback"));
            return answer;
        }

        return new Answer(student, exercise);
    }

    public Map<String, BigDecimal> getFeedback(Exercise exercise) throws SQLException {
        Map<String, BigDecimal> feedback = new LinkedHashMap<>();
        if(exercise.getId() == null) { return feedback; }

        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT feedback, MAX(points) FROM answers WHERE exercise = ? AND feedback IS NOT NUll" +
                        " GROUP BY feedback ORDER BY count(*) DESC");
        pstmt.setInt(1, exercise.getId());
        ResultSet rs = pstmt.executeQuery();
        while(rs.next()) {
            feedback.put(rs.getString(1), rs.getBigDecimal(2));
        }
        return feedback;
    }


}
