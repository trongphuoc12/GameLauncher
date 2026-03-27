package client;

public class DotsBoard {
    // Kích thước lưới điểm (10 chấm x 10 chấm -> 10x10 ô vuông)
    public final int ROWS = 6; 
    public final int COLS = 6;
    
    // Mảng lưu trạng thái các đường kẻ (true = đã kẻ)
    // hLines[i][j]: Dòng kẻ NGANG ở hàng i, cột j
    // vLines[i][j]: Dòng kẻ DỌC ở hàng i, cột j
    private boolean[][] hLines;
    private boolean[][] vLines;
    
    // Mảng lưu người sở hữu ô vuông (0: chưa ai, 1: P1, 2: P2)
    private int[][] boxes;
    
    public DotsBoard() {
        hLines = new boolean[ROWS + 1][COLS];
        vLines = new boolean[ROWS][COLS + 1];
        boxes = new int[ROWS][COLS];
    }
    
    // Thêm đường kẻ ngang
    public boolean addHLine(int r, int c) {
        if (hLines[r][c]) return false; // Đã kẻ rồi
        hLines[r][c] = true;
        return true;
    }

    // Thêm đường kẻ dọc
    public boolean addVLine(int r, int c) {
        if (vLines[r][c]) return false; // Đã kẻ rồi
        vLines[r][c] = true;
        return true;
    }
    
    // Kiểm tra xem nước đi vừa rồi có đóng kín ô vuông nào không
    // Trả về: true nếu có ít nhất 1 ô được đóng (để được đi tiếp)
    public boolean checkBoxes(int playerNum) {
        boolean madeBox = false;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (boxes[i][j] == 0) { // Nếu ô này chưa ai sở hữu
                    // Kiểm tra 4 cạnh xung quanh ô (i,j)
                    boolean top = hLines[i][j];
                    boolean bot = hLines[i + 1][j];
                    boolean left = vLines[i][j];
                    boolean right = vLines[i][j + 1];
                    
                    if (top && bot && left && right) {
                        boxes[i][j] = playerNum; // Đánh dấu chủ quyền
                        madeBox = true;
                    }
                }
            }
        }
        return madeBox;
    }
    
    public void setBoxOwner(int r, int c, int owner) {
            boxes[r][c] = owner;
    }

    // Getter để bên Giao diện lấy dữ liệu vẽ
    public boolean isHLineSet(int r, int c) { return hLines[r][c]; }
    public boolean isVLineSet(int r, int c) { return vLines[r][c]; }
    public int getBoxOwner(int r, int c) { return boxes[r][c]; }
    
    public boolean isFull() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (boxes[i][j] == 0) return false;
            }
        }
        return true;
    }

    // Đếm số ô P1 đã ăn
    public int getP1Score() {
        int score = 0;
        for(int i=0; i<ROWS; i++) {
            for(int j=0; j<COLS; j++) {
                if (boxes[i][j] == 1) score++;
            }
        }
        return score;
    }

    // Đếm số ô P2 đã ăn
    public int getP2Score() {
        int score = 0;
        for(int i=0; i<ROWS; i++) {
            for(int j=0; j<COLS; j++) {
                if (boxes[i][j] == 2) score++;
            }
        }
        return score;
    }
}