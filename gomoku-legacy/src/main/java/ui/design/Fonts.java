// 字体选择：自动选择系统中最优的中文字体 / 等宽字体 / 衬线（B 温暖怀旧风用衬线）
package ui.design;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Fonts {
    private static final Set<String> AVAILABLE;
    static {
        AVAILABLE = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    }

    private Fonts() {}

    public static Font sans(int size, int style) {
        return pick(new String[]{
            "Microsoft YaHei UI", "Microsoft YaHei",
            "PingFang SC", "Hiragino Sans GB",
            "Noto Sans CJK SC", "WenQuanYi Micro Hei",
            "SimHei", "SimSun", "Dialog"
        }, size, style);
    }

    public static Font serif(int size, int style) {
        return pick(new String[]{
            "STSong", "SimSun", "FangSong", "STKaiti", "KaiTi",
            "Microsoft YaHei UI", "Microsoft YaHei",
            "PingFang SC", "Hiragino Sans GB", "Noto Sans CJK SC",
            "Georgia", "Times New Roman", "Serif"
        }, size, style);
    }

    public static Font mono(int size, int style) {
        return pick(new String[]{
            "Consolas", "Cascadia Mono", "Menlo",
            "DejaVu Sans Mono",
            "Microsoft YaHei UI", "Microsoft YaHei",
            Font.MONOSPACED
        }, size, style);
    }

    private static Font pick(String[] candidates, int size, int style) {
        for (String c : candidates) {
            if (AVAILABLE.contains(c)) return new Font(c, style, size);
        }
        return new Font(Font.DIALOG, style, size);
    }
}
