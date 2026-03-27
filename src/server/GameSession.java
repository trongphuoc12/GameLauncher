package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import server.GameServer;
import client.GameBoard;
import client.Ship;

public class GameSession extends Thread {

    private Socket socket1, socket2;
    private PrintWriter out1, out2;

    private GameBoard board1, board2;
    private boolean player1Ready = false, player2Ready = false;
    private int currentPlayer = 1;

    private String p1Name, p2Name;

    private enum GameState {
        SETUP, PLAYING, POST_GAME
    }
    private GameState currentState = GameState.SETUP;

    private Timer turnTimer;
    private int p1MissedTurns = 0; // Số lần P1 mất lượt liên tiếp
    private int p2MissedTurns = 0; // Số lần P2 mất lượt liên tiếp
    private final int TIME_LIMIT = 30; // 30 giây

    public GameSession(Socket s1, String n1, Socket s2, String n2) {
        this.socket1 = s1;
        this.p1Name = n1;
        this.socket2 = s2;
        this.p2Name = n2;
        this.board1 = new GameBoard();
        this.board2 = new GameBoard();

        new PlayerHandler(socket1, this, 1).start();
        new PlayerHandler(socket2, this, 2).start();
    }

    public synchronized void registerPlayer(int playerNum, PrintWriter out) {
        if (playerNum == 1) {
            this.out1 = out;
        } else {
            this.out2 = out;
        }
        if (out1 != null && out2 != null) {
            sendToBoth("UPDATE_NAMES " + p1Name + "," + p2Name);
            out1.println("START");
            out2.println("START");
        }
    }

    public synchronized void processChat(int playerNum, String msg) {
        String senderName = (playerNum == 1) ? p1Name : p2Name;
        sendToBoth("CHAT_MSG " + senderName + ": " + msg);
    }

    public synchronized void processDisconnect(int playerNum) {
        stopTurnTimer(); // Dừng giờ nếu có ai thoát
        if (playerNum == 1) {
            if (out2 != null) {
                out2.println("OPPONENT_LEFT");

            }
        } else {
            if (out1 != null) {
                out1.println("OPPONENT_LEFT");

            }
        }
        try {
            if (socket1 != null && !socket1.isClosed()) {
                socket1.close();
            }
        } catch (IOException e) {
        }

        try {
            if (socket2 != null && !socket2.isClosed()) {
                socket2.close();
            }
        } catch (IOException e) {
        }
    }

    public synchronized void processShipData(int playerNum, String fullCommand) {
        if (currentState != GameState.SETUP) {
            return;
        }

        if (playerNum == 1) {
            processShipDataLogic(fullCommand, board1);
            player1Ready = true;
            if (out2 != null) {
                out2.println("SERVER_STATUS " + p1Name + " đã sẵn sàng!");
            }
        } else {
            processShipDataLogic(fullCommand, board2);
            player2Ready = true;
            if (out1 != null) {
                out1.println("SERVER_STATUS " + p2Name + " đã sẵn sàng!");
            }
        }

        if (player1Ready && player2Ready) {
            sendToBoth("GAME_BEGIN");
            currentPlayer = 1;

            // --- BẮT ĐẦU TÍNH GIỜ CHO P1 ---
            startTurnTimer(1);
            // -------------------------------

            out1.println("TURN your_turn");
            out2.println("TURN opponent_turn");
            currentState = GameState.PLAYING;
        }
    }

    public synchronized void processFire(int playerNum, String fireCommand) {
        if (currentState != GameState.PLAYING || playerNum != currentPlayer) {
            return;
        }

        // --- NGƯỜI CHƠI ĐÃ BẮN -> DỪNG ĐỒNG HỒ CŨ ---
        stopTurnTimer();
        // Reset lỗi vi phạm của người này vì họ đã đi đúng luật
        if (playerNum == 1) {
            p1MissedTurns = 0;
        } else {
            p2MissedTurns = 0;
        }
        // -------------------------------------------

        PrintWriter currentOut = (currentPlayer == 1) ? out1 : out2;
        PrintWriter opponentOut = (currentPlayer == 1) ? out2 : out1;
        GameBoard opponentBoard = (currentPlayer == 1) ? board2 : board1;

        int[] coords = parseFireCommand(fireCommand);
        int result = opponentBoard.receiveFire(coords[0], coords[1]);
        String strResult = (result > 0) ? "HIT" : "MISS";

        currentOut.println("UPDATE opponent " + coords[0] + " " + coords[1] + " " + strResult);
        opponentOut.println("UPDATE self " + coords[0] + " " + coords[1] + " " + strResult);

        if (result == 2) {
            Ship sunkShip = opponentBoard.findShipAt(coords[0], coords[1]);
            if (sunkShip != null) {
                currentOut.println("SINK " + sunkShip.name + " " + sunkShip.getProtocolString());
                opponentOut.println("SINK_SELF " + sunkShip.name + " " + sunkShip.getProtocolString());
            }
        }

        if (opponentBoard.hasLost()) {
            stopTurnTimer(); // Hết game thì tắt hẳn
            currentOut.println("WIN");
            opponentOut.println("LOSE");
            String winnerName = (currentPlayer == 1) ? p1Name : p2Name;
            GameServer.recordWin(winnerName);
            currentState = GameState.POST_GAME;
            return;
        }

        if (result == 0) { // MISS -> Đổi lượt
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
            startTurnTimer(currentPlayer); // BẮT ĐẦU GIỜ CHO NGƯỜI KIA
            currentOut.println("TURN opponent_turn");
            opponentOut.println("TURN your_turn");
        } else { // HIT -> Bắn tiếp
            startTurnTimer(currentPlayer); // BẮT ĐẦU LẠI GIỜ CHO CHÍNH MÌNH
            currentOut.println("TURN your_turn");
            opponentOut.println("TURN opponent_turn");
        }
    }

    // --- CÁC HÀM XỬ LÝ THỜI GIAN (LOGIC MỚI) ---
    private void startTurnTimer(int playerID) {
        stopTurnTimer(); // Dọn dẹp timer cũ trước

        turnTimer = new Timer();
        turnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handleTimeout(playerID);
            }
        }, TIME_LIMIT * 1000); // 30 giây
    }

    private void stopTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
    }

    private synchronized void handleTimeout(int playerID) {
        // Xử lý khi hết giờ
        System.out.println("Player " + playerID + " hết giờ!");

        if (playerID == 1) {
            p1MissedTurns++;
            out1.println("SERVER_STATUS ⚠️ BẠN ĐÃ HẾT GIỜ! MẤT LƯỢT!");
            out2.println("SERVER_STATUS Đối thủ hết giờ. Đến lượt bạn!");
        } else {
            p2MissedTurns++;
            out2.println("SERVER_STATUS ⚠️ BẠN ĐÃ HẾT GIỜ! MẤT LƯỢT!");
            out1.println("SERVER_STATUS Đối thủ hết giờ. Đến lượt bạn!");
        }

        // Kiểm tra xử thua (2 lần liên tiếp)
        if ((playerID == 1 && p1MissedTurns >= 2) || (playerID == 2 && p2MissedTurns >= 2)) {
            stopTurnTimer();
            String winnerName = "";
            if (playerID == 1) {
                out1.println("LOSE");
                out2.println("WIN"); // P1 thua
            } else {
                out2.println("LOSE");
                out1.println("WIN"); // P2 thua
            }
            GameServer.recordWin(winnerName);
            currentState = GameState.POST_GAME;
            return;
        }

        // Chuyển lượt cưỡng bức
        currentPlayer = (currentPlayer == 1) ? 2 : 1;

        if (currentPlayer == 1) {
            out1.println("TURN your_turn");
            out2.println("TURN opponent_turn");
        } else {
            out2.println("TURN your_turn");
            out1.println("TURN opponent_turn");
        }

        // Bắt đầu tính giờ cho người mới
        startTurnTimer(currentPlayer);
    }
    // -------------------------------------------

    public synchronized void processRematchRequest(int playerNum) {
        if (currentState != GameState.POST_GAME) {
            return;
        }
        if (playerNum == 1) {
            player1Ready = true;
            if (out2 != null) {
                out2.println("REMATCH_OFFER");
            }
        } else {
            player2Ready = true;
            if (out1 != null) {
                out1.println("REMATCH_OFFER");
            }
        }

        if (player1Ready && player2Ready) {
            board1 = new GameBoard();
            board2 = new GameBoard();
            player1Ready = false;
            player2Ready = false;
            p1MissedTurns = 0;
            p2MissedTurns = 0; // Reset lỗi phạt
            currentState = GameState.SETUP;
            sendToBoth("RESTART");
        }
    }

    public synchronized void processRematchReject(int playerNum) {
        if (playerNum == 1) {
            if (out2 != null) {
                out2.println("REMATCH_DECLINED");

            }
        } else {
            if (out1 != null) {
                out1.println("REMATCH_DECLINED");

            }
        }
        processDisconnect(playerNum);
    }

    private void sendToBoth(String msg) {
        if (out1 != null) {
            out1.println(msg);
        }
        if (out2 != null) {
            out2.println(msg);
        }
    }

    private void processShipDataLogic(String fullCommand, GameBoard board) {
        if (!fullCommand.startsWith("SHIP_DATA ")) {
            return;
        }
        String data = fullCommand.substring(10);
        String[] ships = data.split(";");
        for (String ship : ships) {
            String[] parts = ship.split(",");
            try {
                int length = Integer.parseInt(parts[0]);
                boolean isHorizontal = parts[1].equals("H");
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                board.placeShip(x, y, length, isHorizontal);
            } catch (Exception e) {
            }
        }
    }

    private int[] parseFireCommand(String command) {
        String[] parts = command.split(" ");
        return new int[]{Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
    }
}
