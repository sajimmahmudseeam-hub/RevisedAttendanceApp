
# AttendanceApp
### JavaFX-Based Attendance Management System

## 1. Project Overview

AttendanceApp is a Java desktop application developed to manage and review student attendance. It provides a graphical user interface for recording attendance, viewing records across different time periods, calculating attendance rates, and identifying students whose attendance may fall below the required threshold.

The project demonstrates the practical application of Java programming, data structures, JavaFX interface development, and SQLite database integration.

## 2. Features

- Student attendance management
- Separate weekly and monthly attendance views
- Attendance-rate calculations
- At-risk student alerts
- SQLite database integration
- CSV student import
- CSV attendance report export
- JavaFX graphical user interface
- FXML-based interface layout
- CSS-based interface styling

## 3. Technologies Used

| Technology | Purpose |
|---|---|
| Java | Application logic |
| JDK 26 | Configured Java compilation target |
| JavaFX 21.0.6 | Desktop user interface |
| FXML | Interface layout |
| CSS | Interface styling |
| SQLite | Data storage |
| JDBC | Java-to-database connectivity |
| Maven | Dependency and build management |
| IntelliJ IDEA | Recommended development environment |

The Java version and dependency versions are based on the current project configuration in `pom.xml`.

## 4. Data Structures

The application uses Java collections to organize and process information.

- **HashMap:** Stores key-value mappings, including the course-to-professor mapping.
- **LinkedHashMap:** Combines attendance records by student ID while preserving insertion order.
- **ArrayList:** Stores collections of records returned by database queries.
- **ObservableList:** Holds observable collections used by JavaFX interface controls.
- **FilteredList:** Filters displayed records according to the application's filtering logic.

These structures have different roles. `HashMap`, `LinkedHashMap`, and `ArrayList` are Java collection implementations. `ObservableList` and `FilteredList` are JavaFX collection utilities.

## 5. Project Structure

Important files and their responsibilities are listed below.

- `pom.xml` — Configures Maven dependencies, Java compilation settings, and the JavaFX launch plugin.
- `src/main/java/com/example/revisedattendanceapp/Launcher.java` — Provides the application entry point.
- `src/main/java/com/example/revisedattendanceapp/MainApp.java` — Initializes the JavaFX application and loads the main interface.
- `src/main/java/com/example/revisedattendanceapp/AttendanceController.java` — Handles interface events, attendance operations, view logic, and related application behavior.
- `src/main/java/com/example/revisedattendanceapp/AttendanceRecord.java` — Represents attendance record data.
- `src/main/java/com/example/revisedattendanceapp/DatabaseHandler.java` — Handles SQLite database operations.
- `src/main/java/module-info.java` — Defines the Java module configuration.
- `src/main/resources/com/example/revisedattendanceapp/AttendanceView.fxml` — Defines the main interface layout.
- `src/main/resources/com/example/revisedattendanceapp/style.css` — Contains the interface styling rules.
- `attendance.db` — The SQLite database file used by the application.

## 6. Requirements

Before running the application, prepare:

- JDK 26
- Maven, or a compatible Maven configuration in IntelliJ IDEA
- IntelliJ IDEA or another Java IDE capable of opening the Maven project
- Internet access if Maven needs to download dependencies

The project is currently configured to compile for Java 26. Configure the project JDK accordingly rather than assuming that JavaFX 21 means the project targets JDK 21.

## 7. How to Run

### Option A: Run Using IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Open the `RevisedAttendanceApp` project folder.
3. Configure the project JDK to use JDK 26.
4. Open `pom.xml` and reload the Maven project.
5. Wait for Maven dependencies to resolve.
6. Open `src/main/java/com/example/revisedattendanceapp/Launcher.java`.
7. Run the `main()` method using the green Run icon.
8. Check that the AttendanceApp window opens.

Set the working directory to the project root, where `pom.xml` and `attendance.db` are located.

### Option B: Run Using Maven

Open a terminal in the project root and execute:

```bash
mvn clean javafx:run
```

This requires Maven to be installed and available in the terminal, along with a compatible JDK 26 setup.

## 8. Database Information

AttendanceApp uses SQLite through JDBC. The database handler connects using the relative database path `attendance.db`.

Run the application with the project root as its working directory so that it uses the expected database location.

Back up the database before replacing or deleting it. Before uploading the project to a public repository, check whether the database contains personal student information or other private records. Do not publish private data; use appropriately sanitized sample data when necessary.

## 9. Verification and Limitations

The documented features describe the application's intended and implemented functionality. Build success and runtime behavior should be verified in the target environment before being reported as tested.

Important checks include weekly and monthly view separation, attendance-rate calculations, at-risk threshold behavior, database persistence after restarting the application, and CSV import/export.

## 10. Author

**Seeam Sajim Mahmud**

## 11. Academic Purpose

This project was developed for academic purposes to demonstrate Java application development, data structures, graphical user interface design, and database integration.
