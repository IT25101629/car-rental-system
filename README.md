#  Car Rental Management System

A full-stack **Car Rental Management Web Application** built with **Spring Boot** (Java) backend and a vanilla **HTML/CSS/JavaScript** frontend. The system supports multiple user roles and manages the full rental lifecycle — from reservations to vehicle returns.

---

##  Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Roles & Dashboards](#roles--dashboards)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [Default Credentials](#default-credentials)

---

##  Features

-  Role-based authentication (Admin, Customer, Driver, Fleet Manager, Rental Staff)
-  Vehicle management & availability tracking
-  Reservation & rental management
- ️ Maintenance request tracking
-  Pickup & return record management
-  Driver return reports
- ️ Microsoft SQL Server database integration

---

## 🛠 Tech Stack

| Layer      | Technology                          |
|------------|-------------------------------------|
| Backend    | Java 17+, Spring Boot, Spring Data JPA |
| Frontend   | HTML5, CSS3, Vanilla JavaScript     |
| Database   | Microsoft SQL Server                |
| Build Tool | Maven                               |
| Server     | Embedded Tomcat (port `8080`)       |

---

##  Project Structure

```
WEBSITE/
├── src/
│   ├── main/
│   │   ├── java/com/carrental/
│   │   │   ├── controller/        # REST API controllers
│   │   │   ├── model/             # JPA entity models
│   │   │   ├── repository/        # Spring Data repositories
│   │   │   ├── service/           # Business logic services
│   │   │   └── DatabaseConnection.java
│   │   └── resources/
│   │       ├── static/            # Frontend (HTML, JS)
│   │       │   ├── index.html
│   │       │   ├── login.html
│   │       │   ├── register.html
│   │       │   ├── admin.html
│   │       │   ├── customer.html
│   │       │   ├── driver.html
│   │       │   ├── fleet-manager.html
│   │       │   ├── rental-staff.html
│   │       │   ├── vehicles.html
│   │       │   └── js/            # JavaScript modules
│   │       ├── application.properties
│   │       └── schema_sqlserver.sql
│   └── test/
├── setup_db.sql                   # One-time database setup script
├── database.local.properties      # Local DB credentials (do not commit!)
├── view-database.sql              # Utility queries
└── view-demo-logins.sql           # Demo login credentials
```

---

##  Roles & Dashboards

| Role           | Dashboard Page          | Capabilities                                      |
|----------------|------------------------|---------------------------------------------------|
| **Admin**      | `admin.html`           | Full system control, user management              |
| **Customer**   | `customer.html`        | Browse vehicles, make & view reservations         |
| **Driver**     | `driver.html`          | View assigned pickups, submit return reports      |
| **Fleet Manager** | `fleet-manager.html` | Manage vehicles, handle maintenance requests    |
| **Rental Staff** | `rental-staff.html`  | Process pickups, returns, and rental records      |

---

##  Prerequisites

- **Java 17+** installed
- **Maven** installed
- **Microsoft SQL Server** (local instance running on default or custom port)
- **SSMS** or any SQL client (optional, for DB inspection)

---

##  Database Setup

> Run this **once** before starting the application.

1. Open **SQL Server Management Studio (SSMS)** or `sqlcmd`.
2. Run the setup script:

```sql
-- Run setup_db.sql in SSMS
```

Or via command line:
```bash
sqlcmd -S localhost -E -i setup_db.sql
```

3. Then run the schema to create all tables:

```bash
sqlcmd -S localhost -U carrental_app -P "CarRental@2024!" -d CarRentalDB -i src/main/resources/schema_sqlserver.sql
```

4. Make sure `database.local.properties` exists in the project root with:
```properties
CARRENTAL_DB_PORT=54465
CARRENTAL_DB_PASSWORD=CarRental@2024!
```

>  **Never commit** `database.local.properties` to version control — it contains sensitive credentials.

---

##  Running the Application

```bash
# Build and run with Maven
mvn spring-boot:run
```

Or build a JAR and run:
```bash
mvn clean package
java -jar target/car-rental-backend-*.jar
```

The application will start at: **http://localhost:8080**

---

##  Default Credentials

Check `view-demo-logins.sql` for demo user credentials.

| Role           | Login Page               |
|----------------|--------------------------|
| Admin          | http://localhost:8080/login.html |
| Customer       | http://localhost:8080/login.html |
| Driver         | http://localhost:8080/login.html |
| Fleet Manager  | http://localhost:8080/login.html |
| Rental Staff   | http://localhost:8080/login.html |



