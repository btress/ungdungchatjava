package org.example.ungdungchat;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller của hello-view.fxml - màn hình nhập tên và địa chỉ server.
 */
public class HelloController {

    @FXML private TextField txtTen;
    @FXML private TextField txtHost;
    @FXML private TextField txtPort;
    @FXML private Button btnKetNoi;
    @FXML private Label lblLoi;

    @FXML
    protected void onKetNoi() {
        String ten = txtTen.getText().trim();
        if (ten.isEmpty()) {
            lblLoi.setText("Bạn chưa nhập tên hiển thị.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            lblLoi.setText("Port không hợp lệ.");
            return;
        }

        String host = txtHost.getText().trim();
        if (host.isEmpty()) host = "localhost";

        btnKetNoi.setDisable(true);
        lblLoi.setText("Đang kết nối...");

        ChatClient client = new ChatClient(host, port, ten);
        try {
            client.connect();
        } catch (IOException e) {
            btnKetNoi.setDisable(false);
            lblLoi.setText("Không kết nối được server: " + e.getMessage());
            return;
        }

        try {
            // Nạp màn hình chat và truyền client sang cho ChatController
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("chat-view.fxml"));
            Scene scene = new Scene(loader.load());

            ChatController controller = loader.getController();
            Stage stage = (Stage) btnKetNoi.getScene().getWindow();
            controller.init(client, stage);

            stage.setScene(scene);
            stage.setTitle("Phòng chat - " + ten);
            stage.centerOnScreen();

            client.start();   // bắt đầu nhận dữ liệu sau khi UI sẵn sàng

        } catch (IOException e) {
            client.close();
            btnKetNoi.setDisable(false);
            lblLoi.setText("Lỗi nạp giao diện: " + e.getMessage());
        }
    }
}