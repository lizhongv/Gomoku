package ui.component;

import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class GTab extends JPanel implements ThemeAware {
    private final List<JLabel> labels = new ArrayList<>();
    private int selected = 0;
    private IntConsumer onSelect;

    public GTab(String... titles) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        Tokens t = Theme.tokens();
        for (int i = 0; i < titles.length; i++) {
            final int idx = i;
            JLabel lab = new JLabel(titles[i]);
            lab.setFont(t.fontBody);
            lab.setForeground(t.fgDefault);
            lab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lab.setBorder(BorderFactory.createEmptyBorder(t.space2, t.space4, t.space3, t.space4));
            lab.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { select(idx); }
            });
            labels.add(lab);
            add(lab);
        }
        applyForegrounds();
    }

    public void onSelect(IntConsumer cb) { this.onSelect = cb; }

    public void select(int index) {
        if (index < 0 || index >= labels.size()) return;
        selected = index;
        applyForegrounds();
        repaint();
        if (onSelect != null) onSelect.accept(index);
    }

    public int selectedIndex() { return selected; }

    private void applyForegrounds() {
        Tokens t = Theme.tokens();
        for (int i = 0; i < labels.size(); i++) {
            labels.get(i).setForeground(i == selected ? t.accent : t.fgMuted);
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (selected < 0 || selected >= labels.size()) return;
        Tokens t = Theme.tokens();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle r = labels.get(selected).getBounds();
        g2.setColor(t.accent);
        int barH = 3;
        g2.fillRoundRect(r.x + t.space2, r.y + r.height - barH, r.width - t.space2 * 2, barH, barH, barH);
        g2.dispose();
    }

    @Override
    public void onSkinChanged() {
        Tokens t = Theme.tokens();
        for (JLabel lab : labels) lab.setFont(t.fontBody);
        applyForegrounds();
        repaint();
    }
}
