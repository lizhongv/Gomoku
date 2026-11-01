package io.github.lizhongv.gomoku.desktop;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * 音效：优先使用资源包中的 wav，缺失时静默失败。
 *
 * <p>Clip 数量少、生命周期与应用一致；关闭时统一释放，避免长时间运行后句柄泄漏。
 */
public final class SoundPlayer implements AutoCloseable {

  private final List<Clip> clips = new ArrayList<>();

  private boolean enabled = true;

  /** 设置是否播放。 */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** 播放一个音效：优先用内置资源，其次用户目录；都没有时静默。 */
  public void play(String name) {
    if (!enabled) {
      return;
    }
    InputStream stream = SoundPlayer.class.getResourceAsStream("/sounds/" + name + ".wav");
    Path path = AppContext.dataDir().resolve("sounds").resolve(name + ".wav");
    try {
      if (stream == null) {
        if (!Files.exists(path)) {
          return;
        }
        stream = Files.newInputStream(path);
      }
      try (InputStream raw = stream;
          AudioInputStream in = AudioSystem.getAudioInputStream(raw)) {
        Clip clip = AudioSystem.getClip();
        clip.open(in);
        clip.start();
        clips.add(clip);
        // 播放完成后回收，避免长期运行累积
        clip.addLineListener(
            event -> {
              if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                clip.close();
                clips.remove(clip);
              }
            });
      }
    } catch (Exception ignored) {
      // 解码失败时保持静音，不影响对局
    }
  }

  @Override
  public void close() {
    for (Clip clip : List.copyOf(clips)) {
      clip.close();
    }
    clips.clear();
  }
}
