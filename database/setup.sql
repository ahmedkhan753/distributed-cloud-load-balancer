CREATE DATABASE IF NOT EXISTS cloud_system;
USE cloud_system;

-- Table for User Accounts
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'user'
);

-- Table for tracking file chunks across storage nodes
CREATE TABLE IF NOT EXISTS file_metadata (
    id INT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    owner_id INT,
    total_chunks INT,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Insert a default admin for testing
INSERT IGNORE INTO users (username, password_hash, role)
VALUES ('admin', 'admin123', 'admin');