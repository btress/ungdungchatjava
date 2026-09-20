package org.example.ungdungchat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller của chat-view.fxml - phòng chat, hiển thị tin nhắn và file.
 */
public class ChatController implements ChatClient.Listener {

    @FXML private Label lblTieuDe;
    @FXML private Label lblTen;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox khungTinNhan;
    @FXML private ListView<String> danhSachOnline;
    @FXML private TextField oNhap;

    private final DateTimeFormatter gio = DateTimeFormatter.ofPattern("HH:mm");

    private ChatClient client;
    private Stage stage;
    private File thuMucLuu;

    /** Được gọi từ HelloController ngay sau khi nạp FXML */
    public void init(ChatClient client, Stage stage) {
        this.client = client;
        this.stage = stage;
        client.setListener(this);

        lblTen.setText("Bạn: " + client.getUserName());

        thuMucLuu = new File("received_files_"
                + client.getUserName().replaceAll("[^a-zA-Z0-9]", "_"));
        if (!thuMucLuu.exists()) thuMucLuu.mkdirs();

        // Tự cuộn xuống cuối khi có tin nhắn mới
        khungTinNhan.heightProperty().addListener((o, a, b) -> scrollPane.setVvalue(1.0));

        stage.setOnCloseRequest(e -> {
            client.close();
            Platform.exit();
            System.exit(0);
        });

        Platform.runLater(() -> oNhap.requestFocus());
    }

    // ==================================================================
    //  Sự kiện từ giao diện (khai báo onAction trong FXML)
    // ==================================================================

    @FXML
    protected void onGuiTinNhan() {
        String tinnhan = oNhap.getText().trim();
        if (tinnhan.isEmpty()) return;
        try {
            client.sendMessage(tinnhan);
            oNhap.clear();
        } catch (IOException e) {
            themThongBao("Không gửi được tin nhắn: " + e.getMessage());
        }
    }

    @FXML
    protected void onGuiFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn file để gửi");
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        // Gửi ở thread riêng để không đóng băng giao diện khi file lớn
        new Thread(() -> {
            try {
                client.sendFile(file);
                Platform.runLater(() ->
                        themBongBongFile("Bạn", file.getName(), file.length(), file, true));
            } catch (IOException e) {
                Platform.runLater(() -> themThongBao("Gửi file thất bại: " + e.getMessage()));
            }
        }).start();
    }

    // ==================================================================
    //  Sự kiện từ mạng (luôn bọc trong Platform.runLater)
    // ==================================================================

    @Override
    public void onMessage(String sender, String content) {
        Platform.runLater(() ->
                themBongBong(sender, content, sender.equals(client.getUserName())));
    }

    @Override
    public void onFile(String sender, String fileName, byte[] data) {
        Platform.runLater(() -> {
            try {
                File dich = new File(thuMucLuu, fileName);
                int i = 1;
                while (dich.exists()) {               // tránh ghi đè file trùng tên
                    String ten = fileName, duoi = "";
                    int cham = fileName.lastIndexOf('.');
                    if (cham > 0) {
                        ten = fileName.substring(0, cham);
                        duoi = fileName.substring(cham);
                    }
                    dich = new File(thuMucLuu, ten + "(" + (i++) + ")" + duoi);
                }
                Files.write(dich.toPath(), data);
                themBongBongFile(sender, fileName, data.length, dich, false);
                themThongBao("Đã lưu vào: " + dich.getAbsolutePath());
            } catch (IOException e) {
                themThongBao("Lưu file thất bại: " + e.getMessage());
            }
        });
    }

    @Override
    public void onSystem(String content) {
        Platform.runLater(() -> themThongBao(content));
    }

    @Override
    public void onUsers(List<String> users) {
        Platform.runLater(() -> {
            danhSachOnline.getItems().setAll(users);
            lblTieuDe.setText("Phòng chat chung  ·  " + users.size() + " người online");
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> themThongBao("Đã mất kết nối tới server."));
    }

    // ==================================================================
    //  Vẽ bong bóng tin nhắn
    // ==================================================================

    private void themBongBong(String nguoiGui, String noiDung, boolean laCuaToi) {
        Label lblNoiDung = new Label(noiDung);
        lblNoiDung.setWrapText(true);
        lblNoiDung.setMaxWidth(400);
        lblNoiDung.getStyleClass().add(laCuaToi ? "noi-dung-toi" : "noi-dung-ho");

        Label lblInfo = new Label((laCuaToi ? "Bạn" : nguoiGui) + "  ·  "
                + LocalTime.now().format(gio));
        lblInfo.getStyleClass().add(laCuaToi ? "info-toi" : "info-ho");

        VBox bong = new VBox(3, lblInfo, lblNoiDung);
        bong.setPadding(new Insets(8, 12, 8, 12));
        bong.getStyleClass().add(laCuaToi ? "bong-toi" : "bong-ho");

        HBox dong = new HBox(bong);
        dong.setAlignment(laCuaToi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        khungTinNhan.getChildren().add(dong);
    }

    private void themBongBongFile(String nguoiGui, String tenFile, long kichThuoc,
                                  File fileTrenMay, boolean laCuaToi) {
        Label lblInfo = new Label((laCuaToi ? "Bạn đã gửi file" : nguoiGui + " đã gửi file")
                + "  ·  " + LocalTime.now().format(gio));
        lblInfo.getStyleClass().add(laCuaToi ? "info-toi" : "info-ho");

        Label lblTenFile = new Label("📎 " + tenFile + "  ("
                + Protocol.formatSize(kichThuoc) + ")");
        lblTenFile.setWrapText(true);
        lblTenFile.setMaxWidth(340);
        lblTenFile.getStyleClass().add(laCuaToi ? "ten-file-toi" : "ten-file-ho");

        Button btnMo = new Button(laCuaToi ? "Mở file" : "Mở thư mục");
        btnMo.getStyleClass().add("btn-nho");
        btnMo.setOnAction(e -> moTrenMay(laCuaToi ? fileTrenMay : fileTrenMay.getParentFile()));

        VBox bong = new VBox(4, lblInfo, lblTenFile, btnMo);
        bong.setPadding(new Insets(8, 12, 10, 12));
        bong.getStyleClass().add(laCuaToi ? "bong-toi" : "bong-ho");

        HBox dong = new HBox(bong);
        dong.setAlignment(laCuaToi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        khungTinNhan.getChildren().add(dong);
    }

    private void themThongBao(String noiDung) {
        Label lbl = new Label(noiDung);
        lbl.setWrapText(true);
        lbl.getStyleClass().add("thong-bao");

        HBox dong = new HBox(lbl);
        dong.setAlignment(Pos.CENTER);
        khungTinNhan.getChildren().add(dong);
    }

    private void moTrenMay(File f) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
        } catch (Exception e) {
            themThongBao("Không mở được: " + f.getAbsolutePath());
        }
    }
}