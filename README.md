# DevOps API

A robust, production-ready DevOps dashboard backend built with Java 25 and Spring Boot. This application serves as the core engine for managing and monitoring deployments, integrating seamlessly with GitHub Actions and offering real-time insights.

## 🚀 Features

### Core Functionality
- **Deployment Tracking:** Real-time monitoring of deployment statuses (Active, Failed, Completed).
- **GitHub Integration:**
  - Automated synchronization via **Webhooks** (`workflow_run` events).
  - Dynamic artifact fetching and build log streaming.
  - Interactive "New Deployment" trigger with branch selection.
- **Real-Time Updates:** WebSocket-based event system for instant UI refreshes.
- **Infrastructure Health:** Automated heartbeat checks for service availability.

### Security & Architecture
- **Authentication:** Stateless **JWT** (JSON Web Token) security with **Spring Security 6**.
- **User Management:** Role-Based Access Control (RBAC), secure password hashing (BCrypt), and profile management.
- **Database:** Persistent storage using **MySQL 8.0**, fully Dockerized.
- **Configuration:** Dynamic application settings managed via API (no restarts required).

## 🛠️ Tech Stack

- **Language:** Java 25
- **Framework:** Spring Boot 3
- **Build Tool:** Gradle
- **Database:** MySQL 8.0
- **Containerization:** Docker & Docker Compose
- **Security:** Spring Security, JJWT

## 📦 Getting Started

### Prerequisites
- Java 25
- Docker & Docker Compose
- GitHub Personal Access Token (for API integration)

### Running the Application

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/devops_api.git
   cd devops_api
   ```

2. **Start Infrastructure (MySQL):**
   ```bash
   cd devops
   docker-compose up -d
   ```

3. **Run the Application:**
   ```bash
   ./gradlew bootRun
   ```

## 🔐 Environment Configuration

The application uses a secure `application.properties` and environment variables. Ensure you have configured:
- `MYSQL_HOST`, `MYSQL_USER`, `MYSQL_PASSWORD`
- `JWT_SECRET`
- `GITHUB_TOKEN` (Optional, can be set via UI)

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request.
