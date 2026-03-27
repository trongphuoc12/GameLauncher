// File: AudioTester.java (CÔNG CỤ KIỂM TRA LOA)
import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioTester {
    public static void main(String[] args) {
        System.out.println(">>> BẮT ĐẦU KIỂM TRA ÂM THANH <<<");
        
        // Danh sách các file cần test
        String[] files = {
            // "sounds/win.wav", 
            "sounds/lose.wav", 
            "sounds/draw.wav", 
            "sounds/score.wav"
        };

        for (String filename : files) {
            testFile(filename);
        }
    }

    public static void testFile(String filePath) {
        System.out.println("\n---------------------------------");
        System.out.println("Dang kiem tra: " + filePath);
        
        File f = new File(filePath);
        if (!f.exists()) {
            System.err.println("❌ LỖI: Không tìm thấy file này! (Kiểm tra lại tên file/thư mục)");
            return;
        }
        System.out.println("✅ Đã tìm thấy file. Kích thước: " + f.length() + " bytes");

        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(f);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            
            System.out.println("▶️ Đang phát thử (3 giây)...");
            clip.start();
            
            // Giữ chương trình chạy 3 giây để nghe
            Thread.sleep(3000); 
            
            clip.close();
            System.out.println("✅ File OK: Định dạng chuẩn Java.");
            
        } catch (UnsupportedAudioFileException e) {
            System.err.println("❌ LỖI ĐỊNH DẠNG: File này không phải WAV chuẩn PCM 16-bit.");
            System.err.println("   -> Java không đọc được MP3 đổi đuôi đâu nhé!");
        } catch (Exception e) {
            System.err.println("❌ LỖI KHÁC: " + e.getMessage());
            e.printStackTrace();
        }
    }
}