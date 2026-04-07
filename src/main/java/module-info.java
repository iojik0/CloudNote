module com.cloudnote.cloudnote {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;
    requires java.sql;

    opens com.cloudnote to javafx.fxml;
    exports com.cloudnote;
    exports com.cloudnote.controller;
    opens com.cloudnote.controller to javafx.fxml;
}