package database;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DatabaseManager {

    // ===> DÁN URL BẠN VỪA COPY VÀO ĐÂY <===
    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwhb_l9CoeKG2e9HqbR60gPqIzTYQXhAK7ky2eAKHaIZoIrDNf9wZ7Xv71M-HMLyvdzfw/exec";
    // =======================================

    // Không cần khởi tạo gì cả vì Google lo hết rồi
    public static void initialize() {
        System.out.println("Đang sử dụng Database Online (Google Sheets)");
    }

    // Gửi yêu cầu cộng điểm (POST)
    public static void addWin(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return;
        if (playerName.equals("Unknown") || playerName.equals("Player")) return;

        new Thread(() -> {
            try {
                // Mã hóa tên để tránh lỗi tiếng Việt (Ví dụ: "Vũ" -> "V%C5%A9")
                String encodedName = URLEncoder.encode(playerName, StandardCharsets.UTF_8.toString());
                
                URL url = new URL(SCRIPT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                
                // Gửi dữ liệu: name=TenNguoiChoi
                String params = "name=" + encodedName;
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = params.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                // Đọc phản hồi để chắc chắn lệnh đã chạy
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    System.out.println("[CLOUD] Đã lưu điểm cho: " + playerName);
                }
                
            } catch (Exception e) {
                System.err.println("Lỗi kết nối Google Sheets: " + e.getMessage());
            }
        }).start();
    }

    // Lấy danh sách Top (GET)
    public static String getTop5() {
        try {
            URL url = new URL(SCRIPT_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            // Đọc dữ liệu trả về
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            
            // Google Script trả về chuỗi: "Tuan:5,Nam:3"
            return content.toString();
            
        } catch (Exception e) {
            System.err.println("Lỗi lấy BXH: " + e.getMessage());
            return "Lỗi mạng";
        }
    }
}