## Project Overview

This repository contains the backend for the CapX platform, developed using Java Spring Boot. The backend provides APIs for managing portfolios, handling stock transactions, and supporting real-time data updates.

## Deployed Backend Links

[Base Url](https://backend-capx-production.up.railway.app)

[Dashboard data](https://backend-capx-production.up.railway.app/data/dashboard)
[Portfolio data](https://backend-capx-production.up.railway.app/data/portfolio)
[History data](https://backend-capx-production.up.railway.app/data/history)

## Features

### Key Functionality
- **Portfolio Management**: Handles CRUD operations for user portfolios, including adding, updating, and deleting stocks.
- **Real-Time Updates**: Supports live price updates for stocks.
- **Transaction Logging**: Records all stock transactions, including buy and sell operations, with timestamps.
- **Industry Insights**: Provides data aggregation for industry-specific portfolio breakdowns.

## Tech Stack
- **Java 17**: Core programming language.
- **Spring Boot**: Backend framework for RESTful APIs.
- **Hibernate/JPA**: For database interactions.
- **MySQL/PostgreSQL**: Relational database for data persistence.
- **Maven**: Dependency management and build tool.

## How to Run the Project

### Prerequisites
Ensure the following tools are installed:
- [Java JDK 17](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
- [Maven](https://maven.apache.org/)
- [MySQL/PostgreSQL](https://www.mysql.com/)

## Setting Up Rapid Api for key
- Visit [Rapidapi](https://rapidapi.com/apidojo/api/yahoo-finance1)
- Create an account and subscribe to Yahoo-finance1.
- Note down the Api X-RapidAPI-Key.
- It has a limit of 500 req/Month

### Setup Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/capx-backend.git
   cd capx-backend
2. Configure the backend
   Create a database named capx in MySQL.
    Update the database connection details in src/main/resources/application.properties:
    spring.datasource.url=jdbc:mysql://localhost:3306/capx
    spring.datasource.username=<your-username>
    spring.datasource.password=<your-password>

3. Setup the environment variable RAPIDAPI_KEY
   
4. Build and run the project
    ```bash
    mvn clean install
    mvn spring-boot:run
   ```

6. Access the APIs:

Base URL: http://localhost:8080


## Folder Structure
```
capx-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com.capx/
│   │   │   │   ├── controller/              # RESTful API endpoints
│   │   │   │   │   ├── StockController.java
│   │   │   │   ├── model/                   # Domain models and response objects
│   │   │   │   │   ├── DashboardResponseModel.java
│   │   │   │   │   ├── Stock.java
│   │   │   │   │   ├── StockEntity.java
│   │   │   │   │   ├── TransactionEntity.java
│   │   │   │   ├── repository/              # Data access layer
│   │   │   │   │   ├── PortfolioRepository.java
│   │   │   │   │   ├── TransactionRepository.java
│   │   │   │   ├── services/                # Business logic
│   │   │   │   │   ├── api/                 # Service interfaces
│   │   │   │   │   │   ├── StockServiceInterface.java
│   │   │   │   │   ├── impl/                # Service implementations
│   │   │   │   │       ├── StockService.java
│   ├── resources/
│   │   ├── application.properties           # Application configuration
│   │   ├── schema.sql                       # Optional: Database schema initialization
├── pom.xml                                  # Maven dependencies and build configuration
└── README.md                                # Project documentation


```
