package feature.skin;

import java.awt.Color;
import java.awt.Font;
import ui.design.BoardStyle;
import ui.design.Fonts;
import ui.design.PieceStyle;
import ui.design.Skin;
import ui.design.Tokens;

public final class NeonSkin {
    public static final Skin INSTANCE = build();

    private NeonSkin() {}

    private static Skin build() {
        Tokens.Builder b = Tokens.builder();
        b.bgPrimary    = new Color(0x0F0F1E);
        b.bgSecondary  = new Color(0xEDD9B0);
        b.bgCard       = new Color(0xFAF6EE);
        b.bgBoard      = new Color(0xD4A373);
        b.bgToolbar    = new Color(0xE8D5B7);

        b.fgDefault    = new Color(0x3A2418);
        b.fgMuted      = new Color(0x8A6D4F);
        b.fgOnAccent   = new Color(0xFAF6EE);

        b.accent       = new Color(0x00FFAA);
        b.accentDark   = new Color(0x3E2723);
        b.danger       = new Color(0xA52A2A);
        b.success      = new Color(0x558B2F);
        b.warning      = new Color(0xE65100);

        b.boardLine    = new Color(0xE0D2B8);
        b.boardStar    = new Color(0x3E2723);
        b.boardShadow  = new Color(0, 0, 0, 80);
        b.lastMoveRing = new Color(0xA52A2A);

        b.borderLight  = new Color(0xD7C9B0);
        b.borderMedium = new Color(0xA88B6A);

        b.fontDisplay  = Fonts.serif(30, Font.BOLD);
        b.fontTitle    = Fonts.serif(20, Font.BOLD);
        b.fontHeading  = Fonts.serif(14, Font.BOLD);
        b.fontBody     = Fonts.serif(13, Font.PLAIN);
        b.fontCaption  = Fonts.serif(11, Font.PLAIN);
        b.fontMono     = Fonts.mono(13, Font.PLAIN);

        Tokens tokens = b.build();

        BoardStyle board = new BoardStyle(
            new Color(0xE9C892),
            new Color(0xC9A06A),
            true,
            6,
            1f / 24f
        );

        PieceStyle pieces = new PieceStyle(
            new Color(0x888888),
            new Color(0x000000),
            new Color(0xFFFFFF),
            new Color(0xAAAAAA),
            true
        );

        return new Skin("neon", "霓虹竞技", true, 2500, tokens, board, pieces);
    }
}
