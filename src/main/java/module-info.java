module com.ethankiers.databasemanagement.databasemanager {

    requires org.apache.poi.ooxml;
    requires org.apache.logging.log4j;
    requires java.sql;

    opens com.ethankiers.databasemanagement.databasemanager to javafx.fxml;
    exports com.ethankiers.databasemanagement.databasemanager;
}