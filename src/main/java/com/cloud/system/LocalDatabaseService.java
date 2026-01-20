package com.cloud.system;

import java.sql.*;

public class LocalDatabaseService {
    private static final String URL = "jdbc:sqlite:local_session.db";

    static {
        try (Connection conn = DriverManager.getConnection(URL)) {
            Statement stmt = conn.createStatement();
            // Table for session data
            stmt.execute("CREATE TABLE IF NOT EXISTS session (id INTEGER PRIMARY KEY, username TEXT, login_time TEXT)");
            // Table for temporary metadata/logs
            stmt.execute("CREATE TABLE IF NOT EXISTS activity_log (id INTEGER PRIMARY KEY, action TEXT, file_name TEXT, timestamp TEXT)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void saveSession(String username) {
        try (Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO session (username, login_time) VALUES (?, datetime('now'))");
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void logActivity(String action, String fileName) {
        try (Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO activity_log (action, file_name, timestamp) VALUES (?, ?, datetime('now'))");
            pstmt.setString(1, action);
            pstmt.setString(2, fileName);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}