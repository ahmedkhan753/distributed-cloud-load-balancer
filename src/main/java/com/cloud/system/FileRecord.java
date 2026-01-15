package com.cloud.system;

import javafx.beans.property.SimpleStringProperty;

public class FileRecord {
    private final SimpleStringProperty fileName;
    private final SimpleStringProperty fileSize;
    private final SimpleStringProperty storageNode;

    public FileRecord(String fileName, String fileSize, String storageNode) {
        this.fileName = new SimpleStringProperty(fileName);
        this.fileSize = new SimpleStringProperty(fileSize);
        this.storageNode = new SimpleStringProperty(storageNode);
    }

    public String getFileName() { return fileName.get(); }
    public String getFileSize() { return fileSize.get(); }
    public String getStorageNode() { return storageNode.get(); }
}