# NEXUS BATTLEGROUND - Ultimate LAN Gaming

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![TCP/IP](https://img.shields.io/badge/Network-TCP%2FIP-blue?style=for-the-badge)
![Swing](https://img.shields.io/badge/GUI-Java_Swing-green?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Completed-success?style=for-the-badge)

**Nexus Battleground** là một hệ thống Game Launcher tích hợp các tựa game đối kháng trực tuyến thời gian thực (Real-time Multiplayer). Đồ án được xây dựng dựa trên kiến trúc **Client-Server** sử dụng **Java Socket thuần (TCP/IP)**, cho phép người chơi kết nối, giao tiếp và thi đấu với nhau qua mạng LAN hoặc VPN.

---

## Tính năng nổi bật

* **Tích hợp 2 tựa game kinh điển:**
    **Battleship (Bắn Tàu Chiến):** Game chiến thuật theo lượt với sương mù chiến tranh (Fog of War) và đồng hồ đếm ngược.
    **Dots & Boxes (Nối Điểm):** Game trí tuệ thi đấu giành lãnh thổ với hệ thống tính điểm thời gian thực.
* **Hệ thống kết nối mượt mà:** Xử lý đa luồng (Multithreading) giúp Server phục vụ nhiều phòng chơi cùng lúc mà không giật lag.
* **Kênh Chat Real-time:** Tích hợp khung chat trực tiếp trong từng trận đấu.
* **Bảng xếp hạng trực tuyến (Cloud Leaderboard):** Lưu trữ kết quả và hiển thị danh sách TOP người chơi thông qua Google Sheets API.
* **Giao diện hiện đại (Modern UI):** Thiết kế Dark Mode, hiệu ứng Hover, Animation cháy nổ và hệ thống âm thanh sống động.

---

## Công nghệ sử dụng

* **Ngôn ngữ:** Java (JDK 21+)
* **Giao diện:** Java Swing (Custom UI Component)
* **Mạng (Networking):** `java.net.Socket`, TCP/IP, Custom Text-based Protocol.
* **Cơ sở dữ liệu:** Google Apps Script & Google Sheets (RESTful API).
* **Xử lý luồng:** Java `Thread` & `Runnable`.

## Architecture
Hệ thống hoạt động theo mô hình Client-Server:
- **Server:** Chịu trách nhiệm điều phối luồng dữ liệu (Data Orchestration), quản lý trạng thái trận đấu (Game State) thông qua các `PlayerHandler`.
- **Client:** Xử lý giao diện người dùng (Swing UI), âm thanh và gửi các gói tin điều khiển (Custom Packets) về Server.
---

## Hướng dẫn cài đặt & Chạy Game

### Yêu cầu hệ thống:
* Máy tính đã cài đặt **Java Runtime Environment (JRE) 8** hoặc mới hơn.

### Cách khởi chạy:
Cách 1: không terminal
1. Tải về và giải nén thư mục dự án (hoặc thư mục `dist`).
2. Đảm bảo file `.jar` luôn nằm cùng thư mục với `images/` và `sounds/`.
3. Nhấp đúp vào file `PlayGame.bat` (Khuyên dùng - để chạy ẩn Console) hoặc `GameLauncher.jar`.

Cách 2: Với terminal:
# Chạy Game Server chính
java -cp "bin;libs/*" server.GameServer

# Nếu bạn muốn chạy server cờ caro (DotsServer)
java -cp "bin;libs/*" server.DotsServer

# Để chạy Client (Game)
Mở một Tab Terminal mới (nhấn dấu + ở góc Terminal VS Code) để chạy giao diện người chơi:
- java -cp "bin;libs/*" client.GameMenu
---

## Hướng dẫn kết nối (Multiplayer)

Trò chơi hỗ trợ kết nối 2 người chơi thông qua địa chỉ IP. 

* **Trường hợp 1 (Chung mạng WiFi / LAN):**
    * Người tạo phòng (Host) xem `IP Của Bạn` hiện trên màn hình chính và gửi cho bạn bè.
    * Người tham gia (Client) nhập IP đó vào ô `IP Server`, nhập Mã phòng và chiến.
* **Trường hợp 2 (Khác mạng - Chơi qua Internet):**
    * Cả 2 người chơi cài đặt phần mềm [Radmin VPN](https://www.radmin-vpn.com/) (Miễn phí).
    * Tạo chung một Network trên Radmin.
    * Sử dụng IP do Radmin cấp để kết nối trong Game.

---

## Ảnh chụp màn hình (Screenshots)


| Game Menu & Leaderboard | Battleship Gameplay |
| :---: | :---: |
| ![Menu]![alt text](image.png) | ![Battleship]![alt text](image-1.png) |
| **Dots & Boxes Gameplay** | **Match Results & Rematch** |
| ![Dots]![alt text](image-2.png) | !![alt text](image-3.png) |

---

## Tác giả
Đồ án Môn học: Lập trình mạng máy tính
* **Sinh viên thực hiện:** Phạm Long Vũ, Nguyễn Trọng Phước, Hồ Ngọc Trọng
* **Trường:** Trường Đại học Công Nghệ Thành phố Hồ Chí Minh (HUTECH)
