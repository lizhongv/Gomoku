package ui.component;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class PlayerCard extends GCard implements ThemeAware {
    private final GAvatar avatar;
    private final JLabel nameLabel;
    private final GChip rankChip;
    private final JLabel timerLabel;
    private boolean active;
    private boolean isBlack;

    public PlayerCard(String name, boolean isBlack, String rank) {
        super(false);
        this.isBlack = isBlack;
        Tokens t = Theme.tokens();
        setLayout(new BorderLayout(t.space3, 0));

        avatar = new GAvatar(48, isBlack);
        add(avatar, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        nameLabel = new JLabel(name == null ? "" : name);
        nameLabel.setFont(t.fontBody);
        nameLabel.setForeground(t.fgDefault);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rankChip = new GChip(rank == null ? "" : rank, t.accent, t.fgOnAccent);
        JPanel rankRow = new JPanel(new BorderLayout());
        rankRow.setOpaque(false);
        rankRow.add(rankChip, BorderLayout.WEST);
        rankRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        timerLabel = new JLabel("05:00");
        timerLabel.setFont(t.fontMono);
        timerLabel.setForeground(t.fgMuted);
        timerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        center.add(nameLabel);
        center.add(Box.createVerticalStrut(t.space1));
        center.add(rankRow);
        center.add(Box.createVerticalStrut(t.space1));
        center.add(timerLabel);

        add(center, BorderLayout.CENTER);
        applyBorder();
    }

    public void setActive(boolean v) {
        this.active = v;
        applyBorder();
        repaint();
    }

    public void setTimer(int seconds) {
        if (seconds < 0) seconds = 0;
        int m = seconds / 60;
        int s = seconds % 60;
        timerLabel.setText(String.format("%02d:%02d", m, s));
    }

    public void setName(String name) {
        nameLabel.setText(name == null ? "" : name);
    }

    public void setRank(String rank) {
        rankChip.setText(rank == null ? "" : rank);
    }

    public void setIsBlack(boolean v) {
        this.isBlack = v;
        avatar.setBlack(v);
    }

    private void applyBorder() {
        Tokens t = Theme.tokens();
        if (active) {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(t.accent, 2),
                BorderFactory.createEmptyBorder(t.space3, t.space3, t.space3, t.space3)));
        } else {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(t.borderLight, 1),
                BorderFactory.createEmptyBorder(t.space3 + 1, t.space3 + 1, t.space3 + 1, t.space3 + 1)));
        }
    }

    @Override
    public void onSkinChanged() {
        Tokens t = Theme.tokens();
        nameLabel.setFont(t.fontBody);
        nameLabel.setForeground(t.fgDefault);
        timerLabel.setFont(t.fontMono);
        timerLabel.setForeground(t.fgMuted);
        applyBorder();
        super.onSkinChanged();
    }
}
