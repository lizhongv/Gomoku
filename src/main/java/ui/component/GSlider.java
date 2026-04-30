package ui.component;

import javax.swing.JSlider;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class GSlider extends JSlider implements ThemeAware {
    public GSlider() { super(); applyStyle(); }
    public GSlider(int min, int max) { super(min, max); applyStyle(); }
    public GSlider(int min, int max, int value) { super(min, max, value); applyStyle(); }

    private void applyStyle() {
        Tokens t = Theme.tokens();
        setForeground(t.accent);
        setBackground(t.bgCard);
        setFont(t.fontCaption);
        setOpaque(false);
    }

    @Override
    public void onSkinChanged() {
        applyStyle();
        repaint();
    }
}
