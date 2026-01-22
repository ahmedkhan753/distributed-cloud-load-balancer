package com.cloud.system;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private FileService fileService;
    private TableView<FileRecord> table = new TableView<>();
    private ObservableList<FileRecord> fileData = FXCollections.observableArrayList();

    public void show(Stage stage, String username) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(25));

        // 0. Initialize Shared Services
        LoadBalancer sharedLB = new LoadBalancer();
        this.fileService = new FileService(sharedLB); // Shared LB ensures UI and Logic sync

        // 1. Top Section: Welcome Label & Algorithm Switcher
        Label welcomeLabel = new Label("Welcome, " + username + " | Distributed Cloud Dashboard");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label algoLabel = new Label("Load Balancer Strategy:");
        ComboBox<LoadBalancer.Strategy> algoBox = new ComboBox<>();
        algoBox.getItems().addAll(LoadBalancer.Strategy.values());
        algoBox.setValue(LoadBalancer.Strategy.ROUND_ROBIN);

        algoBox.setOnAction(e -> {
            sharedLB.setStrategy(algoBox.getValue());
            System.out.println("Strategy changed to: " + algoBox.getValue());
        });

        HBox settingsBar = new HBox(15, algoLabel, algoBox);
        settingsBar.setAlignment(Pos.CENTER_RIGHT);

        VBox topContainer = new VBox(10, welcomeLabel, settingsBar);
        topContainer.setPadding(new Insets(0, 0, 15, 0));
        root.setTop(topContainer);

        // 2. Center Section: TabPane (File Manager & Terminal)
        TabPane tabPane = new TabPane();

        // --- File Manager Tab ---
        TableColumn<FileRecord, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        nameCol.setMinWidth(260);

        TableColumn<FileRecord, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        sizeCol.setMinWidth(100);

        TableColumn<FileRecord, String> nodeCol = new TableColumn<>("Storage Mode");
        nodeCol.setCellValueFactory(new PropertyValueFactory<>("storageNode"));
        nodeCol.setMinWidth(150);

        table.getColumns().setAll(nameCol, sizeCol, nodeCol);
        table.setItems(fileData);

        Tab dashboardTab = new Tab("File Manager", table);
        dashboardTab.setClosable(false);

        // --- Cloud Terminal Tab ---
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

        // 3. Bottom Section: Action Buttons
        Button uploadBtn = new Button("Upload New File");
        uploadBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        Button downloadBtn = new Button("Download Selected");
        downloadBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");

        Button refreshBtn = new Button("Reload Files");
        refreshBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white;");

        HBox controls = new HBox(10, uploadBtn, downloadBtn, deleteBtn, refreshBtn, logoutBtn);
        controls.setPadding(new Insets(15, 0, 0, 0));
        controls.setAlignment(Pos.CENTER);
        root.setBottom(controls);

        // --- BUTTON LOGIC ---

        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                fileService.uploadFile(selectedFile, username);
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(ev -> refreshTableData());
                pause.play();
            }
        });

        downloadBtn.setOnAction(e -> {
            FileRecord selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                FileChooser fileSaver = new FileChooser();
                fileSaver.setInitialFileName(selectedFile.getFileName());
                File destination = fileSaver.showSaveDialog(stage);
                if (destination != null) {
                    fileService.downloadAndReassemble(selectedFile.getFileName(), destination);
                }
            } else {
                new Alert(Alert.AlertType.WARNING, "Please select a file first!").show();
            }
        });

        deleteBtn.setOnAction(e -> {
            FileRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                fileService.deleteDistributedFile(selected.getFileName());
                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(ev -> refreshTableData());
                pause.play();
            }
        });

        refreshBtn.setOnAction(e -> refreshTableData());

        logoutBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to end your session?", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("Confirm Logout");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    LocalDatabaseService.clearSession(); //
                    LocalDatabaseService.logActivity("LOGOUT", "User Session Ended");
                    try {
                        new LoginApp().start(stage);
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            });
        });

        // Terminal Logic
        TerminalService terminalService = new TerminalService(username);
        terminalInput.setOnAction(e -> {
            String cmd = terminalInput.getText();
            String result = terminalService.execute(cmd);
            terminalOutput.appendText(cmd + "\n" + result + "\n$ ");
            terminalInput.clear();
        });

        // Final Display
        refreshTableData();
        Scene scene = new Scene(root, 800, 550);
        stage.setTitle("NebulaStore Dashboard | " + username);
        stage.setScene(scene);
        stage.show();
    }

    private void refreshTableData() {
        fileData.clear();

        String sql = "SELECT file_name, file_size, storage_node FROM file_metadata";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                fileData.add(new FileRecord(
                        rs.getString("file_name"),
                        rs.getString("file_size"),
                        rs.getString("storage_node")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}