package com.example.revisedattendanceapp;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {

    private static final String DB_URL = "jdbc:sqlite:attendance.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initDatabase() {
        String createClasses = "CREATE TABLE IF NOT EXISTS classes (class_name TEXT PRIMARY KEY);";
        String createStudents = "CREATE TABLE IF NOT EXISTS students (student_id TEXT PRIMARY KEY, name TEXT NOT NULL);";
        String createAttendance = "CREATE TABLE IF NOT EXISTS attendance (" +
                "student_id TEXT, course TEXT, date TEXT, status TEXT, reason TEXT, " +
                "PRIMARY KEY (student_id, course, date));";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createClasses);
            stmt.execute(createStudents);
            stmt.execute(createAttendance);

            // Add reason column automatically if missing in older database tables
            try {
                stmt.execute("ALTER TABLE attendance ADD COLUMN reason TEXT;");
            } catch (SQLException ignored) {}

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addClass(String className) {
        String sql = "INSERT OR IGNORE INTO classes(class_name) VALUES(?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, className);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Loads all saved classes from the database
    public static List<String> getAllClasses() {
        List<String> classes = new ArrayList<>();

        String sql = "SELECT class_name FROM classes ORDER BY class_name";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                classes.add(rs.getString("class_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return classes;
    }

    public static void insertStudent(String studentId, String studentName) {
        String sql = "INSERT OR REPLACE INTO students(student_id, name) VALUES(?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, studentName);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addStudent(String id, String name) {
        insertStudent(id, name);
    }

    public static void updateStudent(String oldId, String newId, String newName) {
        String sqlUpdateStudent = "UPDATE students SET student_id = ?, name = ? WHERE student_id = ?";
        String sqlUpdateAttendance = "UPDATE attendance SET student_id = ? WHERE student_id = ?";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt1 = conn.prepareStatement(sqlUpdateStudent);
                 PreparedStatement pstmt2 = conn.prepareStatement(sqlUpdateAttendance)) {

                pstmt1.setString(1, newId);
                pstmt1.setString(2, newName);
                pstmt1.setString(3, oldId);
                pstmt1.executeUpdate();

                pstmt2.setString(1, newId);
                pstmt2.setString(2, oldId);
                pstmt2.executeUpdate();

                conn.commit();

            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteStudent(String id) {
        String sql1 = "DELETE FROM students WHERE student_id = ?";
        String sql2 = "DELETE FROM attendance WHERE student_id = ?";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt1 = conn.prepareStatement(sql1);
                 PreparedStatement pstmt2 = conn.prepareStatement(sql2)) {

                pstmt1.setString(1, id);
                pstmt1.executeUpdate();

                pstmt2.setString(1, id);
                pstmt2.executeUpdate();

                conn.commit();

            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<AttendanceRecord> getMonthlyAttendance(String course, YearMonth yearMonth) {
        List<AttendanceRecord> list = new ArrayList<>();

        String startDate = String.format(
                "%04d-%02d-01",
                yearMonth.getYear(),
                yearMonth.getMonthValue()
        );

        String endDate = String.format(
                "%04d-%02d-%02d",
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                yearMonth.lengthOfMonth()
        );

        String studentSql = "SELECT student_id, name FROM students";

        String attendanceSql =
                "SELECT student_id, date, status, reason " +
                        "FROM attendance " +
                        "WHERE course = ? AND date BETWEEN ? AND ?";

        try (Connection conn = connect();
             PreparedStatement studentStmt = conn.prepareStatement(studentSql);
             PreparedStatement attStmt = conn.prepareStatement(attendanceSql)) {

            ResultSet sRs = studentStmt.executeQuery();

            while (sRs.next()) {

                String id = sRs.getString("student_id");
                String name = sRs.getString("name");

                double rate = calculateOverallRate(conn, id, course);

                AttendanceRecord record =
                        new AttendanceRecord(id, name, course, rate);

                attStmt.setString(1, course);
                attStmt.setString(2, startDate);
                attStmt.setString(3, endDate);

                ResultSet aRs = attStmt.executeQuery();

                while (aRs.next()) {

                    if (aRs.getString("student_id").equals(id)) {

                        record.addStatusForDate(
                                aRs.getString("date"),
                                aRs.getString("status"),
                                aRs.getString("reason")
                        );
                    }
                }

                list.add(record);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private static double calculateOverallRate(
            Connection conn,
            String studentId,
            String course) throws SQLException {

        String sql =
                "SELECT COUNT(*) as total, " +
                        "SUM(CASE WHEN status IN ('Present', 'Late') " +
                        "THEN 1 ELSE 0 END) as attended " +
                        "FROM attendance " +
                        "WHERE student_id = ? AND course = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, course);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                int total = rs.getInt("total");
                int attended = rs.getInt("attended");

                return total > 0
                        ? ((double) attended / total) * 100.0
                        : 100.0;
            }
        }

        return 100.0;
    }

    public static void saveAttendanceStatus(
            String studentId,
            String course,
            String date,
            String status,
            String reason) {

        String sql =
                "INSERT INTO attendance" +
                        "(student_id, course, date, status, reason) " +
                        "VALUES(?, ?, ?, ?, ?) " +
                        "ON CONFLICT(student_id, course, date) " +
                        "DO UPDATE SET status = excluded.status, " +
                        "reason = excluded.reason";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, course);
            pstmt.setString(3, date);
            pstmt.setString(4, status);
            pstmt.setString(5, reason);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void markAllPresentForCourseAndDate(
            String course,
            String date) {

        String sql =
                "INSERT INTO attendance" +
                        "(student_id, course, date, status, reason) " +
                        "SELECT student_id, ?, ?, 'Present', NULL " +
                        "FROM students " +
                        "ON CONFLICT(student_id, course, date) " +
                        "DO UPDATE SET status = 'Present', reason = NULL";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, course);
            pstmt.setString(2, date);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}