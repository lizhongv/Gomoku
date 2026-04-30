package tools;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SoundGenerator {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/resources/sounds");
        dir.mkdirs();
        write(new File(dir, "place.wav"),  tone(800, 0.10, 0.6));
        write(new File(dir, "click.wav"),  tone(500, 0.04, 0.5));
        write(new File(dir, "emote.wav"),  tone(1200, 0.08, 0.4));
        write(new File(dir, "invite.wav"), chord(new int[]{1000, 1500}, 0.30, 0.4));
        write(new File(dir, "win.wav"),    chord(new int[]{523, 659, 784}, 0.60, 0.5));
        write(new File(dir, "lose.wav"),   chord(new int[]{440, 415}, 0.50, 0.5));
        System.out.println("Generated 6 sounds in " + dir.getAbsolutePath());
    }

    static byte[] tone(double freq, double seconds, double amp) {
        return synth(new double[]{freq}, seconds, amp);
    }

    static byte[] chord(int[] freqs, double seconds, double amp) {
        double[] f = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++) f[i] = freqs[i];
        return synth(f, seconds, amp);
    }

    static byte[] synth(double[] freqs, double seconds, double amp) {
        int sr = 44100;
        int n = (int) (sr * seconds);
        ByteBuffer bb = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN);
        double atk = Math.min(0.01, seconds * 0.1);
        double rel = Math.min(0.05, seconds * 0.4);
        for (int i = 0; i < n; i++) {
            double t = (double) i / sr;
            double sample = 0;
            for (double f : freqs) sample += Math.sin(2 * Math.PI * f * t);
            sample /= freqs.length;
            double env = 1.0;
            if (t < atk) env = t / atk;
            else if (t > seconds - rel) env = (seconds - t) / rel;
            short s = (short) (sample * amp * env * 32767);
            bb.putShort(s);
        }
        return bb.array();
    }

    static void write(File f, byte[] pcm) throws IOException {
        AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
        AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(pcm), fmt, pcm.length / 2);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, f);
    }
}
