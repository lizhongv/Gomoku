package ui.component;

import java.awt.GridLayout;
import java.util.function.Consumer;
import feature.emote.Emote;
import feature.emote.EmotePack;
import ui.Theme;
import ui.design.Tokens;

public class EmoteBar extends GCard {
    public EmoteBar(Consumer<Emote> onPick) {
        super(false);
        Tokens t = Theme.tokens();
        setLayout(new GridLayout(2, 4, t.space2, t.space2));
        for (Emote e : EmotePack.DEFAULT) {
            GButton b = GButton.secondary(e.text);
            b.addActionListener(ev -> {
                if (onPick != null) onPick.accept(e);
            });
            add(b);
        }
    }
}
