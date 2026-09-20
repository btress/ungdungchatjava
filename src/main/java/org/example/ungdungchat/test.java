package org.example.ungdungchat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class test extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Ứng dụng Chat - TCP + JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}