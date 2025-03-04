module com.ethankiers.databasemanagement.databasemanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires org.apache.poi.ooxml;
    requires org.apache.logging.log4j;
    requires java.sql;

    opens com.ethankiers.databasemanagement.databasemanager to javafx.fxml;
    exports com.ethankiers.databasemanagement.databasemanager;
}