package ui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class ChatPanel extends JPanel implements ThemeAware {
    private final JTextPane pane;
    private final StyledDocument doc;
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

    public ChatPanel() {
        super(new BorderLayout());
        setOpaque(false);
        Tokens t = Theme.tokens();

        pane = new JTextPane();
        pane.setEditable(false);
        pane.setFont(t.fontBody);
        pane.setBackground(t.bgCard);
        pane.setForeground(t.fgDefault);
        pane.setBorder(BorderFactory.createEmptyBorder(t.space3, t.space3, t.space3, t.space3));
        doc = pane.getStyledDocument();

        JScrollPane sp = new JScrollPane(pane);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());

        setBorder(BorderFactory.createEmptyBorder(t.space2, t.space2, t.space2, t.space2));
        add(sp, BorderLayout.CENTER);
    }

    public void appendInfo(String msg) {
        Tokens t = Theme.tokens();
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, t.fgMuted);
        StyleConstants.setFontFamily(a, t.fontBody.getFamily());
        StyleConstants.setFontSize(a, t.fontBody.getSize());
        appendStyled("[" + fmt.format(new Date()) + "] " + msg + "\n", a);
    }

    public void appendOk(String msg) {
        Tokens t = Theme.tokens();
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, t.success);
        StyleConstants.setBold(a, true);
        StyleConstants.setFontFamily(a, t.fontBody.getFamily());
        StyleConstants.setFontSize(a, t.fontBody.getSize());
        appendStyled("[" + fmt.format(new Date()) + "] ✓ " + msg + "\n", a);
    }

    public void appendError(String msg) {
        Tokens t = Theme.tokens();
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, t.danger);
        StyleConstants.setFontFamily(a, t.fontBody.getFamily());
        StyleConstants.setFontSize(a, t.fontBody.getSize());
        appendStyled("[" + fmt.format(new Date()) + "] ⚠ " + msg + "\n", a);
    }

    public void appendChat(String name, String content, boolean self) {
        Tokens t = Theme.tokens();
        String stamp = "[" + fmt.format(new Date()) + "] ";

        SimpleAttributeSet stampAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(stampAttr, t.fgMuted);
        StyleConstants.setFontFamily(stampAttr, t.fontBody.getFamily());
        StyleConstants.setFontSize(stampAttr, t.fontBody.getSize());
        appendStyled(stamp, stampAttr);

        SimpleAttributeSet nameAttr = new SimpleAttributeSet();
        Color nameColor = self ? t.success : t.accent;
        StyleConstants.setForeground(nameAttr, nameColor);
        StyleConstants.setBold(nameAttr, true);
        StyleConstants.setFontFamily(nameAttr, t.fontBody.getFamily());
        StyleConstants.setFontSize(nameAttr, t.fontBody.getSize());
        appendStyled(name + ": ", nameAttr);

        SimpleAttributeSet bodyAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(bodyAttr, t.fgDefault);
        StyleConstants.setFontFamily(bodyAttr, t.fontBody.getFamily());
        StyleConstants.setFontSize(bodyAttr, t.fontBody.getSize());
        appendStyled(content + "\n", bodyAttr);
    }

    public void clear() {
        try { doc.remove(0, doc.getLength()); }
        catch (BadLocationException ignored) {}
    }

    private void appendStyled(String text, SimpleAttributeSet attr) {
        try {
            doc.insertString(doc.getLength(), text, attr);
            pane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {}
    }

    @Override
    protected void paintComponent(Graphics g) {
        Tokens t = Theme.tokens();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = t.radiusLg;
        g2.setColor(t.bgCard);
        g2.fillRoundRect(0, 0, w, h, r, r);
        g2.setColor(t.borderLight);
        g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public void onSkinChanged() {
        Tokens t = Theme.tokens();
        pane.setBackground(t.bgCard);
        pane.setForeground(t.fgDefault);
        pane.setFont(t.fontBody);
        repaint();
    }
}
