package com.cloud.system;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

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

        // 1. Top Section
        Label welcomeLabel = new Label("Welcome, " + username + " | Distributed Cloud Dashboard");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        root.setTop(welcomeLabel);

        // 2. Table Configuration
        TableColumn<FileRecord, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        nameCol.setMinWidth(250);

        TableColumn<FileRecord, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        sizeCol.setMinWidth(100);

        TableColumn<FileRecord, String> nodeCol = new TableColumn<>("Storage Mode");
        nodeCol.setCellValueFactory(new PropertyValueFactory<>("storageNode"));
        nodeCol.setMinWidth(150);

        table.getColumns().addAll(nameCol, sizeCol, nodeCol);
        table.setItems(fileData);
        root.setCenter(table);

        // 3. Action Buttons
        Button uploadBtn = new Button("Upload New File");
        Button refreshBtn = new Button("Refresh List");
        Button downloadBtn = new Button("Download Selected");
        Button deleteBtn = new Button("Delete Selected");

        deleteBtn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");

        HBox controls = new HBox(10, uploadBtn, downloadBtn, deleteBtn, refreshBtn);
        controls.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(controls);

        // --- BUTTON LOGIC ---

        // UPLOAD (Now splits and encrypts)
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                fileService.uploadFile(selectedFile, username);
                // Auto-refresh after a small delay to allow SFTP and SQL to finish
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(ev -> refreshTableData());
                pause.play();
            }
        });

        // DOWNLOAD (Now reassembles and decrypts)
        downloadBtn.setOnAction(e -> {
            FileRecord selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                FileChooser fileSaver = new FileChooser();
                fileSaver.setInitialFileName(selectedFile.getFileName());
                File destination = fileSaver.showSaveDialog(stage);

                if (destination != null) {
                    // FIXED: Using the new reassemble method
                    fileService.downloadAndReassemble(selectedFile.getFileName(), destination);
                }
            } else {
                new Alert(Alert.AlertType.WARNING, "Please select a file first!").show();
            }
        });

        // DELETE (Now cleans up both storage nodes)
        deleteBtn.setOnAction(e -> {
            FileRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // FIXED: Using the new distributed delete method
                fileService.deleteDistributedFile(selected.getFileName());

                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(ev -> refreshTableData());
                pause.play();
            }
        });

        refreshBtn.setOnAction(e -> refreshTableData());
        refreshTableData();

        Scene scene = new Scene(root, 700, 450);
        stage.setTitle("Cloud System Dashboard");
        stage.setScene(scene);
        stage.show();

        // Inside Dashboard.java show() method:

        TabPane tabPane = new TabPane();
        Tab dashboardTab = new Tab("File Manager", root.getCenter());
        dashboardTab.setClosable(false);

// Create the Terminal Tab
        VBox terminalBox = new VBox(10);
        terminalBox.setPadding(new Insets(10));
        TextArea terminalOutput = new TextArea("Welcome to CloudShell v1.0\nType 'help' for commands...\n\n$ ");
        terminalOutput.setEditable(false);
        terminalOutput.setStyle("-fx-control-inner-background: black; -fx-text-fill: lime; -fx-font-family: 'Courier New';");
        terminalOutput.setPrefHeight(300);

        TextField terminalInput = new TextField();
        terminalInput.setPromptText("Enter command...");

        terminalBox.getChildren().addAll(terminalOutput, terminalInput);
        Tab terminalTab = new Tab("Cloud Terminal", terminalBox);
        terminalTab.setClosable(false);

        tabPane.getTabs().addAll(dashboardTab, terminalTab);
        root.setCenter(tabPane);

// Logic to handle terminal commands
        TerminalService terminalService = new TerminalService(username);
        terminalInput.setOnAction(e -> {
            String cmd = terminalInput.getText();
            String result = terminalService.execute(cmd);
            terminalOutput.appendText(cmd + "\n" + result + "\n$ ");
            terminalInput.clear();
        });
    }

    private void refreshTableData() {
        fileData.clear();
        // Updated Query: Selecting from the metadata table updated for distributed storage
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}