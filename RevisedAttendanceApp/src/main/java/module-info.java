module com.example.revisedattendanceapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.revisedattendanceapp to javafx.fxml;
    exports com.example.revisedattendanceapp;
}