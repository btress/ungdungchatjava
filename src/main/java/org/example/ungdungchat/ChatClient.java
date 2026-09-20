package org.example.ungdungchat;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Phần MẠNG của client (không dính gì tới JavaFX).
 * Controller chỉ cần implement Listener để nhận sự kiện.
 */
public class ChatClient {

    public interface Listener {
        void onMessage(String sender, String content);
        void onFile(String sender, String fileName, byte[] data);
        void onSystem(String content);
        void onUsers(List<String> users);
        void onDisconnected();
    }

    private final String host;
    private final int port;
    private final String userName;
    private Listener listener;

    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private volatile boolean running = false;

    public ChatClient(String host, int port, String userName) {
        this.host = host;
        this.port = port;
        this.userName = userName;
    }

    public void setListener(Listener listener) { this.listener = listener; }

    public String getUserName() { return userName; }

    /** Kết nối tới server và gửi gói JOIN. Ném IOException nếu không kết nối được. */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        os = new DataOutputStream(socket.getOutputStream());
        is = new DataInputStream(socket.getInputStream());

        os.writeUTF(Protocol.JOIN);
        os.writeUTF(userName);
        os.flush();
    }

    /** Bắt đầu thread nhận dữ liệu (gọi sau khi giao diện chat đã sẵn sàng) */
    public void start() {
        running = true;
        Thread receiveThread = new Thread(this::receiveLoop, "receive-thread");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    private void receiveLoop() {
        try {
            while (running) {
                String type = is.readUTF();

                if (Protocol.MSG.equals(type)) {
                    String sender = is.readUTF();
                    String content = is.readUTF();
                    listener.onMessage(sender, content);

                } else if (Protocol.SYS.equals(type)) {
                    listener.onSystem(is.readUTF());

                } else if (Protocol.USERS.equals(type)) {
                    int n = is.readInt();
                    List<String> users = new ArrayList<>();
                    for (int i = 0; i < n; i++) users.add(is.readUTF());
                    listener.onUsers(users);

                } else if (Protocol.FILE.equals(type)) {
                    String sender = is.readUTF();
                    String fileName = is.readUTF();
                    int len = is.readInt();
                    byte[] data = new byte[len];
                    is.readFully(data);
                    listener.onFile(sender, fileName, data);
                }
            }
        } catch (IOException e) {
            // mất kết nối
        } finally {
            running = false;
            if (listener != null) listener.onDisconnected();
        }
    }

    // ------------------------------------------------------------------
    //  Gửi dữ liệu
    // ------------------------------------------------------------------

    public void sendMessage(String tinnhan) throws IOException {
        synchronized (os) {
            os.writeUTF(Protocol.MSG);
            os.writeUTF(tinnhan);
            os.flush();
        }
    }

    public void sendFile(File file) throws IOException {
        if (file.length() > Protocol.MAX_FILE_SIZE) {
            throw new IOException("File quá lớn (tối đa "
                    + Protocol.formatSize(Protocol.MAX_FILE_SIZE) + ")");
        }
        byte[] data = Files.readAllBytes(file.toPath());
        synchronized (os) {
            os.writeUTF(Protocol.FILE);
            os.writeUTF(file.getName());
            os.writeInt(data.length);
            os.write(data);
            os.flush();
        }
    }

    public void close() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
    }
}