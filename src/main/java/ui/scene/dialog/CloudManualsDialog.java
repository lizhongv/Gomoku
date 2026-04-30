package ui.scene.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import game.Game;
import ui.Theme;
import ui.component.GButton;
import ui.component.GDialog;
import ui.design.Tokens;

public class CloudManualsDialog extends GDialog {
    private final DefaultListModel<Game> model = new DefaultListModel<>();
    private final JList<Game> list;
    private final Consumer<Game> onSelect;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");

    public CloudManualsDialog(java.awt.Window owner, Consumer<Game> onSelect) {
        super(owner, "云端棋谱", true);
        this.onSelect = onSelect;
        Tokens t = Theme.tokens();

        list = new JList<>(model);
        list.setCellRenderer(new GameRenderer());
        list.setBackground(t.bgCard);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(420, 280));
        sp.setBorder(BorderFactory.createLineBorder(t.borderLight));

        JLabel hint = new JLabel("加载中...");
        hint.setFont(t.fontBody);
        hint.setForeground(t.fgMuted);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, t.space2, 0));

        content.add(hint, BorderLayout.NORTH);
        content.add(sp, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, t.space2, 0));
        buttons.setOpaque(false);
        GButton view = GButton.primary("查看");
        view.addActionListener(e -> {
            Game g = list.getSelectedValue();
            if (g == null) return;
            if (onSelect != null) onSelect.accept(g);
        });
        GButton close = GButton.ghost("关闭");
        close.addActionListener(e -> dispose());
        buttons.add(view);
        buttons.add(close);
        content.add(buttons, BorderLayout.SOUTH);

        setSize(new Dimension(480, 420));
        setLocationRelativeTo(owner);
    }

    public void setData(ArrayList<Game> games) {
        model.clear();
        if (games != null) for (Game g : games) model.addElement(g);
    }

    private class GameRenderer extends JPanel implements ListCellRenderer<Game> {
        private final JLabel left = new JLabel();
        private final JLabel right = new JLabel();
        GameRenderer() {
            super(new BorderLayout());
            Tokens t = Theme.tokens();
            setOpaque(true);
            left.setFont(t.fontBody);
            right.setFont(t.fontCaption);
            right.setForeground(t.fgMuted);
            add(left, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
            setBorder(BorderFactory.createEmptyBorder(t.space2, t.space3, t.space2, t.space3));
        }
        @Override
        public Component getListCellRendererComponent(JList<? extends Game> list, Game g,
                                                       int index, boolean isSelected, boolean cellHasFocus) {
            Tokens t = Theme.tokens();
            if (g == null) {
                left.setText(""); right.setText("");
            } else {
                String b = g.getbUser();
                String w = g.getwUer();
                String winner = g.getWinner();
                left.setText(b + " (黑) vs " + w + " (白)  胜者：" + winner);
                right.setText(g.getDate() != null ? fmt.format(g.getDate()) : "");
            }
            setBackground(isSelected
                    ? new java.awt.Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 40)
                    : t.bgCard);
            left.setForeground(t.fgDefault);
            return this;
        }
    }
}
