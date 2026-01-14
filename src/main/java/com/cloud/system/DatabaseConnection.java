package com.cloud.system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Port is 3307, mapped to the container's 3306
    private static final String URL = "jdbc:mysql://localhost:3307/cloud_system";
    private static final String USER = "root";
    private static final String PASSWORD = "admin_password";

    public static Connection getConnection() throws SQLException {
        try {
            // Load the MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Driver not found. Check pom.xml dependencies.", e);
        }
    }
}