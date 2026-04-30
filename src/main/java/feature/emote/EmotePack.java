package feature.emote;

import java.util.List;

public final class EmotePack {
    public static final List<Emote> DEFAULT = List.of(
        new Emote("good", "妙手！"),
        new Emote("nice", "好棋！"),
        new Emote("hello", "你好"),
        new Emote("haha", "哈哈"),
        new Emote("think", "让我想想"),
        new Emote("oops", "失误了"),
        new Emote("gg", "认输"),
        new Emote("bye", "再见")
    );

    public static Emote byCode(String code) {
        for (Emote e : DEFAULT) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }

    private EmotePack() {}
}
