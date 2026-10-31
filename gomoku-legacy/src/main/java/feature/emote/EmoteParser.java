package feature.emote;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmoteParser {
    private static final Pattern PATTERN = Pattern.compile("^\\[emote:([a-z]+)\\](.*)$");

    private EmoteParser() {}

    public static Optional<Emote> parse(String content) {
        if (content == null) return Optional.empty();
        Matcher m = PATTERN.matcher(content);
        if (!m.matches()) return Optional.empty();
        String code = m.group(1);
        Emote e = EmotePack.byCode(code);
        return e == null ? Optional.empty() : Optional.of(e);
    }

    public static String wrap(Emote emote) {
        if (emote == null) return "";
        return "[emote:" + emote.code + "]" + emote.text;
    }
}
