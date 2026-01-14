package com.cloud.system;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // This trick bypasses the JavaFX runtime check
        Application.launch(LoginApp.class, args);
    }
}