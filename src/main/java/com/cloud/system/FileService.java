package com.cloud.system;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class FileService {
    private LoadBalancer loadBalancer = new LoadBalancer();

    public void uploadFile(File file, String username) {
        // 1. Get the next server from Load Balancer
        String targetNode = loadBalancer.getNextNode();

        // 2. Determine the SFTP Port based on the target node
        // In our docker-compose: storage_1 = 2221, storage_2 = 2222
        int port = targetNode.equals("storage_1") ? 2221 : 2222;

        // 3. Start SFTP Transfer in a background thread
        // This prevents the UI from "freezing" during the upload
        new Thread(() -> {
            System.out.println("🚀 Starting SFTP transfer for: " + file.getName());
            SftpUploader.upload("localhost", port, "storage_user", "storage_pass", file);
        }).start();

        // 4. Format file size for the UI
        String fileSize = (file.length() / 1024) + " KB";

        // 5. Save the "Metadata" to our MySQL database
        saveMetadataToDb(file.getName(), fileSize, targetNode, username);

        System.out.println("✅ Metadata saved: " + file.getName() + " assigned to " + targetNode);
    }

    private void saveMetadataToDb(String name, String size, String node, String user) {
        String sql = "INSERT INTO file_metadata (file_name, file_size, storage_node, user_id) " +
                "VALUES (?, ?, ?, (SELECT id FROM users WHERE username = ?))";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, size);
            pstmt.setString(3, node);
            pstmt.setString(4, user);
            pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}