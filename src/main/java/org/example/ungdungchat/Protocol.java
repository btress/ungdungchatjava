package org.example.ungdungchat;



/**
 * Giao thức dùng chung giữa Client và Server.
 *
 * Mỗi gói tin bắt đầu bằng 1 writeUTF cho biết "loại gói tin",
 * sau đó là các trường dữ liệu tương ứng:
 *
 *  JOIN  : writeUTF(tên)
 *  MSG   : writeUTF(người gửi) + writeUTF(nội dung)
 *  FILE  : writeUTF(người gửi) + writeUTF(tên file) + writeInt(độ dài) + write(byte[])
 *  USERS : writeInt(n) + n * writeUTF(tên)
 *  SYS   : writeUTF(nội dung)      // thông báo hệ thống
 */
public class Protocol {

    public static final int PORT = 8888;

    public static final String JOIN  = "JOIN";
    public static final String MSG   = "MSG";
    public static final String FILE  = "FILE";
    public static final String USERS = "USERS";
    public static final String SYS   = "SYS";

    /** Giới hạn kích thước file: 50 MB */
    public static final int MAX_FILE_SIZE = 50 * 1024 * 1024;

    /** Đổi số byte sang dạng dễ đọc: 1.2 MB, 340 KB... */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}