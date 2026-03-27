package client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class GameClient extends JFrame {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String myName;
    private String myRoomID;
    private boolean isCreating;

    // Components
    private JButton[][] myGridButtons = new JButton[10][10];
    private JButton[][] opponentGridButtons = new JButton[10][10];
    private JPanel mainGridContainer;
    private CardLayout cardLayout;

    private ModernButton btnReady, btnRandom, btnPlayAgain;
    private JPanel setupPanel, endGamePanel;
    private JLabel turnLabel;
    private JPanel captainPanel;

    // --- ĐỒNG HỒ ĐẾM NGƯỢC ---
    private JLabel lblTimer;
    private Timer clientTimer;
    private int timeLeft = 30; // Giây
    // -------------------------

    private SoundManager soundManager;
    private ImageIcon iconWater, iconHit, iconMiss;
    private ImageIcon iconShip5, iconShip4, iconShip3, iconShip2;
    private Image backgroundImage;
    private ImageIcon captainIcon;
    private ImageIcon iconWater, iconHit, iconMiss, iconSunkX;

    private GameBoard localGameBoard;
    private List<Ship> ships;
    private Ship selectedShipToMove = null;
    private Random rand = new Random();

    public GameClient(String serverIP, String playerName, String roomID, boolean isCreating) {
        this.myName = playerName;
        this.myRoomID = roomID;
        this.isCreating = isCreating;

        loadImages();
        soundManager = new SoundManager();
        soundManager.playMusic("music_bs.wav");

        localGameBoard = new GameBoard();
        initShips();

        setupGUI();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cleanup();
            }
        });

        randomlyPlaceShipsAndDraw();
        connectToServer(serverIP);
    }

    private void initShips() {
        ships = new ArrayList<>();
        ships.add(new Ship("Carrier", 5));
        ships.add(new Ship("Battleship", 4));
        ships.add(new Ship("Cruiser", 3));
        ships.add(new Ship("Submarine", 3));
        ships.add(new Ship("Destroyer", 2));
    }

    // --- LOGIC ĐỒNG HỒ CLIENT ---
    private void startClientTimer() {
        if (clientTimer != null) {
            clientTimer.stop();
        }

        timeLeft = 30; // Reset về 30s
        lblTimer.setText(timeLeft + "s");
        lblTimer.setForeground(Color.WHITE);
        lblTimer.setVisible(true);

        clientTimer = new Timer(1000, e -> {
            timeLeft--;
            if (timeLeft <= 10) {
                lblTimer.setForeground(Color.RED); // Sắp hết giờ thì đỏ
            } else {
                lblTimer.setForeground(Color.WHITE);
            }

            lblTimer.setText(timeLeft + "s");

            if (timeLeft <= 0) {
                clientTimer.stop();
            }
        });
        clientTimer.start();
    }

    private void stopClientTimer() {
        if (clientTimer != null) {
            clientTimer.stop();
        }
        lblTimer.setVisible(false);
    }
    // ----------------------------

    private void setupGUI() {
        setTitle("Battleship - " + myName);
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        BackgroundPanel bgPanel = new BackgroundPanel();
        bgPanel.setLayout(new BorderLayout());
        setContentPane(bgPanel);

        // 1. HEADER (Thêm đồng hồ vào đây)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        turnLabel = new JLabel("GIAI ĐOẠN SẮP XẾP ĐỘI HÌNH", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        turnLabel.setForeground(new Color(255, 215, 0));
        turnLabel.setText("<html><span style='text-shadow: 2px 2px #000000;'>GIAI ĐOẠN SẮP XẾP ĐỘI HÌNH</span></html>");

        // Label Đồng hồ (Mới)
        lblTimer = new JLabel("30s", SwingConstants.CENTER);
        lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTimer.setForeground(Color.WHITE);
        lblTimer.setBorder(new EmptyBorder(5, 0, 0, 0));
        lblTimer.setVisible(false); // Ẩn lúc đầu

        // Panel chứa Tiêu đề + Đồng hồ
        JPanel centerHeader = new JPanel(new BorderLayout());
        centerHeader.setOpaque(false);
        centerHeader.add(turnLabel, BorderLayout.CENTER);
        centerHeader.add(lblTimer, BorderLayout.SOUTH);

        JLabel lblP1Info = new JLabel(myName + " (Bạn)", SwingConstants.LEFT);
        lblP1Info.setFont(new Font("Arial", Font.BOLD, 16));
        lblP1Info.setForeground(Color.GREEN);
        // JLabel lblP2Info = new JLabel("Đang chờ...", SwingConstants.RIGHT);
        // lblP2Info.setFont(new Font("Arial", Font.BOLD, 16));
        // lblP2Info.setForeground(Color.RED);

        headerPanel.add(lblP1Info, BorderLayout.WEST);
        headerPanel.add(centerHeader, BorderLayout.CENTER);
        // headerPanel.add(lblP2Info, BorderLayout.EAST);
        bgPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. CENTER
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20));
        centerWrapper.setOpaque(false);

        captainPanel = new JPanel();
        captainPanel.setPreferredSize(new Dimension(150, 300));
        captainPanel.setOpaque(false);
        captainPanel.setLayout(new BoxLayout(captainPanel, BoxLayout.Y_AXIS));
        JLabel lblAvatar = new JLabel();
        if (captainIcon != null) {
            lblAvatar.setIcon(captainIcon);
        }
        lblAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lblName = new JLabel("COMMANDER");
        lblName.setForeground(Color.WHITE);
        lblName.setFont(new Font("Arial", Font.BOLD, 16));
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
        captainPanel.add(lblAvatar);
        captainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        captainPanel.add(lblName);
        centerWrapper.add(captainPanel);

        cardLayout = new CardLayout();
        mainGridContainer = new JPanel(cardLayout);
        mainGridContainer.setOpaque(false);
        mainGridContainer.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0, 191, 255), 2), new EmptyBorder(5, 5, 5, 5)));

        JPanel myPanel = createTransparentGrid(myGridButtons, true);
        JPanel opponentPanel = createTransparentGrid(opponentGridButtons, false);

        mainGridContainer.add(myPanel, "MY_VIEW");
        mainGridContainer.add(opponentPanel, "OPPONENT_VIEW");
        centerWrapper.add(mainGridContainer);

        JPanel guidePanel = new JPanel();
        guidePanel.setPreferredSize(new Dimension(150, 300));
        guidePanel.setOpaque(false);
        centerWrapper.add(guidePanel);

        bgPanel.add(centerWrapper, BorderLayout.CENTER);

        // 3. FOOTER
        JPanel footerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerWrapper.setOpaque(false);
        footerWrapper.setBorder(new EmptyBorder(0, 0, 40, 0));

        JPanel controlCardPanel = new JPanel(new CardLayout());
        controlCardPanel.setOpaque(false);

        setupPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        setupPanel.setOpaque(false);
        btnRandom = new ModernButton("XÁO TRỘN", new Color(255, 140, 0));
        btnRandom.setPreferredSize(new Dimension(150, 45));
        btnRandom.addActionListener(e -> {
            selectedShipToMove = null;
            randomlyPlaceShipsAndDraw();
        });
        btnReady = new ModernButton("SẴN SÀNG CHIẾN ĐẤU", new Color(50, 205, 50));
        btnReady.setPreferredSize(new Dimension(200, 45));
        btnReady.addActionListener(e -> sendShipData());
        setupPanel.add(btnRandom);
        setupPanel.add(btnReady);

        endGamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        endGamePanel.setOpaque(false);
        btnPlayAgain = new ModernButton("CHƠI VÁN MỚI", new Color(30, 144, 255));
        btnPlayAgain.setPreferredSize(new Dimension(200, 45));
        btnPlayAgain.addActionListener(e -> {
            out.println("REMATCH_REQUEST");
            btnPlayAgain.setText("Đang đợi...");
            btnPlayAgain.setEnabled(false);
        });
        endGamePanel.add(btnPlayAgain);

        controlCardPanel.add(setupPanel, "SETUP");
        controlCardPanel.add(endGamePanel, "END_GAME");
        footerWrapper.add(controlCardPanel);
        bgPanel.add(footerWrapper, BorderLayout.SOUTH);

        ((CardLayout) controlCardPanel.getLayout()).show(controlCardPanel, "SETUP");
        cardLayout.show(mainGridContainer, "MY_VIEW");
        setVisible(true);
    }

    private void processServerMessage(String message) {
        if (message.startsWith("LOGIN_ERROR ")) {
            showModernNotification("LỖI ĐĂNG NHẬP", message.substring(12), false);
            this.dispose();
        } else if (message.startsWith("SERVER_STATUS ")) {
            String msg = message.substring(14);
            turnLabel.setText("<html><span style='text-shadow: 2px 2px #000000;'>" + msg.toUpperCase() + "</span></html>");
            if (msg.contains("đã vào phòng") || msg.contains("Kết nối thành công")) {
                turnLabel.setForeground(new Color(50, 255, 50));
            } else {
                turnLabel.setForeground(Color.ORANGE);
            }
        } else if (message.equals("START")) {
            setupPanel.setVisible(false);
        } else if (message.startsWith("TURN your_turn")) {
            setOpponentGridEnabled(true);
            cardLayout.show(mainGridContainer, "OPPONENT_VIEW");
            turnLabel.setText("LƯỢT BẠN: TẤN CÔNG!");
            turnLabel.setForeground(Color.RED);
            startClientTimer();
        } else if (message.startsWith("TURN opponent_turn")) {
            setOpponentGridEnabled(false);
            cardLayout.show(mainGridContainer, "MY_VIEW");
            turnLabel.setText("LƯỢT ĐỐI THỦ: TẤN CÔNG!");
            turnLabel.setForeground(Color.WHITE);
            startClientTimer();
        } else if (message.startsWith("UPDATE")) {
            handleUpdateMessage(message);
        } else if (message.startsWith("SINK ")) {
            handleSinkMessage(message, false);
        } else if (message.startsWith("SINK_SELF ")) {
            handleSinkMessage(message, true);
        } else if (message.equals("WIN")) {
            stopClientTimer();
            soundManager.playWin();
            showModernNotification("CHIẾN THẮNG", "CHÚC MỪNG! BẠN ĐÃ THẮNG!", true);
            endGame();
        } else if (message.equals("LOSE")) {
            stopClientTimer();
            soundManager.playLose();
            showModernNotification("THẤT BẠI", "RẤT TIẾC! BẠN ĐÃ THUA.", false);
            endGame();
        } else if (message.equals("REMATCH_OFFER")) {
            // Dùng Dialog hỏi ý kiến đẹp
            boolean agree = showModernConfirm("LỜI MỜI TÁI ĐẤU", "Đối thủ muốn chơi ván mới.\nBạn có đồng ý không?");
            if (agree) {
                out.println("REMATCH_REQUEST");
                btnPlayAgain.setText("Đang thiết lập...");
                btnPlayAgain.setEnabled(false);
            } else {
                out.println("REMATCH_REJECT");
                cleanup();
                this.dispose();
            }
        } else if (message.equals("REMATCH_DECLINED")) {
            showModernNotification("THÔNG BÁO", "Đối thủ đã từ chối hoặc rời đi.", false);
            cleanup();
            this.dispose();
        } else if (message.equals("RESTART")) {
            resetGameGUI();
        } else if (message.equals("OPPONENT_LEFT")) {
            stopClientTimer();
            // soundManager.playWin(); // Thắng do địch thoát
            showModernNotification("ĐỐI THỦ THOÁT", "TRÒ CHƠI KẾT THÚC!", true);
            cleanup();
            this.dispose();
        }
    }

    private void showModernNotification(String title, String msg, boolean isSuccess) {
        JDialog d = new JDialog(this, title, true);
        d.setUndecorated(true);
        d.setSize(450, 250);
        d.setLocationRelativeTo(this);
        d.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 450, 250, 20, 20));

        JPanel p = new JPanel(new GridLayout(3, 1));
        p.setBackground(new Color(20, 30, 50));

        // Màu sắc dựa trên trạng thái (Thắng/Thành công = Vàng, Thua/Lỗi = Đỏ)
        Color themeColor = isSuccess ? new Color(255, 215, 0) : new Color(255, 69, 0);

        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(themeColor, 2),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(themeColor);

        JLabel lblMsg = new JLabel("<html><center>" + msg.replace("\n", "<br>") + "</center></html>", SwingConstants.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblMsg.setForeground(Color.WHITE);

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);

        JButton btnClose = new JButton("ĐÓNG");
        btnClose.setBackground(new Color(0, 120, 215));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> d.dispose());

        btnPanel.add(btnClose);
        p.add(lblTitle);
        p.add(lblMsg);
        p.add(btnPanel);
        d.add(p);
        d.setVisible(true);
    }

    private boolean showModernConfirm(String title, String msg) {
        JDialog d = new JDialog(this, title, true);
        d.setUndecorated(true);
        d.setSize(450, 250);
        d.setLocationRelativeTo(this);
        d.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 450, 250, 20, 20));

        JPanel p = new JPanel(new GridLayout(3, 1));
        p.setBackground(new Color(20, 30, 50));
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0, 191, 255), 2), // Viền xanh dương
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(new Color(0, 191, 255));

        JLabel lblMsg = new JLabel("<html><center>" + msg.replace("\n", "<br>") + "</center></html>", SwingConstants.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblMsg.setForeground(Color.WHITE);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);

        // Biến để lưu kết quả
        final boolean[] result = {false};

        JButton btnYes = new JButton("ĐỒNG Ý");
        btnYes.setBackground(new Color(50, 205, 50)); // Xanh lá
        btnYes.setForeground(Color.WHITE);
        btnYes.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnYes.setPreferredSize(new Dimension(120, 40));
        btnYes.addActionListener(e -> {
            result[0] = true;
            d.dispose();
        });

        JButton btnNo = new JButton("TỪ CHỐI");
        btnNo.setBackground(new Color(220, 20, 60)); // Đỏ
        btnNo.setForeground(Color.WHITE);
        btnNo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNo.setPreferredSize(new Dimension(120, 40));
        btnNo.addActionListener(e -> {
            result[0] = false;
            d.dispose();
        });

        btnPanel.add(btnYes);
        btnPanel.add(btnNo);

        p.add(lblTitle);
        p.add(lblMsg);
        p.add(btnPanel);
        d.add(p);
        d.setVisible(true);

        return result[0];
    }

    private void showOpponentLeftDialog(String title, String msg, boolean isWin) {
        // if (isWin) {
        //     soundManager.playWin();
        // } else {
        //     soundManager.playLose();
        // }

        JDialog d = new JDialog(this, title, true);
        d.setUndecorated(true);
        d.setSize(450, 250);
        d.setLocationRelativeTo(this);
        d.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 450, 250, 20, 20));

        JPanel p = new JPanel(new GridLayout(3, 1));
        p.setBackground(new Color(20, 30, 50));
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(isWin ? new Color(255, 215, 0) : Color.RED, 2),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(isWin ? new Color(255, 215, 0) : Color.RED);

        JLabel lblMsg = new JLabel("<html><center>" + msg + "</center></html>", SwingConstants.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblMsg.setForeground(Color.WHITE);

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);

        JButton btnClose = new JButton("ĐÓNG");
        btnClose.setBackground(new Color(0, 120, 215));
        btnClose.setForeground(Color.WHITE);
        btnClose.addActionListener(e -> d.dispose());

        btnPanel.add(btnClose);
        p.add(lblTitle);
        p.add(lblMsg);
        p.add(btnPanel);
        d.add(p);
        d.setVisible(true);
    }

    private void connectToServer(String ip) {
        try {
            socket = new Socket(ip, 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String command = isCreating ? "CREATE_ROOM" : "JOIN_ROOM";
            out.println(command + " " + myRoomID + " " + myName);
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String finalLine = line;
                        SwingUtilities.invokeLater(() -> processServerMessage(finalLine));
                    }
                } catch (IOException e) {
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối Server!");
            this.dispose();
        }
    }

    private void cleanup() {
        if (soundManager != null) {
            soundManager.stopMusic();

        }
        try {
            if (out != null) {
                out.println("QUIT");
                out.flush();
            }
            if (socket != null) {
                socket.close();

            }
        } catch (IOException e) {
        }
    }

        private void handleUpdateMessage(String message) {
        String[] parts = message.split(" ");
        String boardType = parts[1]; // "opponent" hoặc "self"
        int x = Integer.parseInt(parts[2]);
        int y = Integer.parseInt(parts[3]);
        String result = parts[4]; // "HIT" hoặc "MISS"

        if (boardType.equals("opponent")) {
            // Nếu Server báo kết quả lượt bắn của mình lên ĐỐI THỦ
            if (result.equals("HIT")) {
                setButtonState(opponentGridButtons[x][y], "X", x, y, null); // Đối thủ chìm mình chưa biết ngay nên để null board
                soundManager.playHit();
            } else {
                setButtonState(opponentGridButtons[x][y], "O", x, y, null);
                soundManager.playMiss();
            }
        } else {
            // Nếu Server báo ĐỐI THỦ bắn trúng MÌNH
            if (result.equals("HIT")) {
                setButtonState(myGridButtons[x][y], "X", x, y, localGameBoard);
                soundManager.playHit();
            } else {
                setButtonState(myGridButtons[x][y], "O", x, y, localGameBoard);
                soundManager.playMiss();
            }
        }
    }

    private void handleSinkMessage(String message, boolean isSelf) {
        try {
            String prefix = isSelf ? "SINK_SELF " : "SINK ";
            String content = message.substring(prefix.length());
            int lastSpace = content.lastIndexOf(" ");
            String shipName = content.substring(0, lastSpace);
            String data = content.substring(lastSpace + 1);
            String[] parts = data.split(",");
            int len = Integer.parseInt(parts[0]);
            boolean hor = parts[1].equals("H");
            int sx = Integer.parseInt(parts[2]);
            int sy = Integer.parseInt(parts[3]);
            String shipCode = "S" + len;
            List<JButton> btns = new ArrayList<>();
            JButton[][] grid = isSelf ? myGridButtons : opponentGridButtons;
            for (int i = 0; i < len; i++) {
                int cx = sx, cy = sy;
                if (hor) {
                    cy += i;
                } else {
                    cx += i;

                }
                if (cx < 10 && cy < 10) {
                    btns.add(grid[cx][cy]);

                }
            }
            playSinkingAnimation(btns, shipCode, shipName);
        } catch (Exception e) {
        }
    }

    private void setMyGridEnabled(boolean enabled) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                myGridButtons[i][j].setEnabled(enabled);
            }
        }
    }

    private void sendShipData() {
        if (selectedShipToMove != null) {
            return;

        }
        StringBuilder sb = new StringBuilder();
        for (Ship s : ships) {
            sb.append(s.getProtocolString()).append(";");

        }
        out.println("SHIP_DATA " + sb.substring(0, sb.length() - 1));
        setupPanel.setVisible(false);
        setMyGridEnabled(false);
        btnRandom.setEnabled(false);
        btnReady.setEnabled(false);
        turnLabel.setText("ĐANG CHỜ ĐỐI THỦ...");
        turnLabel.setForeground(Color.LIGHT_GRAY);
    }

    private void resetGameGUI() {
        initShips();
        localGameBoard = new GameBoard();
        randomlyPlaceShipsAndDraw();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                setButtonState(opponentGridButtons[i][j], "~");
                opponentGridButtons[i][j].setEnabled(false);
            }
        }
        setMyGridEnabled(true);
        JPanel p = (JPanel) setupPanel.getParent();
        ((CardLayout) p.getLayout()).show(p, "SETUP");
        setupPanel.setVisible(true);
        endGamePanel.setVisible(false);
        btnReady.setEnabled(true);
        btnRandom.setEnabled(true);
        btnPlayAgain.setEnabled(true);
        btnPlayAgain.setText("CHƠI VÁN MỚI");
        cardLayout.show(mainGridContainer, "MY_VIEW");
        turnLabel.setText("GIAI ĐOẠN SẮP XẾP ĐỘI HÌNH");
        turnLabel.setForeground(new Color(255, 215, 0));
        stopClientTimer();
    }

    private void endGame() {
        setOpponentGridEnabled(false);
        JPanel p = (JPanel) setupPanel.getParent();
        ((CardLayout) p.getLayout()).show(p, "END_GAME");
        setupPanel.setVisible(false);
        endGamePanel.setVisible(true);
        cardLayout.show(mainGridContainer, "MY_VIEW");
        turnLabel.setText("KẾT THÚC TRẬN ĐẤU");
    }

    private void randomlyPlaceShipsAndDraw() {
        localGameBoard = new GameBoard();
        for (Ship ship : ships) {
            boolean placed = false;
            while (!placed) {
                int x = rand.nextInt(10);
                int y = rand.nextInt(10);
                boolean isHorizontal = rand.nextBoolean();
                ship.isHorizontal = isHorizontal;
                if (localGameBoard.placeShip(x, y, ship.length, isHorizontal)) {
                    ship.setPosition(x, y);
                    placed = true;
                }
            }
        }
        drawMyGridFromShipList();
    }

    private void drawMyGridFromShipList() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                setButtonState(myGridButtons[i][j], "~", i, j, localGameBoard);
            }
        }
        for (Ship ship : ships) {
            if (ship.x == -1) {
                continue;

            }
            String shipCode = "S" + ship.length;
            for (int i = 0; i < ship.length; i++) {
                int currentX = ship.x;
                int currentY = ship.y;
                if (ship.isHorizontal) {
                    currentY += i;
                } else {
                    currentX += i;

                }
                if (currentX < 10 && currentY < 10) {
                    setButtonState(myGridButtons[currentX][currentY], shipCode);
                }
            }
        }
    }

    private void handleLeftClick(int x, int y) {
        if (selectedShipToMove == null) {
            Ship clickedShip = getShipAt(x, y);
            if (clickedShip != null) {
                localGameBoard.removeShip(clickedShip.x, clickedShip.y, clickedShip.length, clickedShip.isHorizontal);
                selectedShipToMove = clickedShip;
                clickedShip.setPosition(-1, -1);
                drawMyGridFromShipList();
            }
        } else {
            boolean success = localGameBoard.placeShip(x, y, selectedShipToMove.length, selectedShipToMove.isHorizontal);
            if (success) {
                selectedShipToMove.setPosition(x, y);
                selectedShipToMove = null;
                drawMyGridFromShipList();
            }
        }
    }

    private void handleRightClick(int x, int y) {
        if (selectedShipToMove != null) {
            return;

        }
        Ship clickedShip = getShipAt(x, y);
        if (clickedShip != null) {
            localGameBoard.removeShip(clickedShip.x, clickedShip.y, clickedShip.length, clickedShip.isHorizontal);
            clickedShip.rotate();
            boolean success = localGameBoard.placeShip(clickedShip.x, clickedShip.y, clickedShip.length, clickedShip.isHorizontal);
            if (!success) {
                clickedShip.rotate();

            }
            localGameBoard.placeShip(clickedShip.x, clickedShip.y, clickedShip.length, clickedShip.isHorizontal);
            drawMyGridFromShipList();
        }
    }

    private Ship getShipAt(int x, int y) {
        for (Ship ship : ships) {
            if (ship.contains(x, y)) {
                return ship;

            }
        }
        return null;
    }

    private JPanel createTransparentGrid(JButton[][] buttons, boolean isMyGrid) {
        JPanel p = new JPanel(new GridLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 40));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(420, 420));
        p.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2));
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (isMyGrid) {
                    buttons[i][j] = createMyGridButton(i, j);
                } else {
                    buttons[i][j] = createOpponentButton(i, j);

                }
                p.add(buttons[i][j]);
            }
        }
        return p;
    }

    private JButton createMyGridButton(final int x, final int y) {
        JButton button = new JButton();
        setButtonState(button, "~");
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 1));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!btnReady.isEnabled()) {
                    return;

                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(x, y);
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    handleLeftClick(x, y);

                }
            }
        });
        return button;
    }

    private JButton createOpponentButton(final int x, final int y) {
        JButton button = new JButton();
        setButtonState(button, "~");
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 1));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.addActionListener(e -> out.println("FIRE " + x + " " + y));
        return button;
    }

        private void setButtonState(JButton btn, String state, int x, int y, GameBoard board) {
        btn.setText(state);
        btn.setForeground(new Color(0, 0, 0, 0));
        btn.setIcon(null);
        btn.setDisabledIcon(null);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);

        if (state.equals("~")) {
            btn.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 50), 1));
        } else if (state.startsWith("S") || state.equals("X")) {
            ImageIcon currentIcon = null;

            // 1. Chọn icon gốc (Tàu hoặc Tâm đỏ)
            if (state.startsWith("S")) {
                if (state.equals("S5")) currentIcon = iconShip5;
                else if (state.equals("S4")) currentIcon = iconShip4;
                else if (state.equals("S3")) currentIcon = iconShip3;
                else if (state.equals("S2")) currentIcon = iconShip2;
                else currentIcon = iconShip3;
            } else if (state.equals("X")) {
                currentIcon = iconHit;
            }

            // 2. Nếu tàu tại ô này ĐÃ CHÌM, đè ảnh X đỏ lên
            if (board != null && board.isShipSunkAt(x, y)) {
                currentIcon = combineWithSunkX(currentIcon);
            }

            if (currentIcon != null) {
                btn.setIcon(currentIcon);
                btn.setDisabledIcon(currentIcon);
                btn.setBorder(null);
            } else {
                btn.setBackground(state.equals("X") ? Color.RED : Color.GRAY);
                btn.setOpaque(true);
            }
        } else if (state.equals("O")) {
            if (iconMiss != null) {
                btn.setIcon(iconMiss);
                btn.setDisabledIcon(iconMiss);
            } else {
                btn.setBackground(Color.BLUE);
                btn.setOpaque(true);
            }
        }
    }

    private void playSinkingAnimation(List<JButton> shipButtons, String finalShipCode, String shipName) {
        soundManager.playHit();
        int delay = 120;
        int totalFlashes = 7;
        final int[] counter = {0};
        Timer explosionTimer = new Timer(delay, null);
        explosionTimer.addActionListener(e -> {
            counter[0]++;
            boolean isBright = (counter[0] % 2 != 0);
            for (JButton btn : shipButtons) {
                if (isBright) {
                    btn.setBackground(new Color(255, 69, 0));
                    btn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                    btn.setIcon(null);
                    btn.setOpaque(true);
                } else {
                    btn.setBackground(Color.RED);
                    btn.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 1));
                    if (iconHit != null) {
                        btn.setIcon(iconHit);

                    }
                    btn.setOpaque(false);
                }
            }
            if (counter[0] >= totalFlashes) {
                explosionTimer.stop();
                for (JButton btn : shipButtons) {
                    btn.setBorder(null);
                    setButtonState(btn, finalShipCode);
                }
            }
        });
        explosionTimer.start();
    }

    private ImageIcon combineWithSunkX(ImageIcon baseIcon) {
    int size = 40; 
    BufferedImage combined = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = combined.createGraphics();

    // Vẽ tàu bên dưới
    if (baseIcon != null) {
        g2.drawImage(baseIcon.getImage(), 0, 0, size, size, null);
    }

    // Vẽ ảnh X đỏ của Phước đè lên trên
    if (iconSunkX != null) {
        g2.drawImage(iconSunkX.getImage(), 0, 0, size, size, null);
    } else {
        // Dự phòng nếu load ảnh lỗi thì tự vẽ đường chéo đỏ
        g2.setColor(Color.RED);
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(5, 5, size-5, size-5);
        g2.drawLine(size-5, 5, 5, size-5);
    }

    g2.dispose();
    return new ImageIcon(combined);
}

    private void setOpponentGridEnabled(boolean enabled) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                String state = opponentGridButtons[i][j].getText();
                if (!state.equals("X") && !state.equals("O") && !state.startsWith("S")) {
                    opponentGridButtons[i][j].setEnabled(enabled);
                } else {
                    opponentGridButtons[i][j].setEnabled(false);
                    opponentGridButtons[i][j].setDisabledIcon(opponentGridButtons[i][j].getIcon());
                }
            }
        }
    }

    private void loadImages() {
        try {
            iconWater = loadAndScale("resources/images/water.png", 32, 32);
            iconHit = loadAndScale("resources/images/hit.png", 32, 32);
            iconMiss = loadAndScale("resources/images/miss.png", 32, 32);
            iconShip5 = loadAndScale("resources/images/ship5.png", 32, 32);
            iconShip4 = loadAndScale("resources/images/ship4.png", 32, 32);
            iconShip3 = loadAndScale("resources/images/ship3.png", 32, 32);
            iconShip2 = loadAndScale("resources/images/ship2.png", 32, 32);
            iconSunkX = new ImageIcon(ImageIO.read(new File("resources/images/sunk.png"))
                        .getScaledInstance(40, 40, Image.SCALE_SMOOTH));
            try {
                backgroundImage = ImageIO.read(new File("images/bg_sea.jpg"));
            } catch (Exception e) {
                backgroundImage = null;
            }
            captainIcon = loadAndScale("images/avatar.png", 100, 100);
        } catch (Exception e) {
            System.err.println("CẢNH BÁO: Thiếu file ảnh.");
        }
    }

    private ImageIcon loadAndScale(String path, int w, int h) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException e) {
            return null;
        }
    }

    class BackgroundPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(0, 100, 150), 0, getHeight(), new Color(0, 30, 60));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g.setColor(new Color(255, 255, 255, 20));
            for (int i = 0; i < getWidth(); i += 50) {
                g.drawLine(i, 0, i, getHeight());

            }
            for (int i = 0; i < getHeight(); i += 50) {
                g.drawLine(0, i, getWidth(), i);

            }
        }
    }

    class ModernButton extends JButton {

        private Color baseColor;
        private boolean isHovered = false;

        public ModernButton(String text, Color color) {
            super(text);
            this.baseColor = color;
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(new Color(80, 80, 80));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            } else {
                Color top = isHovered ? baseColor.brighter() : baseColor;
                Color bottom = isHovered ? baseColor.darker() : baseColor.darker();
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                // Outer glow border
                g2.setColor(new Color(255, 255, 255, isHovered ? 120 : 80));
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 16, 16);
            }

            g2.dispose();
            super.paintComponent(g);
        }
        private ImageIcon getSunkShipIcon(ImageIcon shipIcon) {
            if (shipIcon == null) return iconSunkX;
            
            int w = shipIcon.getIconWidth();
            int h = shipIcon.getIconHeight();
            BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = combined.createGraphics();
            
            // Vẽ tàu bên dưới
            g2.drawImage(shipIcon.getImage(), 0, 0, null);
            
            // Vẽ dấu X màu đỏ đè lên trên
            g2.setColor(new Color(255, 0, 0, 200)); // Màu đỏ
            g2.setStroke(new BasicStroke(4)); // Nét dày
            int gap = 5;
            g2.drawLine(gap, gap, w - gap, h - gap);
            g2.drawLine(w - gap, gap, gap, h - gap);
            
            g2.dispose();
            return new ImageIcon(combined);
        }
    }
}
