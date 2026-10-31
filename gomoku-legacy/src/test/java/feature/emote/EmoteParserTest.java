package feature.emote;

import java.util.Optional;

public class EmoteParserTest {
    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        testParseValid();
        testParsePlainText();
        testParseUnknownCode();
        testWrap();
        System.out.println("结果: passed=" + passed + " failed=" + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void testParseValid() {
        Optional<Emote> e = EmoteParser.parse("[emote:good]妙手！");
        ok("有效 [emote:good] 解析成功且 code=good",
                e.isPresent() && "good".equals(e.get().code));
    }

    static void testParsePlainText() {
        Optional<Emote> e = EmoteParser.parse("普通文字");
        ok("普通文字解析为 empty", !e.isPresent());
    }

    static void testParseUnknownCode() {
        Optional<Emote> e = EmoteParser.parse("[emote:unknown]xxx");
        ok("未知 code 解析为 empty", !e.isPresent());
    }

    static void testWrap() {
        Emote first = EmotePack.DEFAULT.get(0);
        String wrapped = EmoteParser.wrap(first);
        ok("wrap(good) == [emote:good]妙手！", "[emote:good]妙手！".equals(wrapped));
    }

    static void ok(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  ✓ " + name); }
        else      { failed++; System.out.println("  ✗ " + name); }
    }
}
