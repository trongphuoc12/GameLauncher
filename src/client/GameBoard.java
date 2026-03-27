package client;

import java.util.ArrayList;
import java.util.List;

public class GameBoard {
    
    // Kích thước bàn cờ (ví dụ 10x10)
    private final int SIZE = 10;
    private int[][] board;

    private List<Ship> ships = new ArrayList<>();

    public static final int EMPTY = 0; // trống
    public static final int SHIP = 1; // tàu
    public static final int MISS = 2; // trượt
    public static final int HIT = 3; // trúng

    public GameBoard() {
        board = new int[SIZE][SIZE];
        // Khởi tạo tất cả là EMPTY
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
    }

    // Hàm đặt tàu (đơn giản, chỉ đặt 1 ô)
    // Bây giờ hàm này sẽ tạo ra đối tượng Ship thật sự và lưu vào list
    public boolean placeShip(int x, int y, int length, boolean isHorizontal) {
        // 1. Kiểm tra biên
        if (isHorizontal) { if (y + length > SIZE) return false; } 
        else { if (x + length > SIZE) return false; }

        // 2. Kiểm tra va chạm
        for (int i = 0; i < length; i++) {
            if (isHorizontal) { if (board[x][y + i] != EMPTY) return false; } 
            else { if (board[x + i][y] != EMPTY) return false; }
        }

        // 3. Đặt tàu lên ma trận số
        for (int i = 0; i < length; i++) {
            if (isHorizontal) board[x][y + i] = SHIP;
            else board[x + i][y] = SHIP;
        }
        
        // 4. --- QUAN TRỌNG: LƯU ĐỐI TƯỢNG TÀU VÀO DANH SÁCH ---
        Ship newShip = new Ship(getShipNameByLength(length), length);
        newShip.setPosition(x, y);
        newShip.isHorizontal = isHorizontal;
        ships.add(newShip);
        
        return true;
    }

    // Hàm phụ trợ lấy tên tàu
    private String getShipNameByLength(int len) {
        switch(len) {
            case 5: return "Carrier (5 ô)";
            case 4: return "Battleship (4 ô)";
            case 3: return "Cruiser/Submarine (3 ô)";
            case 2: return "Destroyer (2 ô)";
            default: return "Tàu lạ";
        }
    }

    // Hàm nhận một phát bắn
    // Trả về: 
    // 0: MISS
    // 1: HIT (nhưng chưa chìm)
    // 2: SINK (bắn trúng và làm chìm tàu)
    public int receiveFire(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return 0; // MISS

        if (board[x][y] == SHIP) {
            board[x][y] = HIT;
            
            // Tìm xem trúng con tàu nào
            Ship hitShip = findShipAt(x, y);
            if (hitShip != null) {
                hitShip.registerHit(); // Trừ máu tàu
                if (hitShip.isSunk()) {
                    return 2; // SINK
                }
            }
            return 1; // HIT
            
        } else if (board[x][y] == EMPTY) {
            board[x][y] = MISS;
            return 0; // MISS
        }
        return 0; // Đã bắn rồi thì coi như trượt
    }

    // Hàm tìm tàu tại tọa độ x,y
    public Ship findShipAt(int x, int y) {
        for (Ship s : ships) {
            if (s.contains(x, y)) return s;
        }
        return null;
    }

    public boolean isShipSunkAt(int x, int y) {
        Ship s = findShipAt(x, y);
        // Trả về true nếu ô này có tàu VÀ con tàu đó đã bị bắn chìm hoàn toàn
        return (s != null && s.isSunk());
    }
    
    // Hàm kiểm tra xem đã hết tàu chưa
    public boolean hasLost() {
        // Logic cũ: duyệt mảng. Logic mới: kiểm tra list tàu
        for (Ship s : ships) {
            if (!s.isSunk()) return false; // Còn tàu sống
        }
        return true;
    }

    public void removeShip(int x, int y, int length, boolean isHorizontal) {
        for (int i = 0; i < length; i++) {
            int currentX = x;
            int currentY = y;

            if (isHorizontal) {
                currentY += i;
            } else {
                currentX += i;
            }

            // Chỉ xóa nếu nó nằm trong biên và là TÀU
            if (currentX < SIZE && currentY < SIZE && board[currentX][currentY] == SHIP) {
                board[currentX][currentY] = EMPTY;
            }
        }
    }

    // (Tùy chọn) Hàm in bàn cờ ra console để debug
    public void printBoard() {
        System.out.println("  0 1 2 3 4 5 6 7 8 9");
        for (int i = 0; i < SIZE; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < SIZE; j++) {
                switch (board[i][j]) {
                    case EMPTY: System.out.print("~ "); break; // Nước
                    case SHIP: System.out.print("S "); break; // Tàu (debug)
                    case MISS: System.out.print("O "); break; // Trượt
                    case HIT: System.out.print("X "); break; // Trúng
                }
            }
            System.out.println();
        }
    }
}