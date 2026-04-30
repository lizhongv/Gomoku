package ui.scene;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import config.AppConfig;
import feature.skin.SkinManager;
import feature.sound.SoundManager;
import profile.Profile;
import ui.Theme;
import ui.component.BrandHeader;
import ui.component.GButton;
import ui.component.GCard;
import ui.component.GChip;
import ui.component.GSlider;
import ui.component.GTextField;
import ui.component.GToast;
import ui.design.Skin;
import ui.design.Tokens;

public class SettingsScene implements Scene {
    private final Router router;
    private final JPanel rootPanel;
    private boolean soundEnabled = true;

    public SettingsScene(Router router) {
        this.router = router;
        this.rootPanel = new JPanel(new BorderLayout());
        build();
    }

    private void build() {
        Tokens t = Theme.tokens();
        rootPanel.setBackground(t.bgPrimary);

        JPanel north = new JPanel(new BorderLayout(0, t.space2));
        north.setOpaque(false);
        BrandHeader header = new BrandHeader();
        header.setCurrentUser(Profile.get().nickname, null);
        north.add(header, BorderLayout.NORTH);

        JLabel title = new JLabel("设置", SwingConstants.LEFT);
        title.setFont(t.fontDisplay);
        title.setForeground(t.fgDefault);
        title.setBorder(BorderFactory.createEmptyBorder(t.space2, t.space5, t.space2, t.space5));
        north.add(title, BorderLayout.SOUTH);

        rootPanel.add(north, BorderLayout.NORTH);

        JPanel sections = new JPanel();
        sections.setOpaque(false);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setBorder(BorderFactory.createEmptyBorder(t.space3, t.space5, t.space5, t.space5));

        sections.add(buildThemeSection());
        sections.add(Box.createVerticalStrut(t.space3));
        sections.add(buildSoundSection());
        sections.add(Box.createVerticalStrut(t.space3));
        sections.add(buildAppearanceSection());
        sections.add(Box.createVerticalStrut(t.space3));
        sections.add(buildGameplaySection());
        sections.add(Box.createVerticalStrut(t.space3));
        sections.add(buildDataSection());
        sections.add(Box.createVerticalStrut(t.space3));
        sections.add(buildAboutSection());

        JScrollPane scroll = new JScrollPane(sections);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        rootPanel.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space2, t.space2));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, t.space5, t.space3, t.space5));
        GButton back = GButton.ghost("← 返回");
        back.addActionListener(e -> router.pop());
        south.add(back);
        rootPanel.add(south, BorderLayout.SOUTH);
    }

    private GCard buildThemeSection() {
        Tokens t = Theme.tokens();
        GCard card = new GCard(false);
        card.setLayout(new BorderLayout(0, t.space2));
        JLabel head = sectionLabel("主题");
        card.add(head, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space3, 0));
        row.setOpaque(false);
        JLabel cur = new JLabel("当前皮肤");
        cur.setFont(t.fontBody);
        cur.setForeground(t.fgMuted);
        Skin s = SkinManager.current();
        GChip chip = new GChip(s.displayName, t.accent, t.fgOnAccent);
        GButton goShop = GButton.secondary("进入商店");
        goShop.addActionListener(e -> router.push(new ShopScene(router)));
        row.add(cur);
        row.add(chip);
        row.add(goShop);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private GCard buildSoundSection() {
        Tokens t = Theme.tokens();
        GCard card = new GCard(false);
        card.setLayout(new BorderLayout(0, t.space2));
        card.add(sectionLabel("音效"), BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space3, 0));
        row.setOpaque(false);

        GButton toggle = GButton.secondary("音效：开");
        toggle.addActionListener(e -> {
            soundEnabled = !soundEnabled;
            SoundManager.setEnabled(soundEnabled);
            toggle.setText("音效：" + (soundEnabled ? "开" : "关"));
        });

        JLabel volLabel = new JLabel("音量");
        volLabel.setFont(t.fontBody);
        volLabel.setForeground(t.fgMuted);

        GSlider slider = new GSlider(0, 100, 70);
        slider.setPreferredSize(new Dimension(200, 28));
        slider.addChangeListener(e -> SoundManager.setVolume(slider.getValue()));

        row.add(toggle);
        row.add(volLabel);
        row.add(slider);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private GCard buildAppearanceSection() {
        Tokens t = Theme.tokens();
        GCard card = new GCard(false);
        card.setLayout(new BorderLayout(0, t.space2));
        card.add(sectionLabel("外观"), BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space3, 0));
        row.setOpaque(false);
        JLabel fontLbl = new JLabel("字体");
        fontLbl.setFont(t.fontBody);
        fontLbl.setForeground(t.fgMuted);
        String[] fonts = {"系统默认", "微软雅黑", "思源黑体", "楷体"};
        JComboBox<String> fontBox = new JComboBox<>(fonts);
        fontBox.setFont(t.fontBody);
        fontBox.addActionListener(e -> GToast.info(rootPanel, "字体设置将在下次启动生效"));
        row.add(fontLbl);
        row.add(fontBox);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private GCard buildGameplaySection() {
        Tokens t = Theme.tokens();
        GCard card = new GCard(false);
        card.setLayout(new BorderLayout(0, t.space2));
        card.add(sectionLabel("对局"), BorderLayout.NORTH);

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JPanel difRow = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space2, 0));
        difRow.setOpaque(false);
        JLabel difLbl = new JLabel("AI 默认难度");
        difLbl.setFont(t.fontBody);
        difLbl.setForeground(t.fgMuted);
        difRow.add(difLbl);
        String[] difficulties = {"简单", "标准", "困难"};
        for (int i = 0; i < difficulties.length; i++) {
            String name = difficulties[i];
            GButton btn = GButton.secondary(name);
            boolean isCurrent = (i == 1);
            btn.setEnabled(!isCurrent);
            btn.addActionListener(e -> GToast.info(rootPanel, "默认难度已设为：" + name));
            difRow.add(btn);
        }
        difRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel inviteRow = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space2, 0));
        inviteRow.setOpaque(false);
        JLabel inviteLbl = new JLabel("邀请用时默认值（秒）");
        inviteLbl.setFont(t.fontBody);
        inviteLbl.setForeground(t.fgMuted);
        GTextField inviteField = new GTextField();
        inviteField.setText("300");
        inviteField.setPreferredSize(new Dimension(120, 32));
        inviteRow.add(inviteLbl);
        inviteRow.add(inviteField);
        inviteRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        inner.add(difRow);
        inner.add(Box.createVerticalStrut(t.space2));
        inner.add(inviteRow);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private GCard buildDataSection() {
        Tokens t = Theme.tokens();
        GCard card = new GCard(false);
        card.setLayout(new BorderLayout(0, t.space2));
        card.add(sectionLabel("数据"), BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space3, 0));
        row.setOpaque(false);
        GButton export = GButton.secondary("导出档案");
        export.addActionListener(e -> GToast.info(rootPanel, "导出功能 Phase 9 实现"));
        GButton clear = GButton.secondary("清空本地数据");
        clear.setForeground(t.danger);
        clear.addActionListener(e -> {
            int ans = JOptionPane.showConfirmDialog(rootPanel,
                    "将删除本地档案、皮肤解锁与金币记录，且不可恢复。是否继续？",
                    "清空本地数据",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (ans == JOptionPane.OK_OPTION) {
                File f = new File(AppConfig.get().userDataDir(), "profile.properties");
                if (f.isFile()) f.delete();
                System.exit(0);
            }
        });
        row.add(export);
        row.add(clear);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private GCard buildAboutSection() {
        Tokens t = Theme.tokens();
        GCard card = new GCard(false);
        card.setLayout(new BorderLayout(0, t.space2));
        card.add(sectionLabel("帮助"), BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space3, 0));
        row.setOpaque(false);
        GButton about = GButton.ghost("关于");
        about.addActionListener(e -> GToast.info(rootPanel, "关于：8.4 实现"));
        GButton onboarding = GButton.ghost("新手引导");
        onboarding.addActionListener(e -> GToast.info(rootPanel, "新手引导：8.4 实现"));
        GButton rules = GButton.ghost("游戏规则");
        rules.addActionListener(e -> GToast.info(rootPanel,
                "黑棋先手，纵横斜任一方向五子连珠者胜"));
        row.add(about);
        row.add(onboarding);
        row.add(rules);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JLabel sectionLabel(String text) {
        Tokens t = Theme.tokens();
        JLabel l = new JLabel(text);
        l.setFont(t.fontHeading);
        l.setForeground(t.fgDefault);
        return l;
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "设置"; }
}
