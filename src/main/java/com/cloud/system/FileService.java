package com.cloud.system;

import javafx.scene.control.Alert;
import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Random;

public class FileService {
    private LoadBalancer loadBalancer = new LoadBalancer();

    public void uploadFile(File file, String username) {
        new Thread(() -> {
            try {
                // Requirement 10: Artificial Delay Simulation (30 to 90 seconds)
                int delay = new Random().nextInt(61) + 30;
                System.out.println("⏳ Simulating cloud latency: " + delay + " seconds...");
                Thread.sleep(delay * 1000);

                // 1. Read and Chunk (Req 20)
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String fileSizeLabel = (fileBytes.length / 1024) + " KB";
                int mid = fileBytes.length / 2;

                byte[] p1 = Arrays.copyOfRange(fileBytes, 0, mid);
                byte[] p2 = Arrays.copyOfRange(fileBytes, mid, fileBytes.length);

                // 2. AES Encryption (Req 6)
                byte[] enc1 = EncryptionService.encrypt(p1);
                byte[] enc2 = EncryptionService.encrypt(p2);

                // 3. Dynamic Load Balancing (Req 7)
                String node1 = loadBalancer.getNextNode();
                String node2 = loadBalancer.getNextNode();

                int port1 = getPortForNode(node1);
                int port2 = getPortForNode(node2);


                // 4. Physical Upload
                uploadChunkToSftp("localhost", port1, file.getName() + ".part1", enc1);
                uploadChunkToSftp("localhost", port2, file.getName() + ".part2", enc2);

                // 5. Save Stateful Metadata (Req 13)
                saveDistributedMetadata(file.getName(), fileSizeLabel, username, node1, node2);

                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "Upload Successful!\nDistributed to: " + node1 + " & " + node2).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                showError("Upload Failed: " + e.getMessage());
            }
        }).start();
    }
    private int getPortForNode(String nodeName){
        if (nodeName == null ) return 2221;
        switch (nodeName) {
            case "storage_1": return 2221;
            case "storage_2": return 2222;
            case "storage_3": return 2223;
            case "storage_4": return 2224;
            default: return 2221;
        }
    }

    public void downloadAndReassemble(String fileName, File destination) {
        new Thread(() -> {
            try {
                // 1. Fetch metadata to find WHERE the chunks are
                String[] nodes = getChunkLocations(fileName);
                if (nodes == null) throw new Exception("Metadata not found");

                int port1 = nodes[0].equals("storage_1") ? 2221 : 2222;
                int port2 = nodes[1].equals("storage_1") ? 2221 : 2222;

                // 2. Fetch from the specific ports chosen by Load Balancer
                byte[] enc1 = fetchChunkFromServer("localhost", port1, fileName + ".part1");
                byte[] enc2 = fetchChunkFromServer("localhost", port2, fileName + ".part2");

                // 3. Decrypt and Merge
                byte[] part1 = EncryptionService.decrypt(enc1);
                byte[] part2 = EncryptionService.decrypt(enc2);

                try (FileOutputStream fos = new FileOutputStream(destination)) {
                    fos.write(part1);
                    fos.write(part2);
                }

                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "File reassembled and decrypted!").show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                showError("Download Failed: " + e.getMessage());
            }
        }).start();
    }

    private String[] getChunkLocations(String fileName) {
        String sql = "SELECT node_part1, node_part2 FROM file_metadata WHERE file_name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new String[]{rs.getString("node_part1"), rs.getString("node_part2")};
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private void saveDistributedMetadata(String name, String size, String user, String n1, String n2) {
        String sql = "INSERT INTO file_metadata (file_name, file_size, storage_node, user_id, total_chunks, node_part1, node_part2) " +
                "VALUES (?, ?, 'Distributed', (SELECT id FROM users WHERE username = ?), 2, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, size);
            pstmt.setString(3, user);
            pstmt.setString(4, n1);
            pstmt.setString(5, n2);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void uploadChunkToSftp(String host, int port, String chunkName, byte[] data) throws Exception {
        com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
        com.jcraft.jsch.Session session = jsch.getSession("storage_user", host, port);
        session.setPassword("storage_pass");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        com.jcraft.jsch.ChannelSftp sftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        // Fix for "No such file" error: Ensure directory exists
        try {
            sftp.cd("/home/storage_user/uploads");
        } catch (Exception e) {
            sftp.mkdir("/home/storage_user/uploads");
            sftp.cd("/home/storage_user/uploads");
        }

        try (InputStream is = new ByteArrayInputStream(data)) {
            sftp.put(is, chunkName);
        }

        sftp.disconnect();
        session.disconnect();
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
                String[] nodes = getChunkLocations(fileName);

                // Check if metadata exists and nodes are not null
                if (nodes != null && nodes[0] != null && nodes[1] != null) {
                    deletePhysicalChunk("localhost", getPortForNode(nodes[0]), fileName + ".part1");
                    deletePhysicalChunk("localhost", getPortForNode(nodes[1]), fileName + ".part2");
                } else {
                    System.out.println("⚠️ Skipping physical delete: No node location metadata found for " + fileName);
                }

                // Always delete the database record
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM file_metadata WHERE file_name = ?")) {
                    pstmt.setString(1, fileName);
                    pstmt.executeUpdate();
                }

                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "File record removed from system.").show();
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