package com.cloud.system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    public boolean validateLogin(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // In a real app, you would hash this password first!

            ResultSet rs = stmt.executeQuery();

            // If rs.next() is true, it means we found a matching user
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}