package ui.scene;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import game.Manual;
import game.ManualDao;
import profile.Profile;
import protocol.SafeObjectInputStream;
import ui.Theme;
import ui.component.BrandHeader;
import ui.component.GButton;
import ui.component.GCard;
import ui.component.GToast;
import ui.design.Tokens;

public class LocalManualScene implements Scene {
    private final Router router;
    private final JPanel rootPanel;
    private final DefaultListModel<File> listModel = new DefaultListModel<>();
    private final JList<File> fileList;
    private final JLabel detailLabel;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LocalManualScene(Router router) {
        this.router = router;
        Tokens t = Theme.tokens();

        rootPanel = new JPanel(new BorderLayout(t.space3, t.space3));
        rootPanel.setBackground(t.bgPrimary);

        BrandHeader header = new BrandHeader();
        header.setCurrentUser(Profile.get().nickname, null);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setBorder(BorderFactory.createEmptyBorder(t.space2, t.space5, t.space2, t.space5));
        JLabel title = new JLabel("本地棋谱");
        title.setFont(t.fontDisplay);
        title.setForeground(t.fgDefault);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("含 AI 对战 + 联机对战的本地副本（仅本机可见）");
        sub.setFont(t.fontCaption);
        sub.setForeground(t.fgMuted);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(title);
        titleBox.add(sub);

        GButton back = GButton.ghost("← 返回");
        back.addActionListener(e -> router.pop());

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(titleBox, BorderLayout.WEST);
        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, t.space3, 0));
        backWrap.setOpaque(false);
        backWrap.add(back);
        titleRow.add(backWrap, BorderLayout.EAST);
        north.add(titleRow, BorderLayout.SOUTH);
        rootPanel.add(north, BorderLayout.NORTH);

        fileList = new JList<>(listModel);
        fileList.setCellRenderer(new FileCellRenderer());
        fileList.setBackground(t.bgCard);
        fileList.setBorder(BorderFactory.createEmptyBorder(t.space2, t.space2, t.space2, t.space2));
        fileList.addListSelectionListener(e -> updateDetail());
        JScrollPane sp = new JScrollPane(fileList);
        sp.setBorder(BorderFactory.createLineBorder(t.borderLight));
        sp.getViewport().setBackground(t.bgCard);
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, t.space5, 0, 0));
        center.add(sp, BorderLayout.CENTER);
        rootPanel.add(center, BorderLayout.CENTER);

        GCard side = new GCard(true);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        detailLabel = new JLabel("请选择左侧棋谱");
        detailLabel.setFont(t.fontBody);
        detailLabel.setForeground(t.fgDefault);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(detailLabel);
        side.add(Box.createVerticalStrut(t.space3));

        GButton review = GButton.primary("查看复盘");
        review.setAlignmentX(Component.CENTER_ALIGNMENT);
        review.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        review.addActionListener(e -> openReview());
        side.add(review);
        side.add(Box.createVerticalStrut(t.space2));

        GButton del = GButton.secondary("删除");
        del.setForeground(t.danger);
        del.setAlignmentX(Component.CENTER_ALIGNMENT);
        del.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        del.addActionListener(e -> deleteSelected());
        side.add(del);

        JPanel sideWrap = new JPanel(new BorderLayout());
        sideWrap.setOpaque(false);
        sideWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, t.space5));
        sideWrap.setPreferredSize(new Dimension(260, 10));
        sideWrap.add(side, BorderLayout.NORTH);
        rootPanel.add(sideWrap, BorderLayout.EAST);
    }

    private void refresh() {
        listModel.clear();
        File dir = ManualDao.getDir();
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".fiv"));
        if (files == null) return;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File f : files) listModel.addElement(f);
    }

    private void updateDetail() {
        File f = fileList.getSelectedValue();
        if (f == null) { detailLabel.setText("请选择左侧棋谱"); return; }
        String name = f.getName();
        if (name.endsWith(".fiv")) name = name.substring(0, name.length() - 4);
        detailLabel.setText("<html>" + name + "<br>修改时间：" + fmt.format(new Date(f.lastModified())) + "</html>");
    }

    private void openReview() {
        File f = fileList.getSelectedValue();
        if (f == null) { GToast.info(rootPanel, "请先选择棋谱"); return; }
        Manual m = readManualSafely(f);
        if (m == null) { GToast.error(rootPanel, "棋谱解析失败"); return; }
        router.replace(new ReviewScene(router, m));
    }

    private Manual readManualSafely(File f) {
        try (FileInputStream fis = new FileInputStream(f);
             ObjectInputStream ois = new SafeObjectInputStream(fis, "game.Game", "game.Manual")) {
            return (Manual) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteSelected() {
        File f = fileList.getSelectedValue();
        if (f == null) { GToast.info(rootPanel, "请先选择棋谱"); return; }
        int ans = JOptionPane.showConfirmDialog(rootPanel,
                "确认删除棋谱「" + f.getName() + "」？",
                "删除棋谱",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (ans == JOptionPane.OK_OPTION) {
            if (f.delete()) {
                GToast.success(rootPanel, "已删除");
                refresh();
            } else {
                GToast.error(rootPanel, "删除失败");
            }
        }
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "本地棋谱"; }
    @Override public void onEnter() { refresh(); }

    private static class FileFmt {
        static String shortName(File f) {
            String n = f.getName();
            return n.endsWith(".fiv") ? n.substring(0, n.length() - 4) : n;
        }
    }

    private class FileCellRenderer extends JPanel implements ListCellRenderer<File> {
        private final JLabel name = new JLabel();
        private final JLabel time = new JLabel();
        FileCellRenderer() {
            super(new BorderLayout());
            Tokens t = Theme.tokens();
            setOpaque(true);
            name.setFont(t.fontBody);
            time.setFont(t.fontCaption);
            time.setForeground(t.fgMuted);
            add(name, BorderLayout.CENTER);
            add(time, BorderLayout.EAST);
            setBorder(BorderFactory.createEmptyBorder(t.space2, t.space3, t.space2, t.space3));
        }
        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File value,
                                                       int index, boolean isSelected, boolean cellHasFocus) {
            Tokens t = Theme.tokens();
            if (value == null) {
                name.setText(""); time.setText("");
            } else {
                name.setText(FileFmt.shortName(value));
                time.setText(fmt.format(new Date(value.lastModified())));
            }
            if (isSelected) {
                setBackground(new java.awt.Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 40));
            } else {
                setBackground(t.bgCard);
            }
            name.setForeground(t.fgDefault);
            return this;
        }
    }
}
