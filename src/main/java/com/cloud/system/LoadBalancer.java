package com.cloud.system;

import java.util.*;
import java.net.Socket;

public class LoadBalancer {
    public enum Strategy { ROUND_ROBIN, RANDOM, PRIORITY }
    private Strategy currentStrategy = Strategy.ROUND_ROBIN;

    private List<String> storageNodes = Arrays.asList("storage_1", "storage_2", "storage_3", "storage_4");
    private int currentIndex = 0;

    public void setStrategy(Strategy strategy) { this.currentStrategy = strategy; }

    public String getNextNode() {
        switch (currentStrategy) {
            case RANDOM:
                return getRandomHealthyNode();
            case PRIORITY:
                return getFirstHealthyNode();
            case ROUND_ROBIN:
            default:
                return getRoundRobinHealthyNode();
        }
    }

    // 1. Round Robin Algorithm
    private String getRoundRobinHealthyNode() {
        for (int i = 0; i < storageNodes.size(); i++) {
            String node = storageNodes.get(currentIndex);
            currentIndex = (currentIndex + 1) % storageNodes.size();
            if (isNodeAlive(node)) return node;
        }
        return null;
    }

    // 2. Random Selection Algorithm
    private String getRandomHealthyNode() {
        List<String> shuffled = new ArrayList<>(storageNodes);
        Collections.shuffle(shuffled);
        for (String node : shuffled) {
            if (isNodeAlive(node)) return node;
        }
        return null;
    }

    // 3. Priority (First Available) Algorithm
    private String getFirstHealthyNode() {
        for (String node : storageNodes) {
            if (isNodeAlive(node)) return node;
        }
        return null;
    }

    private boolean isNodeAlive(String nodeName) {
        int port = getPortForNode(nodeName);
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), 500);
            return true;
        } catch (Exception e) { return false; }
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