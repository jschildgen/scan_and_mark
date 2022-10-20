module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.pdfbox;
    requires java.desktop;
    requires org.apache.pdfbox.tools;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    opens org.example to javafx.fxml;
    exports org.example;
}
