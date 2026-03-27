package client;

import server.GameServer;
import server.DotsServer;
import java.awt.BorderLayout;
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
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class GameMenu extends JFrame {

    private JTextField ipField;
    private JTextField nameField;
    private JButton btnMusic, btnSFX;

    // Màu sắc giao diện
    private final Color COLOR_BG_START = new Color(36, 37, 92); // Xanh đậm
    private final Color COLOR_BG_END = new Color(15, 12, 41); // Đen xanh
    private final Color COLOR_GOLD = new Color(255, 215, 0); // Vàng gold
    private final Color COLOR_DIALOG_BG = new Color(40, 40, 60); // Màu nền Dialog

    public GameMenu() {
        setTitle("GRID MASTERS - LAN GAMING");
        setSize(500, 750); // Tăng chiều cao một chút để chứa thêm nút
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        BackgroundPanel mainPanel = new BackgroundPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 40, 20, 40));
        setContentPane(mainPanel);

        // 0. TOOLBAR (Chỉ còn nút Nhạc & SFX)
        JPanel topToolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topToolBar.setOpaque(false);
        topToolBar.setMaximumSize(new Dimension(500, 40));
        btnMusic = createToolButton("NHẠC: ON");
        btnMusic.addActionListener(e -> toggleMusic());
        btnSFX = createToolButton("HIỆU ỨNG: ON");
        btnSFX.addActionListener(e -> toggleSFX());
        topToolBar.add(btnMusic);
        topToolBar.add(btnSFX);
        mainPanel.add(topToolBar);

        // 1. HEADER
        JLabel titleLabel = new JLabel("GAME CENTER");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String myIP = getMyIP();
        JLabel ipInfoLabel = new JLabel("IP Của Bạn: " + myIP);
        ipInfoLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        ipInfoLabel.setForeground(COLOR_GOLD);
        ipInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(ipInfoLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // 2. INPUT
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        inputPanel.setLayout(new GridLayout(2, 1, 0, 10));
        inputPanel.setMaximumSize(new Dimension(400, 100));
        JPanel nameRow = createInputRow("Tên bạn:", "Player");
        nameField = (JTextField) nameRow.getComponent(1);
        JPanel ipRow = createInputRow("IP Server:", "localhost");
        ipField = (JTextField) ipRow.getComponent(1);
        inputPanel.add(nameRow);
        inputPanel.add(ipRow);
        mainPanel.add(inputPanel);

        // 3. BUTTONS (THÊM NÚT BXH VÀO ĐÂY)
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 15)); // 3 hàng
        buttonPanel.setOpaque(false);
        buttonPanel.setMaximumSize(new Dimension(350, 180));

        // ===> NÚT BXH MỚI (TO VÀ ĐẸP HƠN) <===
        ModernButton btnRank = new ModernButton(" BẢNG XẾP HẠNG", COLOR_GOLD);
        btnRank.setForeground(Color.BLACK); // Chữ đen trên nền vàng
        btnRank.setFont(new Font("Segoe UI", Font.BOLD, 20));
        btnRank.addActionListener(e -> showLeaderboard());

        ModernButton btnBattleship = new ModernButton("BẮN TÀU CHIẾN", new Color(0, 198, 255));
        btnBattleship.addActionListener(e -> launchBattleship());

        ModernButton btnDots = new ModernButton("NỐI ĐIỂM", new Color(255, 81, 47));
        btnDots.addActionListener(e -> launchDots());

        buttonPanel.add(btnRank); // Thêm nút BXH lên đầu
        buttonPanel.add(btnBattleship);
        buttonPanel.add(btnDots);

        mainPanel.add(buttonPanel);

        // 4. STATUS
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        JLabel statusLabel = new JLabel("Server đang chạy ngầm...", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(0, 255, 127));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalGlue());

        setVisible(true);
    }

    // --- LOGIC BXH (LEADERBOARD) ---
    private void showLeaderboard() {
        // String ip = ipField.getText().trim();
        // if (ip.isEmpty())
        //     ip = "localhost";
        // final String fIp = ip;

        // new Thread(() -> {
        //     try {
        //         Socket s = new Socket(fIp, 12345);
        //         PrintWriter out = new PrintWriter(s.getOutputStream(), true);
        //         BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

        //         out.println("VIEW_TOP");
        //         String response = in.readLine();
        //         s.close();

        //         if (response != null && response.startsWith("TOP_DATA ")) {
        //             String data = response.substring(9);
        //             SwingUtilities.invokeLater(() -> showRankDialog(data));
        //         }
        //     } catch (IOException e) {
        //         SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
        //                 "Không thể lấy dữ liệu BXH!\n(Server chưa bật hoặc sai IP)"));
        //     }
        // }).start();
        showRankDialog();
    }

    // ===> HÀM HIỂN THỊ DIALOG BXH ĐẸP HƠN <===
    private void showRankDialog() {
        JDialog d = new JDialog(this, "BẢNG VÀNG", true);
        d.setUndecorated(true);
        d.setSize(400, 550);
        d.setLocationRelativeTo(this);
        // Bo tròn
        d.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 400, 550, 30, 30));
        
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(COLOR_DIALOG_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(255, 215, 0), 2), // Viền vàng
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        // HEADER
        JLabel lblTitle = new JLabel("<html><span style='font-size:24px'></span> TOP BẢNG XẾP HẠNG <span style='font-size:24px'></span></html>", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(255, 215, 0));
        lblTitle.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        // LIST PANEL (Nơi chứa danh sách)
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        // ===> TRẠNG THÁI CHỜ (LOADING) <===
        JLabel lblLoading = new JLabel("⏳ Đang tải dữ liệu từ máy chủ...", SwingConstants.CENTER);
        lblLoading.setForeground(Color.CYAN);
        lblLoading.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        lblLoading.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Thêm cái Loading vào trước
        listPanel.add(Box.createVerticalGlue());
        listPanel.add(lblLoading);
        listPanel.add(Box.createVerticalGlue());
        
        // ScrollPane
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        p.add(lblTitle, BorderLayout.NORTH);
        p.add(scrollPane, BorderLayout.CENTER);
        
        // FOOTER
        ModernButton btnClose = new ModernButton("ĐÓNG", new Color(80, 80, 80));
        btnClose.setPreferredSize(new Dimension(120, 40));
        btnClose.addActionListener(e -> d.dispose());
        JPanel btnP = new JPanel(); 
        btnP.setOpaque(false); 
        btnP.setBorder(new EmptyBorder(20,0,20,0));
        btnP.add(btnClose);
        p.add(btnP, BorderLayout.SOUTH);
        d.add(p);
        
        // ===> BẮT ĐẦU TẢI DỮ LIỆU NGẦM (THREAD) <===
        new Thread(() -> {
            String data = "Lỗi mạng";
            try {
                String ip = ipField.getText().trim();
                if (ip.isEmpty()) ip = "localhost";
                
                Socket s = new Socket(ip, 12345);
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                
                out.println("VIEW_TOP");
                String response = in.readLine(); 
                s.close();
                
                if (response != null && response.startsWith("TOP_DATA ")) {
                    data = response.substring(9);
                } else {
                    data = "Chưa có dữ liệu";
                }
            } catch (Exception e) {
                data = "Không thể kết nối Server!";
            }

            // Cập nhật giao diện sau khi tải xong
            final String finalData = data;
            SwingUtilities.invokeLater(() -> {
                listPanel.removeAll(); // Xóa chữ "Đang tải..."
                
                if (finalData.equals("Chưa có dữ liệu") || finalData.contains("Lỗi") || finalData.isEmpty()) {
                    JLabel lbl = new JLabel(finalData, SwingConstants.CENTER);
                    lbl.setForeground(Color.GRAY);
                    lbl.setFont(new Font("Segoe UI", Font.ITALIC, 16));
                    lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                    listPanel.add(Box.createVerticalGlue());
                    listPanel.add(lbl);
                    listPanel.add(Box.createVerticalGlue());
                } else {
                    String[] items = finalData.split(",");
                    int rank = 1;
                    for (String item : items) {
                        String[] parts = item.split(":");
                        String name = parts[0];
                        String score = parts[1];
                        
                        JPanel row = new JPanel(new BorderLayout());
                        row.setOpaque(false);
                        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
                        row.setPreferredSize(new Dimension(350, 55));
                        row.setBackground(new Color(255, 255, 255, (rank % 2 == 0) ? 10 : 20));
                        row.setOpaque(true);
                        row.setBorder(new EmptyBorder(0, 15, 0, 15));
                        
                        Color rankColor = (rank == 1) ? new Color(255, 215, 0) : 
                                          (rank == 2) ? new Color(192, 192, 192) : 
                                          (rank == 3) ? new Color(205, 127, 50) : Color.WHITE;

                        JLabel lblName = new JLabel("#" + rank + "  " + name);
                        lblName.setForeground(rankColor);
                        lblName.setFont(new Font("Segoe UI", Font.BOLD, 18));
                        
                        JLabel lblScore = new JLabel(score + " WINS");
                        lblScore.setForeground(Color.GREEN);
                        lblScore.setFont(new Font("Consolas", Font.BOLD, 16));
                        
                        row.add(lblName, BorderLayout.WEST);
                        row.add(lblScore, BorderLayout.EAST);
                        listPanel.add(row);
                        listPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                        rank++;
                    }
                }
                // Vẽ lại Panel để hiện dữ liệu mới
                listPanel.revalidate();
                listPanel.repaint();
            });
        }).start();
        // ===========================================

        d.setVisible(true);
    }

    // ... (Các hàm logic launchBots, launchBattleship, startServers, main, helper
    // giữ nguyên)
    private void launchDots() {
        String ip = ipField.getText().trim();
        String name = nameField.getText().trim();
        if (ip.isEmpty())
            ip = "localhost";
        if (name.isEmpty())
            name = "Unknown";
        final String fIp = ip;
        final String fName = name;
        new CustomOptionDialog(this, "Dots And Boxes", "Chọn chế độ chơi", "TẠO PHÒNG", "VÀO PHÒNG", (choice) -> {
            if (choice == 0) {
                int randomCode = 1000 + new Random().nextInt(9000);
                String roomID = String.valueOf(randomCode);
                new CustomInfoDialog(this, "PHÒNG ĐÃ TẠO", "Mã phòng của bạn là:", roomID, () -> {
                    new Thread(() -> new DotsAndBoxesClient(fIp, fName, roomID, true)).start();
                });
            } else {
                new CustomInputDialog(this, "VÀO PHÒNG", "Nhập mã phòng (Code):", (inputCode) -> {
                    if (inputCode != null && !inputCode.trim().isEmpty()) {
                        new Thread(() -> new DotsAndBoxesClient(fIp, fName, inputCode.trim(), false)).start();
                    }
                });
            }
        });
    }

    private void launchBattleship() {
        String ip = ipField.getText().trim();
        String name = nameField.getText().trim();
        if (ip.isEmpty())
            ip = "localhost";
        if (name.isEmpty())
            name = "Unknown";
        final String fIp = ip;
        final String fName = name;
        new CustomOptionDialog(this, "BATTLESHIP", "Chọn chế độ chơi:", "TẠO PHÒNG", "VÀO PHÒNG", (choice) -> {
            if (choice == 0) {
                int randomCode = 1000 + new Random().nextInt(9000);
                String roomID = String.valueOf(randomCode);
                new CustomInfoDialog(this, "PHÒNG ĐÃ TẠO", "Mã phòng chiến hạm:", roomID, () -> {
                    new Thread(() -> new GameClient(fIp, fName, roomID, true)).start();
                });
            } else {
                new CustomInputDialog(this, "THAM CHIẾN", "Nhập mã phòng:", (inputCode) -> {
                    if (inputCode != null && !inputCode.trim().isEmpty()) {
                        new Thread(() -> new GameClient(fIp, fName, inputCode.trim(), false)).start();
                    }
                });
            }
        });
    }

    public static void startServers() {
        new Thread(() -> {
            try {
                GameServer.main(new String[] {});
            } catch (Exception e) {
            }
        }).start();
        new Thread(() -> {
            try {
                DotsServer.main(new String[] {});
            } catch (Exception e) {
            }
        }).start();
    }

    public static void main(String[] args) {
        startServers();
        SwingUtilities.invokeLater(GameMenu::new);
    }

    private JPanel createInputRow(String labelText, String defaultValue) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setPreferredSize(new Dimension(80, 30));
        JTextField field = new JTextField(defaultValue, 15);
        field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        field.setBackground(Color.WHITE);
        field.setForeground(Color.BLACK);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 0),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        panel.add(label);
        panel.add(field);
        return panel;
    }

    private String getMyIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private JButton createToolButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 30));
        return btn;
    }

    private void toggleMusic() {
        SoundManager.isMusicEnabled = !SoundManager.isMusicEnabled;
        if (SoundManager.isMusicEnabled) {
            btnMusic.setText("NHẠC: ON");
            btnMusic.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        } else {
            btnMusic.setText("NHẠC: OFF");
            btnMusic.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
    }

    private void toggleSFX() {
        SoundManager.isSFXEnabled = !SoundManager.isSFXEnabled;
        if (SoundManager.isSFXEnabled) {
            btnSFX.setText("HIỆU ỨNG: ON");
            btnSFX.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        } else {
            btnSFX.setText("HIỆU ỨNG: OFF");
            btnSFX.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
    }

    class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, COLOR_BG_START, 0, getHeight(), COLOR_BG_END);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    class ModernButton extends JButton {
        private Color baseColor;
        private boolean isHovered = false;

        public ModernButton(String text, Color color) {
            super(text);
            this.baseColor = color;
            setFont(new Font("SansSerif", Font.BOLD, 18));
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
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled())
                g2.setColor(new Color(80, 80, 80));
            else
                g2.setColor(isHovered ? baseColor.brighter() : baseColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            super.paintComponent(g);
        }
    }

    class CustomOptionDialog extends JDialog {
        public CustomOptionDialog(JFrame parent, String title, String message, String btnText1, String btnText2,
                java.util.function.Consumer<Integer> callback) {
            super(parent, title, true);
            setUndecorated(true);
            setSize(400, 250);
            setLocationRelativeTo(parent);
            JPanel p = new JPanel(new GridLayout(4, 1, 10, 10));
            p.setBackground(COLOR_DIALOG_BG);
            p.setBorder(
                    BorderFactory.createCompoundBorder(new LineBorder(COLOR_GOLD, 2), new EmptyBorder(20, 20, 20, 20)));
            JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
            lblTitle.setForeground(Color.CYAN);
            JLabel lblMsg = new JLabel(message, SwingConstants.CENTER);
            lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            lblMsg.setForeground(Color.WHITE);
            JPanel btnPanel = new JPanel(new GridLayout(1, 2, 20, 0));
            btnPanel.setOpaque(false);
            ModernButton b1 = new ModernButton(btnText1, new Color(255, 140, 0));
            b1.addActionListener(e -> {
                dispose();
                callback.accept(0);
            });
            ModernButton b2 = new ModernButton(btnText2, new Color(0, 198, 255));
            b2.addActionListener(e -> {
                dispose();
                callback.accept(1);
            });
            btnPanel.add(b1);
            btnPanel.add(b2);
            ModernButton bCancel = new ModernButton("HỦY BỎ", new Color(100, 100, 100));
            bCancel.addActionListener(e -> dispose());
            p.add(lblTitle);
            p.add(lblMsg);
            p.add(btnPanel);
            p.add(bCancel);
            add(p);
            setVisible(true);
        }
    }

    class CustomInputDialog extends JDialog {
        public CustomInputDialog(JFrame parent, String title, String message,
                java.util.function.Consumer<String> callback) {
            super(parent, title, true);
            setUndecorated(true);
            setSize(400, 250);
            setLocationRelativeTo(parent);
            JPanel p = new JPanel(new GridLayout(4, 1, 10, 10));
            p.setBackground(COLOR_DIALOG_BG);
            p.setBorder(
                    BorderFactory.createCompoundBorder(new LineBorder(COLOR_GOLD, 2), new EmptyBorder(20, 20, 20, 20)));
            JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
            lblTitle.setForeground(Color.GREEN);
            JLabel lblMsg = new JLabel(message, SwingConstants.CENTER);
            lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            lblMsg.setForeground(Color.WHITE);
            JTextField txtInput = new JTextField();
            txtInput.setFont(new Font("Consolas", Font.BOLD, 24));
            txtInput.setHorizontalAlignment(SwingConstants.CENTER);
            txtInput.setBackground(new Color(50, 50, 60));
            txtInput.setForeground(Color.WHITE);
            txtInput.setCaretColor(Color.WHITE);
            txtInput.setBorder(new LineBorder(Color.GRAY));
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnPanel.setOpaque(false);
            ModernButton btnOK = new ModernButton("XÁC NHẬN", new Color(0, 198, 255));
            btnOK.setPreferredSize(new Dimension(150, 40));
            btnOK.addActionListener(e -> {
                dispose();
                callback.accept(txtInput.getText());
            });
            ModernButton btnCancel = new ModernButton("HỦY", new Color(100, 100, 100));
            btnCancel.setPreferredSize(new Dimension(100, 40));
            btnCancel.addActionListener(e -> dispose());
            btnPanel.add(btnOK);
            btnPanel.add(btnCancel);
            p.add(lblTitle);
            p.add(lblMsg);
            p.add(txtInput);
            p.add(btnPanel);
            add(p);
            setVisible(true);
        }
    }

    class CustomInfoDialog extends JDialog {
        public CustomInfoDialog(JFrame parent, String title, String message, String code, Runnable onOK) {
            super(parent, title, true);
            setUndecorated(true);
            setSize(400, 280);
            setLocationRelativeTo(parent);
            JPanel p = new JPanel(new GridLayout(4, 1, 5, 5));
            p.setBackground(COLOR_DIALOG_BG);
            p.setBorder(
                    BorderFactory.createCompoundBorder(new LineBorder(COLOR_GOLD, 2), new EmptyBorder(20, 20, 20, 20)));
            JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
            lblTitle.setForeground(Color.MAGENTA);
            JLabel lblMsg = new JLabel(message, SwingConstants.CENTER);
            lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            lblMsg.setForeground(Color.WHITE);
            JTextField txtCode = new JTextField(code);
            txtCode.setFont(new Font("Consolas", Font.BOLD, 40));
            txtCode.setHorizontalAlignment(SwingConstants.CENTER);
            txtCode.setBackground(COLOR_DIALOG_BG);
            txtCode.setForeground(Color.YELLOW);
            txtCode.setBorder(null);
            txtCode.setEditable(false);
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnPanel.setOpaque(false);
            ModernButton btnOK = new ModernButton("VÀO PHÒNG", new Color(0, 198, 255));
            btnOK.setPreferredSize(new Dimension(150, 40));
            btnOK.addActionListener(e -> {
                dispose();
                onOK.run();
            });
            ModernButton btnCancel = new ModernButton("HỦY", new Color(100, 100, 100));
            btnCancel.setPreferredSize(new Dimension(100, 40));
            btnCancel.addActionListener(e -> dispose());
            btnPanel.add(btnOK);
            btnPanel.add(btnCancel);
            p.add(lblTitle);
            p.add(lblMsg);
            p.add(txtCode);
            p.add(btnPanel);
            add(p);
            setVisible(true);
        }
    }
}