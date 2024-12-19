module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.pdfbox;
    requires java.desktop;
    requires org.apache.pdfbox.tools;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires java.sql;
    requires freemarker;
    requires javafx.swing;
    requires commons.csv;
    requires freetts;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    opens org.example to javafx.fxml;
    exports org.example;
    exports org.example.model;
}
