//FILE GameServer.java
package server;

import database.DatabaseManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {

    static class WaitingPlayer {

        Socket socket;
        String name;

        public WaitingPlayer(Socket s, String n) {
            this.socket = s;
            this.name = n;
        }
    }

    private static ConcurrentHashMap<String, WaitingPlayer> waitingList = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        int port = 12345;

        ServerSocket server = new ServerSocket(port);
        System.out.println("Battleship Server đang chạy tại cổng " + port + "...");

        while (true) {
            try {
                Socket socket = server.accept();
                new Thread(() -> handleRequest(socket)).start();
            } catch (Exception e) {
                System.err.println("Lỗi kết nối: " + e.getMessage());
            }
        }
    }

    // Hàm này được gọi từ GameSession và DotsSession
    public static void recordWin(String winnerName) {
        DatabaseManager.addWin(winnerName);
    }

    private static void handleRequest(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String request = in.readLine();
            if (request == null) {
                return;
            }

            if (request.equals("VIEW_TOP")) {

                String topData = DatabaseManager.getTop5();
                if (topData.isEmpty()) {
                    topData = "Chưa có dữ liệu";
                }
                out.println("TOP_DATA " + topData);

                socket.close();
                return;
            }

            String[] parts = request.split(" ", 3);
            String cmd = parts[0];
            String roomID = parts.length > 1 ? parts[1] : "";
            String playerName = parts.length > 2 ? parts[2] : "Unknown";

            if (cmd.equals("CREATE_ROOM")) {
                synchronized (waitingList) {
                    if (waitingList.containsKey(roomID)) {
                        out.println("LOGIN_ERROR Mã phòng " + roomID + " đã tồn tại!");
                        socket.close();
                    } else {
                        waitingList.put(roomID, new WaitingPlayer(socket, playerName));
                        out.println("MESSAGE Bạn (" + playerName + ") là chủ phòng " + roomID + ". Đang chờ...");
                    }
                }
            } else if (cmd.equals("JOIN_ROOM")) {
                synchronized (waitingList) {
                    if (waitingList.containsKey(roomID)) {
                        WaitingPlayer p1 = waitingList.remove(roomID);
                        if (p1.socket.isClosed()) {
                            waitingList.put(roomID, new WaitingPlayer(socket, playerName));
                            out.println("MESSAGE Chủ phòng đã thoát. Bạn thành chủ phòng " + roomID);
                        } else {
                            System.out.println("Phòng " + roomID + ": Ghép cặp (" + p1.name + " vs " + playerName + ")");
                            PrintWriter out1 = new PrintWriter(p1.socket.getOutputStream(), true);
                            out1.println("SERVER_STATUS " + playerName + " đã vào phòng!");
                            out.println("SERVER_STATUS Kết nối thành công với " + p1.name + "!");
                            new GameSession(p1.socket, p1.name, socket, playerName);
                        }
                    } else {
                        out.println("LOGIN_ERROR Không tìm thấy phòng " + roomID);
                        socket.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
