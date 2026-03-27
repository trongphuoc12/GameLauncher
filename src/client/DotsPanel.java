package client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JPanel;

public class DotsPanel extends JPanel {
    
    public DotsBoard board; // Public để Client lấy điểm số
    private DotsAndBoxesClient parent; 
    private boolean isMyTurn = false; 
    
    // Cấu hình kích thước
    private final int DOT_SIZE = 10;   // Chấm to hơn
    private final int SPACING = 50;    // Khoảng cách vừa phải
    private final int OFFSET = 50;     // Lề rộng hơn cho thoáng
    
    // Biến cho hiệu ứng Hover (Bóng ma)
    private int hoverR = -1, hoverC = -1;
    private String hoverType = null; // "H" hoặc "V" hoặc null

    // 2. Bảng màu Cyberpunk Pastel (Dịu mắt hơn)
    // Nền: Xanh đen thẫm (Deep Navy) thay vì xám đen
    private final Color COLOR_BG = new Color(20, 25, 40);       
    
    // Chấm: Xám xanh nhạt (thay vì trắng bệch)
    private final Color COLOR_DOT = new Color(60, 70, 90);   
    
    // P1 (Xanh): Dùng màu Electric Blue sáng
    private final Color COLOR_LINE_P1 = new Color(0, 200, 255); 
    // Tăng độ đậm đặc (Alpha) lên 100 để màu không bị xỉn khi tô
    private final Color COLOR_BOX_P1 = new Color(0, 200, 255, 80); 

    // P2 (Hồng): Dùng màu Hot Pink rực rỡ hơn
    private final Color COLOR_LINE_P2 = new Color(255, 50, 150); 
    private final Color COLOR_BOX_P2 = new Color(255, 50, 150, 80); 

    // Hover: Màu trắng sáng
    private final Color COLOR_HOVER = new Color(255, 255, 255, 150);

    public DotsPanel(DotsAndBoxesClient parent) { 
        this.parent = parent; 
        this.board = new DotsBoard();
        setBackground(COLOR_BG);
        
        int width = (OFFSET * 2) + (board.COLS * SPACING);
        int height = (OFFSET * 2) + (board.ROWS * SPACING);
        setPreferredSize(new Dimension(width, height));
        
        // Xử lý Click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isMyTurn) handleClick(e.getX(), e.getY());
            }
        });

        // Xử lý Di chuột (Hover)
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isMyTurn) handleHover(e.getX(), e.getY());
                else resetHover(); // Không phải lượt thì không hiện hover
            }
        });
    }

    public void setMyTurn(boolean turn) {
        this.isMyTurn = turn;
        if (!turn) resetHover(); // Mất lượt thì xóa bóng ma đi
        repaint();
    }

    private void resetHover() {
        hoverType = null;
        repaint();
    }

    public void remoteAddLine(String type, int r, int c) {
        if (type.equals("H")) board.addHLine(r, c);
        else board.addVLine(r, c);
        repaint();
    }
    
    public void updateBox(int r, int c, int owner) {
        board.setBoxOwner(r, c, owner); 
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Vẽ Ô VUÔNG (Box)
        for (int i = 0; i < board.ROWS; i++) {
            for (int j = 0; j < board.COLS; j++) {
                int owner = board.getBoxOwner(i, j);
                if (owner != 0) {
                    g2.setColor(owner == 1 ? COLOR_BOX_P1 : COLOR_BOX_P2);
                    g2.fillRoundRect(OFFSET + j * SPACING + DOT_SIZE/2, 
                                     OFFSET + i * SPACING + DOT_SIZE/2, 
                                     SPACING, SPACING, 10, 10); // Bo góc nhẹ
                }
            }
        }

        // 2. Vẽ ĐƯỜNG KẺ (Line)
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Nét tròn đầu
        g2.setColor(Color.GRAY); // Màu mặc định (nếu cần)

        // Ngang
        for (int i = 0; i <= board.ROWS; i++) {
            for (int j = 0; j < board.COLS; j++) {
                if (board.isHLineSet(i, j)) {
                    g2.setColor(Color.WHITE); // Hoặc phân màu theo người chơi nếu muốn lưu history
                    g2.drawLine(OFFSET + j * SPACING + DOT_SIZE/2, OFFSET + i * SPACING + DOT_SIZE/2,
                                OFFSET + (j + 1) * SPACING + DOT_SIZE/2, OFFSET + i * SPACING + DOT_SIZE/2);
                }
            }
        }
        // Dọc
        for (int i = 0; i < board.ROWS; i++) {
            for (int j = 0; j <= board.COLS; j++) {
                if (board.isVLineSet(i, j)) {
                    g2.setColor(Color.WHITE);
                    g2.drawLine(OFFSET + j * SPACING + DOT_SIZE/2, OFFSET + i * SPACING + DOT_SIZE/2,
                                OFFSET + j * SPACING + DOT_SIZE/2, OFFSET + (i + 1) * SPACING + DOT_SIZE/2);
                }
            }
        }

        // 3. Vẽ HIỆU ỨNG HOVER (Bóng ma)
        if (hoverType != null && isMyTurn) {
            g2.setColor(COLOR_HOVER);
            g2.setStroke(new BasicStroke(4)); // Nét mờ bằng nét thật
            if (hoverType.equals("H")) {
                g2.drawLine(OFFSET + hoverC * SPACING + DOT_SIZE/2, OFFSET + hoverR * SPACING + DOT_SIZE/2,
                            OFFSET + (hoverC + 1) * SPACING + DOT_SIZE/2, OFFSET + hoverR * SPACING + DOT_SIZE/2);
            } else {
                g2.drawLine(OFFSET + hoverC * SPACING + DOT_SIZE/2, OFFSET + hoverR * SPACING + DOT_SIZE/2,
                            OFFSET + hoverC * SPACING + DOT_SIZE/2, OFFSET + (hoverR + 1) * SPACING + DOT_SIZE/2);
            }
        }

        // 4. Vẽ CHẤM TRÒN (Dots)
        g2.setColor(COLOR_DOT);
        for (int i = 0; i <= board.ROWS; i++) {
            for (int j = 0; j <= board.COLS; j++) {
                g2.fillOval(OFFSET + j * SPACING, OFFSET + i * SPACING, DOT_SIZE, DOT_SIZE);
            }
        }
    }

    // Logic tìm vị trí (Dùng chung cho cả Click và Hover)
    private void processMouse(int x, int y, boolean isClick) {
        x -= OFFSET;
        y -= OFFSET;
        int tolerance = 15; // Phạm vi bắt dính

        // Check Ngang
        for (int i = 0; i <= board.ROWS; i++) {
            for (int j = 0; j < board.COLS; j++) {
                int lineX = j * SPACING + SPACING/2;
                int lineY = i * SPACING;
                if (Math.abs(x - lineX) < SPACING/2 && Math.abs(y - lineY) < tolerance) {
                    if (!board.isHLineSet(i, j)) {
                        if (isClick) parent.sendMove("H", i, j);
                        else setHover("H", i, j);
                        return;
                    }
                }
            }
        }
        // Check Dọc
        for (int i = 0; i < board.ROWS; i++) {
            for (int j = 0; j <= board.COLS; j++) {
                int lineX = j * SPACING;
                int lineY = i * SPACING + SPACING/2;
                if (Math.abs(x - lineX) < tolerance && Math.abs(y - lineY) < SPACING/2) {
                    if (!board.isVLineSet(i, j)) {
                        if (isClick) parent.sendMove("V", i, j);
                        else setHover("V", i, j);
                        return;
                    }
                }
            }
        }
        
        // Nếu chuột không trúng đâu cả
        if (!isClick) resetHover();
    }

    private void handleClick(int x, int y) { processMouse(x, y, true); }
    private void handleHover(int x, int y) { processMouse(x, y, false); }

    private void setHover(String type, int r, int c) {
        if (!type.equals(hoverType) || r != hoverR || c != hoverC) {
            hoverType = type;
            hoverR = r;
            hoverC = c;
            repaint(); // Chỉ vẽ lại khi vị trí hover thay đổi
        }
    }
}