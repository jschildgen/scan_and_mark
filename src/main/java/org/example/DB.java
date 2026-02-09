package org.example;

import javafx.util.Pair;
import org.example.model.Answer;
import org.example.model.Exercise;
import org.example.model.Student;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class DB {
    private Connection conn;
    public DB(Path dbpath) throws SQLException, IOException {
        if(!Files.exists(dbpath)) {
            Files.createFile(dbpath);
        }
        this.conn = DriverManager.getConnection("jdbc:sqlite:"+dbpath);
        if(!table_exists("students") || !table_exists("exercises") || !table_exists("answers")) {
            createDB();
        };
        if(db_version_older_than(SAM.SAM_VERSION)) {
            updateDB();
        }
    }

    public static Connection remoteConnection(Map<String, String> dbConfigMap) throws SQLException {
        String host = dbConfigMap.get("Host");
        String port = dbConfigMap.get("Port");
        String dbName = dbConfigMap.get("Database Name");
        String user = dbConfigMap.get("Username");
        String password = dbConfigMap.get("Password");
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + dbName;
        Connection connection = DriverManager.getConnection(url, user, password);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
        return connection;
    }

    private void updateDB() {
        System.out.println("Updating database");

        try {
            if (db_version_older_than("0.1.1")) {
                System.out.println("Updating to 0.1.1: Create Table sam, add pdfpage/prcnt columns to students table");

                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE TABLE sam(k VARCHAR(255), v VARCHAR(255))");
                    stmt.executeUpdate("INSERT INTO sam(k,v) VALUES('db_version', '0.1.1')");
                    stmt.executeUpdate("ALTER TABLE students ADD COLUMN pdfpage int");
                    stmt.executeUpdate("ALTER TABLE students ADD COLUMN prcnt int");
                }
            }

            if (db_version_older_than("0.1.2")) {
                System.out.println("Updating to 0.1.2: Add qrcode column to students table");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE students ADD COLUMN qrcode varchar(255)");
                }
            }

            set_sam_config("db_version", SAM.SAM_VERSION);
            System.out.println("Updated to " + SAM.SAM_VERSION);

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }



    private String get_sam_config(String k) throws SQLException {
        if (!table_exists("sam")) {
            return null;
        }

        String sql = "SELECT v FROM sam WHERE k = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, k);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }

        return null;
    }

    private void set_sam_config(String k, String v) throws SQLException {
        String sql;
        if (get_sam_config(k) == null) {
            sql = "INSERT INTO sam(v,k) VALUES(?,?)";
        } else {
            sql = "UPDATE sam SET v = ? WHERE k = ?";
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, v);
            pstmt.setString(2, k);
            pstmt.executeUpdate();
        }
    }

    private boolean db_version_older_than(String version) throws SQLException {
        String db_version = get_sam_config("db_version");
        if(db_version == null) {
            return true;
        }
        String[] db_version_parts = db_version.split("\\.");
        String[] sam_version_parts = version.split("\\.");

        int length = Math.max(db_version_parts.length, sam_version_parts.length);

        for (int i = 0; i < length; i++) {
            int dbv = (i < db_version_parts.length) ? Integer.parseInt(db_version_parts[i]) : 0;
            int samv = (i < sam_version_parts.length) ? Integer.parseInt(sam_version_parts[i]) : 0;

            if (dbv < samv) {
                return true;
            } else if (dbv > samv) {
                return false;
            }
        }
        return false;
    }


    private boolean table_exists(String tablename) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables("%", "%", tablename, null)) {
            return rs.next(); // Gibt true zurück, wenn die Tabelle existiert
        }
    }

    public void persist(Student student) throws SQLException {
        if (student.getId() == null) {
            student.setId(next_sid());
        }

        String sql = """
        INSERT INTO students (sid, matno, name1, name2, pdfpage, prcnt, qrcode)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(sid) DO UPDATE SET
            matno = ?, name1 = ?, name2 = ?, pdfpage = ?, prcnt = ?, qrcode = ?
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, student.getId());
            pstmt.setString(2, student.getMatno());
            pstmt.setString(3, student.getName1());
            pstmt.setString(4, student.getName2());
            pstmt.setInt(5, student.getPdfpage());
            pstmt.setInt(6, student.getPrcnt());
            pstmt.setString(7, student.getQrcode());

            pstmt.setString(2+6, student.getMatno());
            pstmt.setString(3+6, student.getName1());
            pstmt.setString(4+6, student.getName2());
            pstmt.setInt(5+6, student.getPdfpage());
            pstmt.setInt(6+6, student.getPrcnt());
            pstmt.setString(7+6, student.getQrcode());

            pstmt.executeUpdate();
        }
    }


    public void persist(Exercise exercise) throws SQLException {
        if (exercise.getId() == null) {
            exercise.setId(next_eid());
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(
                    "INSERT INTO exercises (eid, label, page, points, pos1x, pos1y, pos2x, pos2y)" +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?)" +
                            " ON CONFLICT(eid) DO UPDATE" +
                            " SET label = ?, page = ?, points = ?, pos1x = ?, pos1y = ?, pos2x = ?, pos2y = ?"
            );

            // Setze die Parameter für das INSERT
            pstmt.setInt(1, exercise.getId());
            pstmt.setString(2, exercise.getLabel());
            pstmt.setString(3, exercise.getPageNo());
            pstmt.setBigDecimal(4, exercise.getPoints());
            pstmt.setDouble(5, exercise.getPos()[0][0]);
            pstmt.setDouble(6, exercise.getPos()[0][1]);
            pstmt.setDouble(7, exercise.getPos()[1][0]);
            pstmt.setDouble(8, exercise.getPos()[1][1]);

            // Setze die Parameter für das UPDATE
            pstmt.setString(9, exercise.getLabel());
            pstmt.setString(10, exercise.getPageNo());
            pstmt.setBigDecimal(11, exercise.getPoints());
            pstmt.setDouble(12, exercise.getPos()[0][0]);
            pstmt.setDouble(13, exercise.getPos()[0][1]);
            pstmt.setDouble(14, exercise.getPos()[1][0]);
            pstmt.setDouble(15, exercise.getPos()[1][1]);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public void persist(Answer answer) throws SQLException {
        if (answer.getStudent().getId() == null || answer.getExercise().getId() == null) {
            return;
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(
                    "INSERT INTO answers (student, exercise, points, feedback) VALUES (?, ?, ?, ?)" +
                            " ON CONFLICT(student, exercise) DO UPDATE" +
                            " SET points = ?, feedback = ?"
            );

            // Setze die Parameter für das INSERT
            pstmt.setInt(1, answer.getStudent().getId());
            pstmt.setInt(2, answer.getExercise().getId());
            pstmt.setBigDecimal(3, answer.getPoints());
            pstmt.setString(4, answer.getFeedback());

            // Setze die Parameter für das UPDATE
            pstmt.setBigDecimal(5, answer.getPoints());
            pstmt.setString(6, answer.getFeedback());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;  // Fehler weiterwerfen, wenn nötig
        } finally {
            if (pstmt != null) {
                pstmt.close();  // Sicherstellen, dass das PreparedStatement immer geschlossen wird
            }
        }
    }


    public List<Student> getStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String query = "SELECT * FROM students ORDER BY matno";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Student student = new Student(rs.getInt("sid"), rs.getString("matno"));
                student.setName1(rs.getString("name1"));
                student.setName2(rs.getString("name2"));
                student.setPdfpage(rs.getInt("pdfpage"));
                student.setPrcnt(rs.getInt("prcnt"));
                student.setQrcode(rs.getString("qrcode"));
                students.add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return students;
    }

    public Student getStudent(int sid) throws SQLException {
        String query = "SELECT * FROM students WHERE sid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, sid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Student student = new Student(rs.getInt("sid"), rs.getString("matno"));
                    student.setName1(rs.getString("name1"));
                    student.setName2(rs.getString("name2"));
                    student.setPdfpage(rs.getInt("pdfpage"));
                    student.setPrcnt(rs.getInt("prcnt"));
                    student.setQrcode(rs.getString("qrcode"));
                    return student;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Fehler weiterwerfen
        }
        return null;
    }

    public Student getStudentByMatno(String matno) throws SQLException {
        String query = "SELECT * FROM students WHERE matno = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, matno);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Student student = new Student(rs.getInt("sid"), rs.getString("matno"));
                    student.setName1(rs.getString("name1"));
                    student.setName2(rs.getString("name2"));
                    student.setPdfpage(rs.getInt("pdfpage"));
                    student.setPrcnt(rs.getInt("prcnt"));
                    student.setQrcode(rs.getString("qrcode"));
                    return student;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return null;
    }

    public Exercise getExercise(int eid) throws SQLException {
        String query = "SELECT * FROM exercises WHERE eid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, eid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double[][] pos = {
                            { rs.getDouble("pos1x"), rs.getDouble("pos1y") },
                            { rs.getDouble("pos2x"), rs.getDouble("pos2y") }
                    };
                    Exercise exercise = new Exercise(rs.getString("label"), rs.getString("page"), pos);
                    exercise.setId(rs.getInt("eid"));
                    exercise.setPoints(rs.getBigDecimal("points"));
                    return exercise;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return null;
    }

    public List<Exercise> getExercises() throws SQLException {
        List<Exercise> exercises = new ArrayList<>();
        String query = "SELECT * FROM exercises";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                double[][] pos = {
                        { rs.getDouble("pos1x"), rs.getDouble("pos1y") },
                        { rs.getDouble("pos2x"), rs.getDouble("pos2y") }
                };
                Exercise exercise = new Exercise(rs.getString("label"), rs.getString("page"), pos);
                exercise.setId(rs.getInt("eid"));
                exercise.setPoints(rs.getBigDecimal("points"));
                exercises.add(exercise);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

        exercises.sort(Comparator.naturalOrder());

        return exercises;
    }

    private Integer next_sid() throws SQLException {
        String query = "SELECT MAX(sid)+1 FROM students";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                Integer nextSid = rs.getInt(1);
                if (nextSid != null && nextSid > 0) {
                    return nextSid;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return 1;
    }


    private Integer next_eid() throws SQLException {
        String query = "SELECT MAX(eid)+1 FROM exercises";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                Integer nextEid = rs.getInt(1);
                if (nextEid != null && nextEid > 0) {
                    return nextEid;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return 1;
    }

    public void createDB() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS sam");
            stmt.executeUpdate("DROP TABLE IF EXISTS answers");
            stmt.executeUpdate("DROP TABLE IF EXISTS exercises");
            stmt.executeUpdate("DROP TABLE IF EXISTS students");

            stmt.executeUpdate("CREATE TABLE sam(k VARCHAR(255), v VARCHAR(255))");
            stmt.executeUpdate("CREATE TABLE students (sid int primary key, matno varchar(255), name1 varchar(255), name2 varchar(255), pdfpage int, prcnt int, qrcode varchar(255))");
            stmt.executeUpdate("CREATE TABLE exercises (eid int primary key, label varchar(255), page varchar(255), points decimal(18,2), pos1x double, pos1y double, pos2x double, pos2y double)");
            stmt.executeUpdate("CREATE TABLE answers (student int references students(sid), exercise int references exercises(eid), points decimal(18,2), feedback varchar(2000000), PRIMARY KEY(student, exercise))");
        }

        try (PreparedStatement pstmt_sam_insert = conn.prepareStatement("INSERT INTO sam(k,v) VALUES(?,?)")) {
            pstmt_sam_insert.setString(1, "db_version");
            pstmt_sam_insert.setString(2, SAM.SAM_VERSION);
            pstmt_sam_insert.executeUpdate();
        }
    }

    public void setSAM(String k, String v) throws SQLException {
        String select = "SELECT COUNT(*) FROM sam WHERE k = ?";
        try (PreparedStatement pstmtSelect = conn.prepareStatement(select)) {
            pstmtSelect.setString(1, k);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    String update = "UPDATE sam SET v = ? WHERE k = ?";
                    try (PreparedStatement pstmtUpdate = conn.prepareStatement(update)) {
                        pstmtUpdate.setString(1, v);
                        pstmtUpdate.setString(2, k);
                        pstmtUpdate.executeUpdate();
                    }
                } else {
                    String insert = "INSERT INTO sam(k, v) VALUES(?, ?)";
                    try (PreparedStatement pstmtInsert = conn.prepareStatement(insert)) {
                        pstmtInsert.setString(1, k);
                        pstmtInsert.setString(2, v);
                        pstmtInsert.executeUpdate();
                    }
                }
            }
        }
    }

    public void delete(Exercise exercise) throws SQLException {
        if(exercise.getId() == null) { return; }

        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM exercises WHERE eid = ?")) {
            pstmt.setInt(1, exercise.getId());
            pstmt.executeUpdate();
        }
    }

    public Answer getAnswer(Student student, Exercise exercise) throws SQLException {
        if(student.getId() == null || exercise.getId() == null) { return null; }

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT points, feedback FROM answers WHERE student = ? AND exercise = ?")) {
            pstmt.setInt(1, student.getId());
            pstmt.setInt(2, exercise.getId());
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                Answer answer = new Answer(student, exercise);
                answer.setPoints(rs.getBigDecimal("points"));
                answer.setFeedback(rs.getString("feedback"));
                return answer;
            }
        }

        return new Answer(student, exercise);
    }


    public Map<String, BigDecimal> getFeedback(Exercise exercise) throws SQLException {
        Map<String, BigDecimal> feedback = new LinkedHashMap<>();
        if(exercise.getId() == null) { return feedback; }

        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT feedback, MAX(points) FROM answers WHERE exercise = ? AND feedback IS NOT NULL " +
                        "GROUP BY feedback ORDER BY count(*) DESC")) {
            pstmt.setInt(1, exercise.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    feedback.put(rs.getString(1), rs.getBigDecimal(2));
                }
            }
        }

        return feedback;
    }



    public int num_marked_answers() throws SQLException {
        return num_marked_answers(null);
    }

    public int num_marked_answers(Exercise exercise) throws SQLException {
        String query;
        if (exercise == null) {
            query = "SELECT count(*) FROM answers WHERE points IS NOT NULL";
        } else {
            query = "SELECT count(*) FROM answers WHERE points IS NOT NULL AND exercise = ?";
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            if (exercise != null) {
                pstmt.setInt(1, exercise.getId());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }


    public Map<Student, BigDecimal> getStudentsWithPoints() throws SQLException {
        Map<Student, BigDecimal> students = new LinkedHashMap<>();
        String query = "SELECT S.sid, S.matno, S.name1, S.name2, S.pdfpage, S.prcnt, SUM(A.points) as points " +
                "FROM students S JOIN answers A ON S.sid = A.student " +
                "GROUP BY S.sid ORDER BY S.matno";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Student student = new Student(rs.getInt("sid"), rs.getString("matno"));
                student.setName1(rs.getString("name1"));
                student.setName2(rs.getString("name2"));
                student.setPdfpage(rs.getInt("pdfpage"));
                student.setPrcnt(rs.getInt("prcnt"));

                BigDecimal points = rs.getBigDecimal("points");
                if (points != null) {
                    students.put(student, points);
                } else {
                    students.put(student, BigDecimal.ZERO); // Falls keine Punkte vorhanden sind
                }
            }
        }

        return students;
    }

    public void delete(Student student) throws SQLException {
        if (student.getId() == null) {
            return;
        }

        String query = "DELETE FROM students WHERE sid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, student.getId());
            pstmt.executeUpdate();
        }
    }


}
