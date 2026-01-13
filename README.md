This README.md is designed to provide a professional overview of your Systems Software (COMP20081) coursework. It covers the project description, technical architecture, and setup instructions based on the specific requirements of the assignment.


Cloud Load Balancer & Distributed Storage System
Project Overview
This project is a simulated cloud infrastructure developed for the COMP20081 Systems Software module. It features a functional load balancer that manages network traffic across multiple microservices and file storage containers. The system is built using JavaFX and orchestrated via Docker Compose to ensure scalability, reliability, and data integrity.

Key Features

Load Balancing & Scheduling: Implements at least three scheduling algorithms (e.g., Round Robin, FCFS, SJN) to distribute requests evenly across servers.



Microservices Architecture: Utilizes specialized Docker containers for file partitioning, database management, and storage.




Security: High-level security including AES encryption for file chunks and user passwords, alongside comprehensive Access Control Lists (ACLs).


Database Synchronization: Real-time data consistency maintained between a local SQLite database (for sessions) and a remote MySQL database (for user profiles and metadata).


Terminal Emulation: Integrated shell allowing users to execute standard commands like ls, mv, cp, mkdir, and whoami.



Real-world Latency Simulation: Introduces artificial delays of 30 to 90 seconds to emulate actual cloud response times.

System Architecture
The system consists of several orchestrated Docker containers:



Main Container: Hosts the JavaFX GUI and local SQLite database.



Load Balancer: Manages traffic distribution and resource scaling.



File Partitioning & Aggregator: Handles file chunking, AES encryption/decryption, and CRC32 validation.



MySQL Database: Centralized storage for user profiles and system logs.



File Storage Containers: Two or more containers dedicated to storing encrypted file chunks.




Host Manager: A support container for managing the Docker environment

Technical Stack

Language: Java (JDK 20+).


Build Tool: Maven.



UI Framework: JavaFX with Scene Builder.


Containerization: Docker & Docker Compose.



Database: SQLite (Local) and MySQL (Remote).


Communication Protocols: MQTT, OpenSSH, and TCP .