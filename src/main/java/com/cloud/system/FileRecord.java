package com.cloud.system;

public class FileRecord {
    private int id;
    private String fileName;
    private String fileSize;
    private String uploadDate;
    private String storageNode; // Server 1 or Server 2

    public FileRecord(int id, String fileName, String fileSize, String uploadDate, String storageNode) {
        this.id = id;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadDate = uploadDate;
        this.storageNode = storageNode;
    }

    // Getters (Required for JavaFX TableView to work)
    public int getId() { return id; }
    public String getFileName() { return fileName; }
    public String getFileSize() { return fileSize; }
    public String getUploadDate() { return uploadDate; }
    public String getStorageNode() { return storageNode; }
}