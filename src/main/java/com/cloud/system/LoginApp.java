package com.cloud.system;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginApp extends Application {

    private UserDAO userDAO = new UserDAO();

    @Override
    public void start(Stage primaryStage) {
        // 1. Create UI Controls
        Label titleLabel = new Label("Cloud System Login");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField userField = new TextField();
        userField.setPromptText("Username");
        userField.setMaxWidth(200);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setMaxWidth(200);

        Button loginButton = new Button("Login");
        Label statusLabel = new Label("");

        // 2. Handle Button Click
        loginButton.setOnAction(e -> {
            String user = userField.getText();
            String pass = passField.getText();

            if (userDAO.validateLogin(user, pass)) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Login Successful! Welcome " + user);
                // Later we will open the Dashboard here
            } else {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("Invalid Credentials");
            }
        });

        // 3. Layout (Vertical Box)
        VBox layout = new VBox(15); // 15px spacing
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, userField, passField, loginButton, statusLabel);

        // 4. Scene and Stage
        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setTitle("Distributed Cloud Load Balancer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}