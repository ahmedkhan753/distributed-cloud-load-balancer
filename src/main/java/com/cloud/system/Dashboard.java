package com.cloud.system;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Dashboard {
    private FileService fileService = new FileService();
    private TableView<FileRecord> table = new TableView<>();
    private ObservableList<FileRecord> fileData = FXCollections.observableArrayList();

    public void show(Stage stage, String username) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // 1. Top Section: Welcome Message
        Label welcomeLabel = new Label("Welcome, " + username + " | Cloud Dashboard");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        root.setTop(welcomeLabel);

        // 2. Center Section: File Table Configuration
        TableColumn<FileRecord, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        nameCol.setMinWidth(250);

        TableColumn<FileRecord, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        sizeCol.setMinWidth(100);

        TableColumn<FileRecord, String> nodeCol = new TableColumn<>("Storage Node");
        nodeCol.setCellValueFactory(new PropertyValueFactory<>("storageNode"));
        nodeCol.setMinWidth(150);

        table.getColumns().addAll(nameCol, sizeCol, nodeCol);
        table.setItems(fileData); // Bind the table to our list
        root.setCenter(table);

        // 3. Bottom Section: Action Buttons
        Button uploadBtn = new Button("Upload New File");
        Button refreshBtn = new Button("Refresh List");
        HBox controls = new HBox(10, uploadBtn, refreshBtn);
        controls.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(controls);

        // 4. Button Actions
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                fileService.uploadFile(selectedFile, username);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "File Uploading... Click Refresh in a few seconds.");
                alert.show();
            }
        });

        // Link Refresh button to our data fetching method
        refreshBtn.setOnAction(e -> refreshTableData());

        // Initial load: fetch data when dashboard opens
        refreshTableData();

        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("Cloud System Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    // This method connects to MySQL and fetches the metadata
    private void refreshTableData() {
        fileData.clear(); // Clear existing rows
        String sql = "SELECT file_name, file_size, storage_node FROM file_metadata";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                fileData.add(new FileRecord(
                        rs.getString("file_name"),
                        rs.getString("file_size"),
                        rs.getString("storage_node")
                ));
            }
            System.out.println("🔄 Dashboard table refreshed from Database.");

        } catch (Exception e) {
            System.err.println("❌ Failed to fetch table data");
            e.printStackTrace();
        }
    }
}