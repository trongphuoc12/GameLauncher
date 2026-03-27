package client;
import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundManager {

    // --- HAI BIẾN ĐIỀU KHIỂN RIÊNG BIỆT (Mặc định là BẬT) ---
    public static boolean isMusicEnabled = true;
    public static boolean isSFXEnabled = true;
    // ---------------------------------------------------------

    private Clip backgroundMusic;

    public SoundManager() {
        // Không tự động load nhạc nữa, đợi lệnh playMusic(tên) mới load
    }

    // --- HÀM PHÁT NHẠC (CÓ THAM SỐ TÊN FILE) ---
    public void playMusic(String fileName) {
        if (!isMusicEnabled) return; 
        
        stopMusic(); 

        try {
            File soundFile = new File("resources/sounds/" + fileName);
            if (!soundFile.exists()) {
                System.err.println("SoundManager: Không tìm thấy file " + fileName);
                return;
            }
            
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioIn);
            
            // Thêm try-catch cho đoạn chỉnh volume vì một số máy không hỗ trợ
            try {
                FloatControl gainControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-10.0f); 
            } catch (Exception volEx) { /* Bỏ qua nếu không chỉnh được volume */ }
            
            backgroundMusic.setFramePosition(0);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY); 
            backgroundMusic.start();
            
        } catch (UnsupportedAudioFileException e) {
            // ===> BẮT LỖI ĐỊNH DẠNG FILE <===
            System.err.println("Lỗi Âm Thanh: File " + fileName + " sai định dạng! Hãy dùng WAV 16-bit.");
            // Không throw exception nữa để game không bị crash
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    public void stopMusic() {
        if (backgroundMusic != null) {
            if (backgroundMusic.isRunning()) backgroundMusic.stop();
            backgroundMusic.close();
            backgroundMusic = null;
        }
    }

    // --- PHẦN HIỆU ỨNG (SFX) ---
    public void playHit() {
        if (!isSFXEnabled) return; 
        playEffect("resources/sounds/hit.wav");
    }

    public void playMiss() {
        if (!isSFXEnabled) return; 
        playEffect("resources/sounds/miss.wav");
    }
    
    // Thêm các âm thanh cho Dots and Boxes
    public void playDraw() { 
        if(isSFXEnabled) playEffect("resources/sounds/draw.wav"); 
    }
    public void playScore() { 
        if(isSFXEnabled) playEffect("resources/sounds/score.wav"); 
    }
    public void playWin() { 
        if(isSFXEnabled) playEffect("resources/sounds/win.wav"); 
    }
    public void playLose() { 
        if(isSFXEnabled) playEffect("sounds/lose.wav"); 
    }

    private void playEffect(String fileName) {
        new Thread(() -> {
            try {
                File soundFile = new File(fileName); // fileName đã bao gồm đường dẫn "sounds/..."
                if (!soundFile.exists()) return;
                
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
                
                // Tự đóng clip khi chạy xong để giải phóng RAM
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (Exception e) {}
        }).start();
    }
}