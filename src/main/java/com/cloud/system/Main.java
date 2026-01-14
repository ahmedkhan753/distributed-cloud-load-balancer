package com.cloud.system;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Cloud System Initialization Test ---");

        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("✅ Successfully connected to Docker MySQL database!");

            // Run a test query to list the tables
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES;");

            System.out.println("Tables found in database:");
            while (rs.next()) {
                System.out.println("- " + rs.getString(1));
            }

        } catch (Exception e) {
            System.err.println("❌ Connection failed!");
            e.printStackTrace();
        }
    }
}