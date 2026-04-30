package ui.scene;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.io.File;
import feature.review.ReviewEngine;
import feature.review.ReviewMove;
import feature.review.ReviewReport;
import feature.share.ExportOpts;
import feature.share.GifExporter;
import feature.skin.SkinManager;
import game.Manual;
import profile.Profile;
import ui.Theme;
import ui.component.GToast;
import ui.component.BrandHeader;
import ui.component.GBoard;
import ui.component.GButton;
import ui.component.GCard;
import ui.component.GSlider;
import ui.component.ScoreChart;
import ui.design.Tokens;

public class ReviewScene implements Scene {
    private static final int BOARD_SIZE = 15;
    private static final int AI_DEPTH = 1;
    private static final int AI_SAMPLES = 5;

    private final Router router;
    private final Manual manual;
    private final JPanel rootPanel;
    private final GBoard board;
    private final GSlider slider;
    private final JLabel stepLabel;
    private final JLabel summaryLabel;
    private final ScoreChart chart;
    private final DefaultListModel<String> moveListModel;
    private final JList<String> moveList;

    private ReviewReport report;

    public ReviewScene(Router router, Manual manual) {
        this.router = router;
        this.manual = manual;

        Tokens t = Theme.tokens();
        rootPanel = new JPanel(new BorderLayout(t.space3, t.space3));
        rootPanel.setBackground(t.bgPrimary);

        BrandHeader header = new BrandHeader();
        header.setCurrentUser(Profile.get().nickname, null);
        rootPanel.add(header, BorderLayout.NORTH);

        board = new GBoard();
        board.setInteractive(false);
        board.setStones(new ArrayList<>());

        int total = Math.max(1, manual.getTotalStep());
        slider = new GSlider(0, total - 1, 0);
        stepLabel = new JLabel("第 0 / " + total + " 手");
        stepLabel.setFont(t.fontBody);
        stepLabel.setForeground(t.fgDefault);
        slider.addChangeListener(e -> onSliderChanged());

        JPanel sliderRow = new JPanel(new BorderLayout(t.space2, 0));
        sliderRow.setOpaque(false);
        sliderRow.setBorder(BorderFactory.createEmptyBorder(0, t.space5, t.space2, t.space5));
        sliderRow.add(slider, BorderLayout.CENTER);
        sliderRow.add(stepLabel, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, t.space2));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, t.space5, 0, 0));
        center.add(board, BorderLayout.CENTER);
        center.add(sliderRow, BorderLayout.SOUTH);
        rootPanel.add(center, BorderLayout.CENTER);

        GCard side = new GCard(true);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        summaryLabel = new JLabel(buildSummaryText(null));
        summaryLabel.setFont(t.fontBody);
        summaryLabel.setForeground(t.fgDefault);

        chart = new ScoreChart();
        chart.setProgress("等待开始");
        chart.setPreferredSize(new Dimension(280, 180));
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        moveListModel = new DefaultListModel<>();
        moveList = new JList<>(moveListModel);
        moveList.setFont(t.fontMono);
        moveList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int idx = moveList.getSelectedIndex();
            if (idx >= 0 && idx <= total - 1 && slider.getValue() != idx) {
                slider.setValue(idx);
            }
        });
        JScrollPane listScroll = new JScrollPane(moveList);
        listScroll.setPreferredSize(new Dimension(260, 160));
        listScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        listScroll.setBorder(BorderFactory.createLineBorder(t.borderLight));

        side.add(summaryLabel);
        side.add(Box.createVerticalStrut(t.space2));
        side.add(chart);
        side.add(Box.createVerticalStrut(t.space2));
        side.add(listScroll);

        JPanel sideWrap = new JPanel(new BorderLayout());
        sideWrap.setOpaque(false);
        sideWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, t.space5));
        sideWrap.setPreferredSize(new Dimension(300, 10));
        sideWrap.add(side, BorderLayout.NORTH);
        rootPanel.add(sideWrap, BorderLayout.EAST);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, t.space2, t.space2));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, t.space5, t.space3, t.space5));
        GButton exportBtn = GButton.secondary("导出 GIF");
        exportBtn.addActionListener(e -> exportGif());
        GButton backBtn = GButton.primary("返回大厅");
        backBtn.addActionListener(e -> router.pop());
        south.add(exportBtn);
        south.add(backBtn);
        rootPanel.add(south, BorderLayout.SOUTH);
    }

    private void onSliderChanged() {
        int v = slider.getValue();
        int total = manual.getTotalStep();
        stepLabel.setText("第 " + (v + 1) + " / " + total + " 手");
        replayBoardTo(v);
        if (moveList.getSelectedIndex() != v && v >= 0 && v < moveListModel.size()) {
            moveList.setSelectedIndex(v);
            moveList.ensureIndexIsVisible(v);
        }
    }

    private void replayBoardTo(int idx) {
        List<GBoard.Stone> stones = new ArrayList<>();
        int[][] cl = manual.getChessList();
        if (cl == null) { board.setStones(stones); return; }
        int upto = Math.min(idx, cl.length - 1);
        for (int i = 0; i <= upto; i++) {
            int[] mv = cl[i];
            if (mv == null || mv.length < 2) continue;
            boolean isBlack = (i % 2 == 0);
            stones.add(new GBoard.Stone(mv[0], mv[1], isBlack));
        }
        board.setStones(stones);
        if (!stones.isEmpty()) {
            GBoard.Stone last = stones.get(stones.size() - 1);
            board.placeChess(last.col, last.row, last.black);
        }
    }

    private String buildSummaryText(ReviewReport r) {
        int total = manual.getTotalStep();
        if (r == null) {
            return "<html>共 " + total + " 手<br>复盘中...</html>";
        }
        return "<html>共 " + total + " 手"
                + "<br>黑失误 " + r.blackMistakes + " · 白失误 " + r.whiteMistakes
                + "<br>黑妙手 " + r.blackBrilliant + " · 白妙手 " + r.whiteBrilliant
                + "</html>";
    }

    private void runAnalysis() {
        chart.setProgress("复盘进行中 0%...");
        SwingWorker<ReviewReport, Integer> worker = new SwingWorker<ReviewReport, Integer>() {
            @Override
            protected ReviewReport doInBackground() {
                return ReviewEngine.analyze(manual, AI_DEPTH, AI_SAMPLES, BOARD_SIZE,
                        (cur, tot) -> {
                            int pct = tot > 0 ? (cur * 100 / tot) : 0;
                            publish(pct);
                        });
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (chunks.isEmpty()) return;
                int latest = chunks.get(chunks.size() - 1);
                chart.setProgress("复盘进行中 " + latest + "%...");
            }

            @Override
            protected void done() {
                try {
                    report = get();
                } catch (Exception ex) {
                    chart.setProgress("复盘失败");
                    return;
                }
                onReportReady();
            }
        };
        worker.execute();
    }

    private void onReportReady() {
        if (report == null || report.moves == null) return;
        int n = report.moves.length;
        int[] black = new int[n];
        int[] white = new int[n];
        for (int i = 0; i < n; i++) {
            ReviewMove mv = report.moves[i];
            black[i] = (i % 2 == 0) ? mv.evalActual : -mv.evalActual;
            white[i] = (i % 2 == 1) ? mv.evalActual : -mv.evalActual;
        }
        chart.setData(black, white);
        summaryLabel.setText(buildSummaryText(report));

        moveListModel.clear();
        for (int i = 0; i < n; i++) {
            ReviewMove mv = report.moves[i];
            String prefix = (i % 2 == 0) ? "黑" : "白";
            moveListModel.addElement(String.format("[%2d] %s (%2d,%2d) %s",
                    i + 1, prefix, mv.col, mv.row, mv.tag.name()));
        }
        if (n > 0) {
            int sel = Math.min(slider.getValue(), n - 1);
            moveList.setSelectedIndex(sel);
            moveList.ensureIndexIsVisible(sel);
        }
    }

    private void exportGif() {
        ExportOpts opts = new ExportOpts();
        opts.skinId = SkinManager.current().id;
        opts.maxFrames = Math.max(1, manual.getTotalStep());
        opts.frameDelayMs = 800;
        GToast.info(rootPanel, "GIF 导出中...");
        new SwingWorker<File, Integer>() {
            @Override
            protected File doInBackground() throws Exception {
                return GifExporter.export(manual, opts, (cur, total) -> publish(cur * 100 / total));
            }

            @Override
            protected void done() {
                try {
                    File f = get();
                    GToast.success(SwingUtilities.getWindowAncestor(rootPanel),
                            "已导出: " + f.getName());
                } catch (Exception ex) {
                    GToast.error(SwingUtilities.getWindowAncestor(rootPanel),
                            "导出失败: " + ex.getMessage());
                }
            }
        }.execute();
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "复盘"; }

    @Override
    public void onEnter() {
        replayBoardTo(0);
        SwingUtilities.invokeLater(this::runAnalysis);
    }
}
