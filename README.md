# RevisedAttendanceApp

## Project Overview

RevisedAttendanceApp is a desktop attendance management application developed using Java and JavaFX. It provides a graphical interface for recording and managing student attendance. The application uses SQLite to store attendance data.

## Technologies Used

- **Java 21**: Application logic
- **JavaFX 21**: Graphical user interface
- **FXML**: Interface layout
- **CSS**: Interface styling
- **SQLite**: Database management
- **Maven**: Dependency management and project build

## Main Features

- Mark students as present or absent
- Manage attendance records by date
- Store attendance data in an SQLite database
- Use a graphical interface to manage attendance
- View and manage student attendance records

## Project Structure

```text
RevisedAttendanceApp/
├── attendance.db
├── How to Run.txt
├── mvnw
├── mvnw.cmd
├── pom.xml
├── readme.md
└── src/
    └── main/
        ├── java/
        │   ├── module-info.java
        │   └── com/example/revisedattendanceapp/
        │       ├── AttendanceController.java
        │       ├── AttendanceRecord.java
        │       ├── DatabaseHandler.java
        │       ├── Launcher.java
        │       └── MainApp.java
        └── resources/
            └── com/example/revisedattendanceapp/
                ├── AttendanceView.fxml
                └── style.css
```

## Requirements

- JDK 21
- Maven support
- An IDE such as IntelliJ IDEA
- Internet connection for downloading dependencies on the first build, if they are not already available

## How to Run

1. Download or clone this repository.
2. Open the project in IntelliJ IDEA.
3. Make sure JDK 21 is configured.
4. Allow Maven to load the dependencies listed in `pom.xml`.
5. Run the application's main class, `com.example.revisedattendanceapp.Launcher`.

For additional instructions, see `How to Run.txt`.

## Database

The application uses SQLite to store attendance information. The database file is named `attendance.db`. If the application creates the database automatically, it may initialize the required tables when it runs.

## Author

**Sajim Mahmud Seeam**

Computer Science and Engineering Student

## Project Purpose

This project was developed as a programming project to practice Java programming, JavaFX interface design, database operations, and application development.
