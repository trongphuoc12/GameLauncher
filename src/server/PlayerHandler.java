package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PlayerHandler extends Thread {
    private Socket socket;
    private GameSession session;
    private int playerNum; // Sẽ là 1 hoặc 2
    private PrintWriter out;
    private BufferedReader in;

    public PlayerHandler(Socket socket, GameSession session, int playerNum) {
        this.socket = socket;
        this.session = session;
        this.playerNum = playerNum;
        
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Báo cho GameSession biết "đôi tai" này đã sẵn sàng
            session.registerPlayer(playerNum, this.out);
            
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo PlayerHandler: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String message;
            // Luôn lắng nghe tin nhắn từ Client này
            while ((message = in.readLine()) != null) {
                
                // Phân loại tin nhắn và báo cho "Trọng tài" (GameSession)
                if (message.startsWith("SHIP_DATA")) {
                    session.processShipData(playerNum, message);
                } 
                else if (message.startsWith("FIRE")) {
                    session.processFire(playerNum, message);
                }
                else if (message.equals("REMATCH_REQUEST")) {
                    session.processRematchRequest(playerNum);
                }
                else if (message.equals("REMATCH_REJECT")) {
                    session.processRematchReject(playerNum);
                }
                else if (message.startsWith("CHAT ")) {
                    session.processChat(playerNum, message.substring(5));
                }
            }
        } catch (IOException e) {
            // Xử lý khi client ngắt kết nối
            System.out.println("Người chơi " + playerNum + " đã ngắt kết nối.");
        } finally {
            // Báo cho "Trọng tài" biết client này đã rời đi
            session.processDisconnect(playerNum);
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }
}