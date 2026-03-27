package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities; // Thêm import này
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class DotsAndBoxesClient extends JFrame {

    private DotsPanel panel;
    private Socket socket;
    private PrintWriter out;

    private String myName;
    private String myRoomID;
    private boolean isCreating;
    private int myID = 0;

    private String p1Name = "Player 1";
    private String p2Name = "Player 2";
    private boolean isRematchMode = false;

    // UI
    private JLabel lblStatus;
    private JLabel lblP1Score, lblP2Score;
    private ModernButton btnReady;
    private JTextArea chatArea;
    private JTextField chatInput;

    // Sound
    private SoundManager soundManager;
    private JDialog gameOverDialog;

    public DotsAndBoxesClient(String serverIP, String playerName, String roomID, boolean isCreating) {
        this.myName = playerName;
        this.myRoomID = roomID;
        this.isCreating = isCreating;

        // 1. ÂM THANH
        soundManager = new SoundManager();
        soundManager.playMusic("music_dots.wav"); // Nhạc nền riêng

        setTitle("Dots & Boxes - " + playerName);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(20, 25, 40));

        // 2. SCORE
        JPanel scorePanel = new JPanel(new GridLayout(1, 3));
        scorePanel.setBackground(new Color(20, 25, 40));
        scorePanel.setPreferredSize(new Dimension(0, 60));
        scorePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(60, 70, 90)));
        lblP1Score = new JLabel("P1 (Xanh): 0", SwingConstants.CENTER);
        lblP1Score.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblP1Score.setForeground(new Color(0, 200, 255));
        lblStatus = new JLabel("Đang kết nối...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblStatus.setForeground(Color.WHITE);
        lblP2Score = new JLabel("P2 (Hồng): 0", SwingConstants.CENTER);
        lblP2Score.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblP2Score.setForeground(new Color(255, 50, 150));
        scorePanel.add(lblP1Score);
        scorePanel.add(lblStatus);
        scorePanel.add(lblP2Score);
        add(scorePanel, BorderLayout.NORTH);

        // 3. BOARD
        panel = new DotsPanel(this);
        panel.setMyTurn(false);
        add(panel, BorderLayout.CENTER);

        // 4. CHAT
        JPanel chatPanel = new JPanel(new BorderLayout(0, 10));
        chatPanel.setPreferredSize(new Dimension(300, 0));
        chatPanel.setBackground(new Color(30, 30, 35));
        chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel lblChat = new JLabel("KÊNH CHAT");
        lblChat.setForeground(Color.CYAN);
        lblChat.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chatPanel.add(lblChat, BorderLayout.NORTH);
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(40, 40, 45));
        chatArea.setForeground(new Color(0, 255, 127));
        chatArea.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollChat = new JScrollPane(chatArea);
        scrollChat.setBorder(new LineBorder(new Color(60, 60, 60)));
        scrollChat.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setOpaque(false);
        inputPanel.setPreferredSize(new Dimension(0, 40));
        chatInput = new JTextField();
        chatInput.setBackground(new Color(20, 20, 20));
        chatInput.setForeground(Color.WHITE);
        chatInput.setCaretColor(Color.WHITE);
        chatInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatInput.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.GRAY), new EmptyBorder(0, 5, 0, 5)));
        ModernButton btnSend = new ModernButton("GỬI", new Color(0, 120, 215));
        btnSend.setPreferredSize(new Dimension(70, 0));
        ActionListener sendAction = e -> sendChat();
        btnSend.addActionListener(sendAction);
        chatInput.addActionListener(sendAction);
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(btnSend, BorderLayout.EAST);
        chatPanel.add(scrollChat, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        add(chatPanel, BorderLayout.EAST);

        // 5. BOTTOM
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(new Color(20, 25, 40));
        bottomPanel.setBorder(new EmptyBorder(10, 0, 15, 0));

        btnReady = new ModernButton("SẴN SÀNG", new Color(255, 140, 0));
        btnReady.setPreferredSize(new Dimension(200, 45));
        btnReady.addActionListener(e -> handleReadyClick());

        bottomPanel.add(btnReady);
        add(bottomPanel, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cleanup();
            }
        });

        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);

        connectToServer(serverIP);
    }

    // --- LOGIC ---
    private void handleReadyClick() {
        if (!isRematchMode) {
            if (out != null) {
                out.println("READY");
                btnReady.setEnabled(false);
                btnReady.setText("ĐANG ĐỢI...");
            }
        } else {
            if (out != null) {
                out.println("REMATCH_REQUEST");
                btnReady.setEnabled(false);
                btnReady.setText("ĐANG ĐỢI ĐỐI THỦ...");
            }
        }
    }

    private void resetGameGUI() {
        remove(panel);
        panel = new DotsPanel(this);
        panel.setMyTurn(false);
        add(panel, BorderLayout.CENTER);
        lblP1Score.setText(p1Name + " (Xanh): 0");
        lblP2Score.setText(p2Name + " (Hồng): 0");
        lblStatus.setText("Ván mới bắt đầu!");
        btnReady.setVisible(false);
        isRematchMode = false;
        revalidate();
        repaint();
    }

    private void connectToServer(String ip) {
        try {
            socket = new Socket(ip, 54321);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String command = isCreating ? "CREATE_ROOM" : "JOIN_ROOM";
            out.println(command + " " + myRoomID + " " + myName);

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        // Dùng invokeLater để tránh lỗi luồng UI
                        String finalLine = line; 
                        SwingUtilities.invokeLater(() -> processMessage(finalLine));
                    }
                } catch (IOException e) {
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối Server!");
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
            if (socket != null && !socket.isClosed()) {
                socket.close();

            }
        } catch (IOException e) {
        }
    }

    public void sendMove(String type, int r, int c) {
        if (out != null) {
            out.println("MOVE " + type + " " + r + " " + c);
        }
    }

    private void sendChat() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty() && out != null) {
            out.println("CHAT " + msg);
            chatInput.setText("");
        }
    }

    private void processMessage(String cmd) {
        if (cmd.startsWith("MESSAGE")) {
            lblStatus.setText(cmd.substring(8));
        } else if (cmd.startsWith("LOGIN_ERROR ")) {
            JOptionPane.showMessageDialog(this, cmd.substring(12), "Lỗi", JOptionPane.ERROR_MESSAGE);
            this.dispose();
        } else if (cmd.startsWith("UPDATE_NAMES ")) {
            String content = cmd.substring(13);
            String[] names = content.split(",");
            if (names.length >= 2) {
                p1Name = names[0];
                p2Name = names[1];
                updateScoreBoard();
            }
        } else if (cmd.startsWith("CHAT_MSG ")) {
            chatArea.append(cmd.substring(9) + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        } else if (cmd.startsWith("START")) {
            myID = Integer.parseInt(cmd.split(" ")[1]);
            String role = (myID == 1) ? "(Chủ Phòng)" : "(Khách)";
            setTitle("Dots & Boxes - " + myName + " " + role);
            if (myID == 1) {
                lblStatus.setText("PHÒNG: " + myRoomID);
                lblStatus.setForeground(Color.YELLOW);
            } else {
                lblStatus.setText("Đã vào phòng!");
            }
        } else if (cmd.equals("GAME_BEGIN")) {
            btnReady.setVisible(false);
            btnReady.getParent().revalidate();
        } else if (cmd.equals("RESTART")) {
            if (gameOverDialog != null) {
                gameOverDialog.dispose();

            }
            resetGameGUI();
        } else if (cmd.startsWith("TURN")) {
            int turn = Integer.parseInt(cmd.split(" ")[1]);
            panel.setMyTurn(turn == myID);
            if (turn == myID) {
                lblStatus.setText("LƯỢT CỦA BẠN!");
                lblStatus.setForeground(Color.GREEN);
            } else {
                lblStatus.setText("Lượt đối thủ...");
                lblStatus.setForeground(Color.YELLOW);
            }
        } else if (cmd.startsWith("UPDATE")) {
            String[] parts = cmd.split(" ");
            String type = parts[1];
            int r = Integer.parseInt(parts[2]);
            int c = Integer.parseInt(parts[3]);
            panel.remoteAddLine(type, r, c);

            if (parts.length > 4) {
                String flag = parts[4];
                if (flag.equals("S")) {
                    soundManager.playScore(); // Ting
                } else {
                    soundManager.playDraw(); // Xoẹt
                }
            } else {
                soundManager.playDraw();
            }
        } else if (cmd.startsWith("BOX")) {
            String[] parts = cmd.split(" ");
            panel.updateBox(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            updateScoreBoard();
        } else if (cmd.equals("OPPONENT_LEFT")) {
            soundManager.playWin(); // Thắng do địch thoát
            showGameOverDialog("ĐỐI THỦ ĐÃ THOÁT", "TRÒ CHƠI KẾT THÚC!", "", true);
            panel.setMyTurn(false);
            lblStatus.setText("ĐỐI THỦ ĐÃ THOÁT");
            lblStatus.setForeground(Color.RED);
            cleanup();
            this.dispose();
        } else if (cmd.startsWith("GAMEOVER")) {
            updateScoreBoard();

            int s1 = panel.board.getP1Score();
            int s2 = panel.board.getP2Score();
            String resultTitle = "HÒA!";
            String resultMsg = "";
            boolean isWin = false;

            if (s1 > s2) {
                if (myID == 1) {
                    resultTitle = "CHIẾN THẮNG!";
                    resultMsg = "Chúc mừng bạn đã thắng!";
                    isWin = true;
                } else {
                    resultTitle = "THẤT BẠI!";
                    resultMsg = "Bạn đã thua cuộc.";
                    isWin = false;
                }
            } else if (s2 > s1) {
                if (myID == 2) {
                    resultTitle = "CHIẾN THẮNG!";
                    resultMsg = "Chúc mừng bạn đã thắng!";
                    isWin = true;
                } else {
                    resultTitle = "THẤT BẠI!";
                    resultMsg = "Bạn đã thua cuộc.";
                    isWin = false;
                }
            } else {
                resultMsg = "Hai bên ngang tài ngang sức!";
                isWin = false; 
            }

            String scoreText = p1Name + ": " + s1 + " - " + p2Name + ": " + s2;

            if (isWin) {
                soundManager.playWin();
            } else {
                soundManager.playLose();
            }

            showGameOverDialog(resultTitle, resultMsg, scoreText, isWin);
            lblStatus.setText("GAME OVER");

            // Hiện nút Chơi lại
            isRematchMode = true;
            btnReady.setText("CHƠI VÁN MỚI");
            btnReady.setEnabled(true);
            btnReady.setVisible(true);
        } else if (cmd.equals("OPPONENT_LEFT")) {
            // soundManager.playWin(); // Thắng do địch thoát
            showGameOverDialog("ĐỐI THỦ ĐÃ THOÁT", "TRÒ CHƠI KẾT THÚC!", "", true);
            panel.setMyTurn(false);
            lblStatus.setText("ĐỐI THỦ ĐÃ THOÁT");
            lblStatus.setForeground(Color.RED);
            cleanup();
            this.dispose();
        }

        // ===> XỬ LÝ REMATCH OFFER (PHẦN MỚI) <===
        else if (cmd.equals("REMATCH_OFFER")) {
            if (!btnReady.isEnabled() && btnReady.getText().contains("ĐANG ĐỢI")) {
                return; 
            }

            boolean agree = showModernConfirm("LỜI MỜI TÁI ĐẤU", "Đối thủ muốn chơi ván mới.\nBạn có đồng ý không?");
            
            if (agree) {
                out.println("REMATCH_REQUEST");
                btnReady.setText("Đang thiết lập...");
                btnReady.setEnabled(false);
            } else {
                out.println("REMATCH_REJECT");
                this.dispose();
            }
        }
        else if (cmd.equals("REMATCH_DECLINED")) {
            showModernNotification("THÔNG BÁO", "Đối thủ đã từ chối.\nTrò chơi kết thúc", false);
            this.dispose();
        }
    }

    private void updateScoreBoard() {
        lblP1Score.setText(p1Name + " (Xanh): " + panel.board.getP1Score());
        lblP2Score.setText(p2Name + " (Hồng): " + panel.board.getP2Score());
    }

    // --- CÁC HÀM HIỂN THỊ THÔNG BÁO MỚI ---
    
    // 1. Hiển thị thông báo Chung (Thắng/Thua)
    private void showGameOverDialog(String title, String msg, String score, boolean isWin) {
        JDialog d = new JDialog(this, "KẾT QUẢ", true);
        d.setUndecorated(true);
        d.setSize(400, 300);
        d.setLocationRelativeTo(this);
        d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel p = new JPanel(new GridLayout(4, 1));
        p.setBackground(new Color(30, 30, 40));
        Color borderColor = isWin ? new Color(255, 215, 0) : Color.RED;
        p.setBorder(BorderFactory.createCompoundBorder(new LineBorder(borderColor, 3), new EmptyBorder(20, 20, 20, 20)));
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblTitle.setForeground(borderColor);
        JLabel lblMsg = new JLabel(msg, SwingConstants.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblMsg.setForeground(Color.WHITE);
        JLabel lblScore = new JLabel(score, SwingConstants.CENTER);
        lblScore.setFont(new Font("Consolas", Font.BOLD, 20));
        lblScore.setForeground(Color.CYAN);

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);
        ModernButton btnOK = new ModernButton("ĐÓNG", borderColor);
        btnOK.setPreferredSize(new Dimension(120, 40));
        btnOK.addActionListener(e -> d.dispose());
        btnPanel.add(btnOK);
        p.add(lblTitle);
        p.add(lblMsg);
        p.add(lblScore);
        p.add(btnPanel);
        d.add(p);
        this.gameOverDialog = d;
        d.setVisible(true);
    }
    
    // 2. Hiển thị thông báo nhỏ (Notification)
    private void showModernNotification(String title, String msg, boolean isSuccess) {
        JDialog d = new JDialog(this, title, true);
        d.setUndecorated(true);
        d.setSize(400, 200);
        d.setLocationRelativeTo(this);
        d.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 400, 200, 20, 20));

        JPanel p = new JPanel(new GridLayout(3, 1));
        p.setBackground(new Color(30, 30, 45));
        Color c = isSuccess ? Color.GREEN : Color.RED;
        p.setBorder(BorderFactory.createLineBorder(c, 2));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(c);

        JLabel lblMsg = new JLabel("<html><center>" + msg + "</center></html>", SwingConstants.CENTER);
        lblMsg.setForeground(Color.WHITE);
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JPanel bp = new JPanel(); bp.setOpaque(false);
        ModernButton b = new ModernButton("OK", c);
        b.addActionListener(e -> d.dispose());
        bp.add(b);

        p.add(lblTitle); p.add(lblMsg); p.add(bp);
        d.add(p);
        d.setVisible(true);
    }

    // 3. Hiển thị Confirm (Đồng ý / Từ chối)
    private boolean showModernConfirm(String title, String msg) {
        JDialog d = new JDialog(this, title, true);
        d.setUndecorated(true);
        d.setSize(400, 200);
        d.setLocationRelativeTo(this);
        d.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 400, 200, 20, 20));

        JPanel p = new JPanel(new GridLayout(3, 1));
        p.setBackground(new Color(40, 40, 60));
        p.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 255), 2));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(0, 255, 255));

        JLabel lblMsg = new JLabel("<html><center>" + msg.replace("\n", "<br>") + "</center></html>", SwingConstants.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblMsg.setForeground(Color.WHITE);

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);
        
        final boolean[] res = {false};
        JButton btnYes = new JButton("ĐỒNG Ý");
        btnYes.setBackground(new Color(50, 205, 50));
        btnYes.setForeground(Color.WHITE);
        btnYes.addActionListener(e -> { res[0] = true; d.dispose(); });
        
        JButton btnNo = new JButton("KHÔNG");
        btnNo.setBackground(new Color(220, 20, 60));
        btnNo.setForeground(Color.WHITE);
        btnNo.addActionListener(e -> { res[0] = false; d.dispose(); });

        btnPanel.add(btnYes); btnPanel.add(btnNo);
        p.add(lblTitle); p.add(lblMsg); p.add(btnPanel);
        d.add(p);
        d.setVisible(true);
        return res[0];
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
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                public void mouseExited(java.awt.event.MouseEvent e) {
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
                java.awt.GradientPaint gp = new java.awt.GradientPaint(0, 0, top, 0, getHeight(), bottom);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(255, 255, 255, isHovered ? 120 : 80));
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 16, 16);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}