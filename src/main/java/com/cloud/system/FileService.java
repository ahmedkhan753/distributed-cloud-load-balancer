package com.cloud.system;

import javafx.scene.control.Alert;
import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;

public class FileService {
    private LoadBalancer loadBalancer = new LoadBalancer();

    public void uploadFile(File file, String username) {
        new Thread(() -> {
            try {
                // 1. Read file and prepare for chunking (Req 20)
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String fileSizeLabel = (fileBytes.length / 1024) + " KB";
                int mid = fileBytes.length / 2;

                // Split into two parts
                byte[] part1 = Arrays.copyOfRange(fileBytes, 0, mid);
                byte[] part2 = Arrays.copyOfRange(fileBytes, mid, fileBytes.length);

                // 2. AES Encryption (Req 6)
                byte[] encrypted1 = EncryptionService.encrypt(part1);
                byte[] encrypted2 = EncryptionService.encrypt(part2);

                // 3. Distributed Upload (Req 7)
                // Upload physical chunks to nodes
                uploadChunkToSftp("localhost", 2221, file.getName() + ".part1", encrypted1);
                uploadChunkToSftp("localhost", 2222, file.getName() + ".part2", encrypted2);

                // 4. Save metadata ONCE for the whole file (Req 13)
                // We record that this file has 2 chunks distributed across the system
                saveFileMetadata(file.getName(), fileSizeLabel, username, 2);

                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "File distributed successfully!").show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                showError("Upload Failed: " + e.getMessage());
            }
        }).start();
    }

    private void uploadChunkToSftp(String host, int port, String chunkName, byte[] data) throws Exception {
        com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
        com.jcraft.jsch.Session session = jsch.getSession("storage_user", host, port);
        session.setPassword("storage_pass");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        com.jcraft.jsch.ChannelSftp sftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        try (InputStream is = new ByteArrayInputStream(data)) {
            sftp.put(is, "/home/storage_user/uploads/" + chunkName);
        }

        sftp.disconnect();
        session.disconnect();
    }

    private void saveFileMetadata(String name, String size, String user, int totalChunks) {
        // Updated SQL: We store the main file record.
        // Note: storage_node column now represents the 'Cluster' or primary node
        String sql = "INSERT INTO file_metadata (file_name, file_size, storage_node, user_id, total_chunks) " +
                "VALUES (?, ?, 'Distributed', (SELECT id FROM users WHERE username = ?), ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, size);
            pstmt.setString(3, user);
            pstmt.setInt(4, totalChunks);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void downloadAndReassemble(String fileName, File destination) {
        new Thread(() -> {
            try {
                // 1. Fetch encrypted chunks from nodes
                byte[] enc1 = fetchChunkFromServer("localhost", 2221, fileName + ".part1");
                byte[] enc2 = fetchChunkFromServer("localhost", 2222, fileName + ".part2");

                // 2. Decrypt chunks (Req 6)
                byte[] part1 = EncryptionService.decrypt(enc1);
                byte[] part2 = EncryptionService.decrypt(enc2);

                // 3. Reassemble (Req 20)
                try (FileOutputStream fos = new FileOutputStream(destination)) {
                    fos.write(part1);
                    fos.write(part2);
                }

                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "File reassembled successfully!").show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                showError("Download Failed: Check if both storage nodes are online.");
            }
        }).start();
    }

    private byte[] fetchChunkFromServer(String host, int port, String chunkName) throws Exception {
        com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
        com.jcraft.jsch.Session session = jsch.getSession("storage_user", host, port);
        session.setPassword("storage_pass");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        com.jcraft.jsch.ChannelSftp sftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        byte[] data;
        try (InputStream is = sftp.get("/home/storage_user/uploads/" + chunkName)) {
            data = is.readAllBytes();
        }

        sftp.disconnect();
        session.disconnect();
        return data;
    }

    public void deleteDistributedFile(String fileName) {
        new Thread(() -> {
            try {
                // Delete from both servers
                deletePhysicalChunk("localhost", 2221, fileName + ".part1");
                deletePhysicalChunk("localhost", 2222, fileName + ".part2");

                // Delete single metadata record
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM file_metadata WHERE file_name = ?")) {
                    pstmt.setString(1, fileName);
                    pstmt.executeUpdate();
                }

                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "File deleted from all nodes.").show();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void deletePhysicalChunk(String host, int port, String chunkName) throws Exception {
        com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
        com.jcraft.jsch.Session session = jsch.getSession("storage_user", host, port);
        session.setPassword("storage_pass");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        com.jcraft.jsch.ChannelSftp sftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        try { sftp.rm("/home/storage_user/uploads/" + chunkName); } catch (Exception ignored) {}
        sftp.disconnect();
        session.disconnect();
    }

    private void showError(String msg) {
        javafx.application.Platform.runLater(() -> {
            new Alert(Alert.AlertType.ERROR, msg).show();
        });
    }
}