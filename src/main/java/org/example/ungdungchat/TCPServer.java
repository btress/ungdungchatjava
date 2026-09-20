package org.example.ungdungchat;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class TCPServer {

    private final List<ClientHandler> clients = new ArrayList<>();

    public void run() {
        try {
            ServerSocket ss = new ServerSocket(Protocol.PORT);
            System.out.println("=== Server Started - port " + Protocol.PORT + " ===");
            System.out.println("Gõ /users để xem danh sách, /quit để tắt server.");

            Thread consoleThread = new Thread(this::doConsole);
            consoleThread.setDaemon(true);
            consoleThread.start();

            while (true) {
                Socket socket = ss.accept();
                new ClientHandler(socket).start();
            }

        } catch (IOException e) {
            System.out.println("Lỗi server: " + e);
        }
    }

    private void doConsole() {
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String cmd = sc.nextLine().trim();
            if (cmd.equalsIgnoreCase("/quit")) {
                System.out.println("Server đang tắt...");
                System.exit(0);
            } else if (cmd.equalsIgnoreCase("/users")) {
                System.out.println("Đang online: " + getUserNames());
            } else if (!cmd.isEmpty()) {
                broadcastSystem("[Server] " + cmd);
            }
        }
    }

    // ------------------------------------------------------------------
    //  Các hàm broadcast
    // ------------------------------------------------------------------

    private synchronized List<String> getUserNames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler c : clients) names.add(c.name);
        return names;
    }

    private synchronized List<ClientHandler> snapshot() {
        return new ArrayList<>(clients);
    }

    private void broadcastMessage(String sender, String content) {
        for (ClientHandler c : snapshot()) c.sendMessage(sender, content);
        System.out.println(sender + ": " + content);
    }

    private void broadcastSystem(String content) {
        for (ClientHandler c : snapshot()) c.sendSystem(content);
        System.out.println("[SYS] " + content);
    }

    /** Gửi file cho tất cả, trừ người gửi */
    private void broadcastFile(ClientHandler from, String fileName, byte[] data) {
        for (ClientHandler c : snapshot()) {
            if (c != from) c.sendFile(from.name, fileName, data);
        }
        System.out.println(from.name + " gửi file: " + fileName
                + " (" + Protocol.formatSize(data.length) + ")");
    }

    private void broadcastUserList() {
        List<String> names = getUserNames();
        for (ClientHandler c : snapshot()) c.sendUsers(names);
    }

    /** Nếu tên bị trùng thì thêm số vào sau: Nam, Nam(2), Nam(3)... */
    private synchronized String makeUniqueName(String raw) {
        String base = (raw == null || raw.trim().isEmpty()) ? "User" : raw.trim();
        String name = base;
        int i = 2;
        boolean trung = true;
        while (trung) {
            trung = false;
            for (ClientHandler c : clients) {
                if (c.name != null && c.name.equalsIgnoreCase(name)) { trung = true; break; }
            }
            if (trung) name = base + "(" + (i++) + ")";
        }
        return name;
    }

    // ------------------------------------------------------------------
    //  Mỗi client 1 thread
    // ------------------------------------------------------------------
    private class ClientHandler extends Thread {

        private final Socket socket;
        private DataInputStream is;
        private DataOutputStream os;
        private String name;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                is = new DataInputStream(socket.getInputStream());
                os = new DataOutputStream(socket.getOutputStream());

                // Gói tin đầu tiên bắt buộc là JOIN + tên
                String type = is.readUTF();
                if (!Protocol.JOIN.equals(type)) {
                    socket.close();
                    return;
                }
                name = makeUniqueName(is.readUTF());

                synchronized (TCPServer.this) { clients.add(this); }
                sendSystem("Bạn đã vào phòng với tên: " + name);
                broadcastSystem(name + " đã tham gia phòng chat.");
                broadcastUserList();

                // Vòng lặp nhận dữ liệu
                while (true) {
                    String t = is.readUTF();

                    if (Protocol.MSG.equals(t)) {
                        String tinnhan = is.readUTF();
                        broadcastMessage(name, tinnhan);

                    } else if (Protocol.FILE.equals(t)) {
                        String fileName = is.readUTF();
                        int len = is.readInt();
                        if (len < 0 || len > Protocol.MAX_FILE_SIZE) {
                            throw new IOException("Kích thước file không hợp lệ: " + len);
                        }
                        byte[] data = new byte[len];
                        is.readFully(data);          // đọc đủ số byte mới thôi
                        broadcastFile(this, fileName, data);

                    } else {
                        System.out.println("Gói tin lạ: " + t);
                    }
                }

            } catch (IOException e) {
                // client ngắt kết nối -> kết thúc thread
            } finally {
                close();
            }
        }

        // Mọi hàm gửi đều đồng bộ trên os để 2 thread không ghi chồng nhau
        void sendMessage(String sender, String content) {
            try {
                synchronized (os) {
                    os.writeUTF(Protocol.MSG);
                    os.writeUTF(sender);
                    os.writeUTF(content);
                    os.flush();
                }
            } catch (IOException e) { close(); }
        }

        void sendSystem(String content) {
            try {
                synchronized (os) {
                    os.writeUTF(Protocol.SYS);
                    os.writeUTF(content);
                    os.flush();
                }
            } catch (IOException e) { close(); }
        }

        void sendFile(String sender, String fileName, byte[] data) {
            try {
                synchronized (os) {
                    os.writeUTF(Protocol.FILE);
                    os.writeUTF(sender);
                    os.writeUTF(fileName);
                    os.writeInt(data.length);
                    os.write(data);
                    os.flush();
                }
            } catch (IOException e) { close(); }
        }

        void sendUsers(List<String> names) {
            try {
                synchronized (os) {
                    os.writeUTF(Protocol.USERS);
                    os.writeInt(names.size());
                    for (String n : names) os.writeUTF(n);
                    os.flush();
                }
            } catch (IOException e) { close(); }
        }

        private void close() {
            boolean removed;
            synchronized (TCPServer.this) { removed = clients.remove(this); }
            try { socket.close(); } catch (IOException ignored) { }
            if (removed && name != null) {
                broadcastSystem(name + " đã rời phòng chat.");
                broadcastUserList();
            }
        }
    }

    public static void main(String[] args) {
        TCPServer s = new TCPServer();
        s.run();
    }
}