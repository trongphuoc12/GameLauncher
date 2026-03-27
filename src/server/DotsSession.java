package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import server.GameServer;
import client.DotsBoard;

public class DotsSession extends Thread {
    private Socket s1, s2;
    private PrintWriter out1, out2;
    private BufferedReader in1, in2;
    
    private DotsBoard board;
    private int currentPlayer = 1;
    
    // Biến trạng thái
    private boolean p1Ready = false, p2Ready = false;
    private boolean gameEnded = false;
    
    private String p1Name, p2Name;

    public DotsSession(Socket s1, String n1, Socket s2, String n2) {
        this.s1 = s1; this.p1Name = n1;
        this.s2 = s2; this.p2Name = n2;
        this.board = new DotsBoard();
    }

    @Override
    public void run() {
        try {
            out1 = new PrintWriter(s1.getOutputStream(), true);
            in1 = new BufferedReader(new InputStreamReader(s1.getInputStream()));
            out2 = new PrintWriter(s2.getOutputStream(), true);
            in2 = new BufferedReader(new InputStreamReader(s2.getInputStream()));

            out1.println("START 1"); out2.println("START 2");
            
            String namesCmd = "UPDATE_NAMES " + p1Name + "," + p2Name;
            out1.println(namesCmd);
            out2.println(namesCmd);

            out1.println("MESSAGE Vui lòng nhấn Sẵn Sàng...");
            out2.println("MESSAGE Vui lòng nhấn Sẵn Sàng...");

            new Thread(() -> listenToClient(1, in1)).start();
            new Thread(() -> listenToClient(2, in2)).start();

        } catch (IOException e) { e.printStackTrace(); }
    }

    private void listenToClient(int playerID, BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("CHAT ")) {
                    String msg = line.substring(5);
                    String senderName = (playerID == 1) ? p1Name : p2Name;
                    sendToBoth("CHAT_MSG " + senderName + ": " + msg);
                }
                else if (line.equals("QUIT")) { handleDisconnect(playerID); break; }
                else if (line.equals("READY")) { handleReady(playerID); }
                else if (line.startsWith("MOVE")) { processMove(playerID, line); }
                
                // ===> XỬ LÝ CHƠI LẠI <===
                else if (line.equals("REMATCH_REQUEST")) {
                    handleRematchRequest(playerID);
                }
                else if (line.equals("REMATCH_REJECT")) { // Xử lý khi từ chối
                    handleRematchReject(playerID);
                }
            }
        } catch (IOException e) { handleDisconnect(playerID); }
    }

    private synchronized void handleRematchRequest(int playerID) {
        if (!gameEnded) return; // Chỉ cho phép khi game đã kết thúc

        if (playerID == 1) {
            p1Ready = true;
            // Gửi lệnh OFFER để bên kia hiện bảng Dialog
            if(out2 != null) out2.println("REMATCH_OFFER");
        } else {
            p2Ready = true;
            if(out1 != null) out1.println("REMATCH_OFFER");
        }
        
        checkRematchStart();
    }
    
    // Hàm xử lý chơi lại (ĐÃ SỬA ĐỂ CÓ THÔNG BÁO)
    private synchronized void handleRematchReject(int playerID) {
        if (playerID == 1) {
            if(out2 != null) out2.println("REMATCH_DECLINED");
        } else {
            if(out1 != null) out1.println("REMATCH_DECLINED");
        }
        // Từ chối xong thì coi như thoát game
        handleDisconnect(playerID);
    }

    private void checkRematchStart() {
        if (p1Ready && p2Ready) {
            board = new DotsBoard();
            gameEnded = false;
            p1Ready = false;
            p2Ready = false;
            
            sendToBoth("RESTART");
            sendToBoth("GAME_BEGIN");
            
            currentPlayer = 1;
            out1.println("TURN 1");
            out2.println("TURN 1");
        }
    }

    private void sendToBoth(String msg) {
        if(out1 != null) out1.println(msg);
        if(out2 != null) out2.println(msg);
    }

    private synchronized void handleDisconnect(int leaverID) {
        if (gameEnded) return;
        System.out.println("Player " + leaverID + " đã thoát.");
        try {
            String winnerName = "";
            if (leaverID == 1) {
                if (out2 != null) out2.println("OPPONENT_LEFT");
                winnerName = p2Name;
            } else {
                if (out1 != null) out1.println("OPPONENT_LEFT");
                winnerName = p1Name;
            }
            if (!gameEnded) GameServer.recordWin(winnerName);
            
            if (s1 != null && !s1.isClosed()) s1.close();
            if (s2 != null && !s2.isClosed()) s2.close();
        } catch (Exception e) {}
    }
    
    private synchronized void handleReady(int playerID) {
        if (playerID == 1) p1Ready = true; else p2Ready = true;
        
        if (playerID == 1) {
            out1.println("MESSAGE Đang chờ đối thủ...");
            out2.println("MESSAGE Đối thủ đã sẵn sàng!");
        } else {
            out2.println("MESSAGE Đang chờ đối thủ...");
            out1.println("MESSAGE Đối thủ đã sẵn sàng!");
        }
        
        if (p1Ready && p2Ready) {
            p1Ready = false; p2Ready = false;
            out1.println("GAME_BEGIN"); out2.println("GAME_BEGIN");
            out1.println("TURN 1"); out2.println("TURN 1");
        }
    }

    private synchronized void processMove(int playerID, String command) {
        if (gameEnded) return;
        if (playerID != currentPlayer) return;

        String[] parts = command.split(" ");
        String type = parts[1];
        int r = Integer.parseInt(parts[2]);
        int c = Integer.parseInt(parts[3]);

        boolean valid = (type.equals("H")) ? board.addHLine(r, c) : board.addVLine(r, c);

        if (valid) {
            boolean scored = board.checkBoxes(playerID);
            // Gửi cờ hiệu S (Score) hoặc N (Normal) để Client phát âm thanh
            String flag = scored ? "S" : "N";
            String updateCmd = "UPDATE " + type + " " + r + " " + c + " " + flag;
            
            out1.println(updateCmd);
            out2.println(updateCmd);

            if (scored) {
                broadcastBoxes();
                if (board.isFull()) {
                    out1.println("GAMEOVER");
                    out2.println("GAMEOVER");
                    gameEnded = true;
                    checkAndRecordWinner();
                    return;
                }
                out1.println("TURN " + currentPlayer);
                out2.println("TURN " + currentPlayer);
            } else {
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
                out1.println("TURN " + currentPlayer);
                out2.println("TURN " + currentPlayer);
            }
        }
    }
    
    private void checkAndRecordWinner() {
        int s1 = board.getP1Score();
        int s2 = board.getP2Score();
        if (s1 > s2) GameServer.recordWin(p1Name);
        else if (s2 > s1) GameServer.recordWin(p2Name);
    }
    
    private void broadcastBoxes() {
        for(int i=0; i<board.ROWS; i++) {
            for(int j=0; j<board.COLS; j++) {
                int owner = board.getBoxOwner(i, j);
                if (owner != 0) {
                    String cmd = "BOX " + i + " " + j + " " + owner;
                    out1.println(cmd); out2.println(cmd);
                }
            }
        }
    }
}