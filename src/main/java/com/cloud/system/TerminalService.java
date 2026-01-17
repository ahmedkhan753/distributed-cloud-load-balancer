package com.cloud.system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

public class TerminalService {
    private String currentUser;
    private String currentDir = "/home/cloud_user";

    public TerminalService(String username) {
        this.currentUser = username;
    }

    public String execute(String input) {
        String command = input.trim().toLowerCase();

        switch (command) {
            case "help":
                return "Available: ls, whoami, pwd, date, clear, version, ps";

            case "pwd":
                return currentDir;

            case "whoami":
                return currentUser + " (authenticated)";

            case "date":
                return new Date().toString();

            case "ls":
                return listUserFiles();

            case "version":
                return "Cloud System v2.1 - Distributed Architecture (4 Nodes)";

            case "ps":
                return "PID TTY          TIME CMD\n 101 tty1     00:00:01 java\n 102 tty1     00:00:00 mysql-client";

            default:
                return "Command not found: " + command;
        }
    }

    private String listUserFiles() {
        StringBuilder sb = new StringBuilder("Files in cloud storage:\n");
        String sql = "SELECT file_name, file_size FROM file_metadata";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                sb.append(String.format("- %-20s [%s]\n", rs.getString("file_name"), rs.getString("file_size")));
            }
        } catch (Exception e) {
            return "Error listing files: " + e.getMessage();
        }
        return sb.toString();
    }
}