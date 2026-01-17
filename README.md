# ☁️ NebulaStore: A Distributed, Fault-Tolerant Cloud Infrastructure
### Systems Software (COMP20081) Project

NebulaStore is a high-performance cloud storage solution built to demonstrate the principles of distributed systems, container orchestration, and secure data management. Unlike standard local storage, this system utilizes a **Master-Worker architecture** to split, encrypt, and distribute data across a cluster of four independent storage nodes.

---

## 🏗 System Architecture
The project is built on a **"Stateful Master-Worker"** model:

* **The Master Node (Java/MySQL):** Acts as the brain of the system. It handles user authentication, file chunking, AES-256 encryption, and maintains the "Global Ledger" in MySQL.
* **The Worker Nodes (4x Docker Containers):** These are isolated SFTP storage environments. They hold the physical encrypted data chunks but have no knowledge of the file's overall structure.
* **The Load Balancer:** An internal service that monitors node health and ensures data is distributed evenly across the cluster using a Round-Robin algorithm.

---

## 🚀 Quick Start Guide

### 1. Environment Preparation
Ensure you have Docker Desktop running and MySQL active. In the project root, run the following to create the physical volume mounts:
```powershell
mkdir storage1_uploads, storage2_uploads, storage3_uploads, storage4_uploads

### 2. Launch the Infrastructure
Deploy the storage cluster using Docker Compose:

PowerShell

docker-compose up -d
This spins up 4 containers on ports 2221, 2222, 2223, and 2224.

### 3. Database InitializationExecute the following SQL commands to set up the metadata ledger:SQLCREATE DATABASE cloud_system;
USE cloud_system;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE file_metadata (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size VARCHAR(50),
    storage_node VARCHAR(50) DEFAULT 'Distributed',
    user_id INT,
    node_part1 VARCHAR(50),
    node_part2 VARCHAR(50),
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Docker Startup
```powershell
docker-compose up -d

