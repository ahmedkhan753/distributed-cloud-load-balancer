#  Distributed Cloud Infrastructure
### Systems Software (COMP20081) Project

 a professional-grade, fault-tolerant cloud storage solution. It demonstrates the coordination between a **Master Node** (Java), a **Central Metadata Ledger** (MySQL), and a **Distributed Storage Cluster** (4x Docker Containers).

---

## 🏗 System Architecture

The project is built on a **Stateful Master-Worker** model to ensure data integrity and high availability:

* **Master Node:** The central "brain" handling user authentication, AES-256 encryption, and file chunking (Req 20).
* **Local Cache (SQLite):** Acts as the system's "short-term memory." It stores session data and a local activity audit trail, allowing the app to run faster and maintain history even if the server is offline.
* **Worker Cluster:** Four isolated SFTP environments (Ports 2221–2224) that serve as physical storage nodes managed via Docker.
* **Load Balancer:** A dynamic router that switches between **Round-Robin**, **Random**, and **Priority** scheduling algorithms based on user choice.



---

## 🚀 Quick Start Guide

### 1. Environment Preparation (PowerShell)
Before launching the system, create the physical volume mounts on your host machine:
`mkdir storage1_uploads, storage2_uploads, storage3_uploads, storage4_uploads`

### 2. Launch the Infrastructure
Deploy the storage cluster using Docker Compose:
`docker-compose up -d`

### 3. Initialize the Databases
The system requires both a remote persistent store and a local session cache:
* **MySQL (Remote):** Create a database named `cloud_system` and execute the provided `schema.sql`.
* **SQLite (Local):** No setup required. The application generates `local_session.db` automatically upon the first login to track activity.

---

## 🛠 Technical Requirements Met

| Requirement | Implementation Detail |
| :--- | :--- |
| **Req 6: Security** | AES-256 Bit Encryption applied to file chunks before transmission. |
| **Req 7/9: Balancing** | Real-time health checks ensure offline nodes are skipped automatically. |
| **Req 10: Validation** | Artificial 30-90s latency simulation tests background thread stability. |
| **Req 19: Terminal** | Integrated `CloudShell` supports `ls`, `pwd`, `whoami`, and `ps`. |
| **Req 20: Chunking** | Files are bisected into two discrete parts for distributed storage. |



---

## 💻 Cloud Terminal Commands
Use the **Cloud Terminal** tab to interact with the system via CLI:
* `ls` - Queries the central MySQL ledger for all distributed files.
* `whoami` - Identifies the active user stored in the local SQLite session.
* `status` - Displays the health and current balancing algorithm of the 4 nodes.

---

## ⚖️ Troubleshooting
* **Dependency Issues:** Ensure `sqlite-jdbc` is set to version `3.45.1.0` in your `pom.xml` and click the **Maven Reload** button in IntelliJ.
* **SQL Errors:** Ensure the `users` table is populated before attempting to upload files, as the system links metadata to user IDs.