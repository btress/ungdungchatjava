module org.example.ungdungchat {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens org.example.ungdungchat to javafx.fxml;
    exports org.example.ungdungchat;
}