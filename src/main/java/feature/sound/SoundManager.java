package feature.sound;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class SoundManager {
    private static final Map<SoundId, Clip> CLIPS = new EnumMap<>(SoundId.class);
    private static boolean enabled = true;
    private static float volume = 0.7f;

    private SoundManager() {}

    public static void preload() {
        for (SoundId id : SoundId.values()) {
            try (InputStream in = SoundManager.class.getResourceAsStream("/sounds/" + id.fileName)) {
                if (in == null) continue;
                AudioInputStream ais = AudioSystem.getAudioInputStream(
                        new java.io.BufferedInputStream(in));
                Clip c = AudioSystem.getClip();
                c.open(ais);
                CLIPS.put(id, c);
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
                // 静默跳过
            } catch (Exception e) {
                // 静默跳过
            }
        }
    }

    public static void play(SoundId id) {
        if (!enabled) return;
        Clip c = CLIPS.get(id);
        if (c == null) return;
        try {
            c.setFramePosition(0);
            try {
                FloatControl gain = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(20f * (float) Math.log10(volume));
            } catch (Exception ignored) {}
            c.start();
        } catch (Exception ignored) {}
    }

    public static void setEnabled(boolean v) { enabled = v; }

    public static void setVolume(int percent) {
        volume = Math.max(0.01f, Math.min(1f, percent / 100f));
    }
}
