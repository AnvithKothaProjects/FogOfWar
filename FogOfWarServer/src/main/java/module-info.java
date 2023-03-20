module com.example.starterfile {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.logging;


    opens com.example.starterfile to javafx.fxml;
    exports com.example.starterfile;
}