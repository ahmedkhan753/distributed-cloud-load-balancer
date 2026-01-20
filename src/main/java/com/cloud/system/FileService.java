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
    private LoadBalancer loadBalancer;

    // Add this constructor
    public FileService(LoadBalancer lb) {
        this.loadBalancer = lb;
    }

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

                String targetNode1 = loadBalancer.getNextNode();
                String targetNode2 = loadBalancer.getNextNode();

                if (targetNode1 == null || targetNode2 == null) {
                    showError("Critical Error: Not enough healthy storage nodes available!");
                    return;
                }

                int port1 = getPortForNode(targetNode1);
                int port2 = getPortForNode(targetNode2);


                // 4. Physical Upload
                uploadChunkToSftp("localhost", port1, file.getName() + ".part1", enc1);
                uploadChunkToSftp("localhost", port2, file.getName() + ".part2", enc2);

                // 5. Save Stateful Metadata (Req 13)
                saveDistributedMetadata(file.getName(), fileSizeLabel, username, targetNode1, targetNode2);
                LocalDatabaseService.logActivity("UPLOAD", file.getName());

                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "Upload Successful!\nDistributed to: " + targetNode1 + " & " + targetNode2).show();
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
                // 1. Ask the Database: "Where did the Load Balancer put these chunks?"
                String[] locations = getChunkLocations(fileName);

                if (locations == null || locations[0] == null || locations[1] == null) {
                    showError("Error: Could not find location metadata for this file.");
                    return;
                }

                String node1 = locations[0];
                String node2 = locations[1];

                // 2. Map those names to the correct ports
                int port1 = getPortForNode(node1);
                int port2 = getPortForNode(node2);

                System.out.println("📥 Downloading from healthy nodes: " + node1 + " (Port " + port1 + ") and " + node2 + " (Port " + port2 + ")");

                // 3. Fetch from those SPECIFIC ports
                byte[] enc1 = fetchChunkFromServer("localhost", port1, fileName + ".part1");
                byte[] enc2 = fetchChunkFromServer("localhost", port2, fileName + ".part2");

                // 4. Decrypt and Reassemble
                byte[] part1 = EncryptionService.decrypt(enc1);
                byte[] part2 = EncryptionService.decrypt(enc2);

                try (FileOutputStream fos = new FileOutputStream(destination)) {
                    fos.write(part1);
                    fos.write(part2);
                }

                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "File reassembled from " + node1 + " and " + node2).show();
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

        String remotePath = "uploads/" + chunkName;

        try (InputStream is = new ByteArrayInputStream(data)) {
            sftp.put(is, remotePath);
            System.out.println("✅ Chunk " + chunkName + " uploaded to port " + port);
        } catch (Exception e) {
            System.err.println("❌ SFTP Put Failed on port " + port + ": " + e.getMessage());
            throw e;
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

        byte[] data = null;
        // We try two different paths because SFTP configurations vary
        String[] possiblePaths = {
                "uploads/" + chunkName,               // Relative path (Used during our upload)
                "/home/storage_user/uploads/" + chunkName // Absolute path
        };

        Exception lastError = null;
        for (String path : possiblePaths) {
            try {
                System.out.println("🔍 Searching for chunk at: " + host + ":" + port + " -> " + path);
                try (InputStream is = sftp.get(path)) {
                    data = is.readAllBytes();
                    System.out.println("✅ Found and downloaded: " + path);
                    break; // If successful, exit the loop
                }
            } catch (Exception e) {
                lastError = e; // Keep track of the error and try the next path
            }
        }

        sftp.disconnect();
        session.disconnect();

        if (data == null) {
            throw new Exception("File not found on server after trying all paths. Last error: " + lastError.getMessage());
        }
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