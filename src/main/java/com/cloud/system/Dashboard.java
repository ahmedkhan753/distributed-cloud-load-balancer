package com.cloud.system;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class Dashboard {
    private FileService fileService = new FileService();
    public void show(Stage stage, String username) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // 1. Top Section: Welcome Message
        Label welcomeLabel = new Label("Welcome, " + username + " | Cloud Dashboard");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        root.setTop(welcomeLabel);

        // 2. Center Section: File Table
        TableView<FileRecord> table = new TableView<>();

        TableColumn<FileRecord, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));

        TableColumn<FileRecord, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("fileSize"));

        TableColumn<FileRecord, String> nodeCol = new TableColumn<>("Storage Node");
        nodeCol.setCellValueFactory(new PropertyValueFactory<>("storageNode"));

        table.getColumns().addAll(nameCol, sizeCol, nodeCol);
        root.setCenter(table);

        // 3. Bottom Section: Action Buttons
        Button uploadBtn = new Button("Upload New File");
        Button refreshBtn = new Button("Refresh List");
        HBox controls = new HBox(10, uploadBtn, refreshBtn);
        controls.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(controls);

        // 4. Upload Logic (Placeholder for now)
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                fileService.uploadFile(selectedFile, username);

                // Show a quick alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "File Uploaded to Cloud!");
                alert.show();
            }
        });

        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("Cloud System Dashboard");
        stage.setScene(scene);
        stage.show();
    }
}