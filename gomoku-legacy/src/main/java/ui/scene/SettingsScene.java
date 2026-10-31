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
        String[] fontKeys = {"default", "microsoft_yahei", "source_han", "kaiti"};
        String[] fontNames = {"系统默认", "微软雅黑", "思源黑体", "楷体"};
        JComboBox<String> fontBox = new JComboBox<>(fontNames);
        fontBox.setFont(t.fontBody);
        String cur = Profile.get().fontFamily == null ? "default" : Profile.get().fontFamily;
        for (int i = 0; i < fontKeys.length; i++) {
            if (fontKeys[i].equals(cur)) { fontBox.setSelectedIndex(i); break; }
        }
        fontBox.addActionListener(e -> {
            int idx = fontBox.getSelectedIndex();
            if (idx < 0) return;
            Profile.get().fontFamily = fontKeys[idx];
            Profile.get().save();
            GToast.info(rootPanel, "字体设置将在下次启动生效");
        });
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
        int[] difDepths = {1, 3, 5};
        GButton[] difBtns = new GButton[difficulties.length];
        int curDepth = Profile.get().aiDefaultDepth;
        for (int i = 0; i < difficulties.length; i++) {
            final int idx = i;
            String name = difficulties[i];
            GButton btn = GButton.secondary(name);
            btn.setEnabled(difDepths[i] != curDepth);
            btn.addActionListener(e -> {
                Profile.get().aiDefaultDepth = difDepths[idx];
                Profile.get().save();
                for (int j = 0; j < difBtns.length; j++) difBtns[j].setEnabled(j != idx);
                GToast.info(rootPanel, "默认难度已设为：" + name);
            });
            difBtns[i] = btn;
            difRow.add(btn);
        }
        difRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel inviteRow = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space2, 0));
        inviteRow.setOpaque(false);
        JLabel inviteLbl = new JLabel("邀请用时默认值（秒）");
        inviteLbl.setFont(t.fontBody);
        inviteLbl.setForeground(t.fgMuted);
        GTextField inviteField = new GTextField();
        inviteField.setText(String.valueOf(Profile.get().inviteSeconds));
        inviteField.setPreferredSize(new Dimension(120, 32));
        inviteField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                String txt = inviteField.getText().trim();
                try {
                    int sec = Integer.parseInt(txt);
                    if (sec < 30 || sec > 3600) throw new NumberFormatException();
                    Profile.get().inviteSeconds = sec;
                    Profile.get().save();
                    GToast.info(rootPanel, "邀请用时已保存：" + sec + " 秒");
                } catch (NumberFormatException nfe) {
                    GToast.error(rootPanel, "请输入 30-3600 之间的整数");
                    inviteField.setText(String.valueOf(Profile.get().inviteSeconds));
                }
            }
        });
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
        GButton avatar = GButton.secondary("更换头像");
        avatar.addActionListener(e -> doChangeAvatar());
        GButton export = GButton.secondary("导出档案");
        export.addActionListener(e -> doExport());
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
        row.add(avatar);
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
        about.addActionListener(e -> new ui.scene.dialog.AboutDialog(
                javax.swing.SwingUtilities.getWindowAncestor(rootPanel)).setVisible(true));
        GButton onboarding = GButton.ghost("新手引导");
        onboarding.addActionListener(e -> new ui.scene.dialog.OnboardingDialog(
                javax.swing.SwingUtilities.getWindowAncestor(rootPanel)).setVisible(true));
        GButton rules = GButton.ghost("游戏规则");
        rules.addActionListener(e -> showRulesDialog());
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

    private void doChangeAvatar() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("选择头像图片");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "图片 (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(rootPanel) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        File src = fc.getSelectedFile();
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(src);
            if (img == null) { GToast.error(rootPanel, "图片格式无法识别"); return; }
            java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(128, 128,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, 128, 128, null);
            g.dispose();
            File out = new File(AppConfig.get().avatarDir(), "me_" + System.currentTimeMillis() + ".png");
            javax.imageio.ImageIO.write(scaled, "PNG", out);
            Profile p = Profile.get();
            p.avatarPath = out.getAbsolutePath();
            p.save();
            GToast.success(rootPanel, "头像已更新（重新进入大厅生效）");
        } catch (Exception ex) {
            GToast.error(rootPanel, "更换失败：" + ex.getMessage());
        }
    }

    private void doExport() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("导出档案为 zip");
        chooser.setSelectedFile(new File("gomoku-backup-"
                + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date())
                + ".zip"));
        if (chooser.showSaveDialog(rootPanel) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        File dataDir = AppConfig.get().userDataDir();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.BufferedOutputStream(new java.io.FileOutputStream(out)))) {
            zipDirInto(zos, dataDir, "");
            GToast.success(rootPanel, "已导出到：" + out.getName());
        } catch (java.io.IOException ex) {
            GToast.error(rootPanel, "导出失败：" + ex.getMessage());
        }
    }

    private void zipDirInto(java.util.zip.ZipOutputStream zos, File dir, String prefix)
            throws java.io.IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String entryName = prefix + f.getName();
            if (f.isDirectory()) {
                zipDirInto(zos, f, entryName + "/");
            } else {
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                java.nio.file.Files.copy(f.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    private void showRulesDialog() {
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(rootPanel);
        ui.component.GDialog d = new ui.component.GDialog(owner, "五子棋规则", true);
        d.getContentPane().setLayout(new BorderLayout());
        javax.swing.JEditorPane pane = new javax.swing.JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        Tokens t = Theme.tokens();
        pane.setBackground(t.bgPrimary);
        pane.setText("<html><body style='font-family:Microsoft YaHei UI; padding:8px; color:#3A2418;'>"
                + "<h3>对局目标</h3>"
                + "<p>在 15×15 棋盘上，先在横、竖、斜任一方向连成五子者胜。</p>"
                + "<h3>基本规则</h3>"
                + "<ul>"
                + "<li>黑棋先手，黑白双方轮流落子，每次仅落一颗。</li>"
                + "<li>已落下的棋子不可移动或拿取。</li>"
                + "<li>本作采用「无禁手」规则，黑白方权利对等。</li>"
                + "<li>满盘和棋（理论上极难）。</li>"
                + "</ul>"
                + "<h3>常见术语</h3>"
                + "<ul>"
                + "<li><b>活四</b>：四子连成且两端均为空，下一步必胜。</li>"
                + "<li><b>双三</b>：同一手形成两个活三，对方难以同时防守。</li>"
                + "<li><b>冲四</b>：四子连成但仅一端可落子，须立即应对。</li>"
                + "</ul>"
                + "<p style='color:#8A6D4F'><i>提示：开局抢占中央 (7,7) 通常更占优势。</i></p>"
                + "</body></html>");
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(pane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        d.getContentPane().add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.setOpaque(false);
        GButton ok = GButton.primary("我知道了");
        ok.addActionListener(e -> d.dispose());
        buttons.add(ok);
        d.getContentPane().add(buttons, BorderLayout.SOUTH);
        d.setSize(new Dimension(500, 460));
        d.setLocationRelativeTo(rootPanel);
        d.setVisible(true);
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "设置"; }
}
