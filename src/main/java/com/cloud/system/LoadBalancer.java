package com.cloud.system;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {
    private List<String> storageNodes;
    private int currentIndex = 0;

    public LoadBalancer() {
        storageNodes = new ArrayList<>();
        // These names must match your service names in docker-compose.yml
        storageNodes.add("storage_1");
        storageNodes.add("storage_2");
    }

    // The Round Robin Algorithm
    public synchronized String getNextNode() {
        if (currentIndex >= storageNodes.size()) {
            currentIndex = 0;
        }
        String selectedNode = storageNodes.get(currentIndex);
        currentIndex++;
        return selectedNode;
    }
}