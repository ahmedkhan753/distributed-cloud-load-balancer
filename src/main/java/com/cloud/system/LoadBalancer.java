package com.cloud.system;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {
    private List<String> storageNodes;
    private int currentIndex = 0;

    public LoadBalancer() {
        storageNodes = new ArrayList<>();
        storageNodes.add("storage_1");
        storageNodes.add("storage_2");
        storageNodes.add("storage_3");
        storageNodes.add("storage_4");
    }

    public synchronized String getNextHealthyNode() {
        int attempts = 0;
        while (attempts < storageNodes.size()) {
            String node = storageNodes.get(currentIndex);
            int port = getPortForNode(node);

            // Move pointer for next time (Round Robin)
            currentIndex = (currentIndex + 1) % storageNodes.size();

            if (isNodeAlive("localhost", port)) {
                System.out.println("✅ Node " + node + " is healthy. Assigning task.");
                return node;
            } else {
                System.out.println("⚠️ Node " + node + " is DOWN. Skipping...");
                attempts++;
            }
        }
        return null; // All nodes are down
    }

    private boolean isNodeAlive(String host, int port) {
        try (Socket socket = new Socket()) {
            // Try to connect to the SFTP port with a 1-second timeout
            socket.connect(new java.net.InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int getPortForNode(String nodeName) {
        switch (nodeName) {
            case "storage_1": return 2221;
            case "storage_2": return 2222;
            case "storage_3": return 2223;
            case "storage_4": return 2224;
            default: return 2221;
        }
    }
}