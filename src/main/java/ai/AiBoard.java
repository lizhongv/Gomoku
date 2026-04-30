// 人机对战的棋盘：鼠标落子 + 触发 AI 应招
package ai;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

public class AiBoard extends JPanel {
	public static final int MARGIN = 15;
	public static final int SPAN = 20;
	public static final int ROWS = 14;
	public static final int COLS = 14;

	Image img;
	private AiGame f;
	int[][] boardStatus; // 0:无棋子 1:黑棋 2:白棋

	AiChess[] chessList = new AiChess[(ROWS + 1) * (COLS + 1)];
	int chessCount;
	int manChessCount;
	int computerChessCount;
	int computerColor; // 1:黑 2:白

	boolean isBlack = true;
	boolean isGamming = false;
	boolean isComputerGo;

	public AiBoard(AiGame f) {
		this.f = f;
		boardStatus = new int[COLS + 1][ROWS + 1];
		img = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/assets/board.jpg"));
		this.addMouseListener(new MouseMonitor());
		this.addMouseMotionListener((MouseMotionListener) new MouseMotionMonitor());
	}

	public Dimension getPreferredSize() {
		return new Dimension(MARGIN * 2 + SPAN * COLS, MARGIN * 2 + SPAN * ROWS);
	}

	class MouseMotionMonitor extends MouseMotionAdapter {
		public void mouseMoved(MouseEvent e) {
			int col = (e.getX() - MARGIN + SPAN / 2) / SPAN;
			int row = (e.getY() - MARGIN + SPAN / 2) / SPAN;
			if (col < 0 || col > COLS || row < 0 || row > ROWS || !isGamming || hasChess(col, row))
				AiBoard.this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			else
				AiBoard.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
		}
	}

	class MouseMonitor extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			if (!isGamming) return;
			if (isComputerGo) return;
			int col = (e.getX() - MARGIN + SPAN / 2) / SPAN;
			int row = (e.getY() - MARGIN + SPAN / 2) / SPAN;
			if (col < 0 || col > COLS || row < 0 || row > ROWS) return;
			if (hasChess(col, row)) return;

			manGo(col, row);
			if (!isGamming) return;
			computerGo();
		}
	}

	public void restartGame() {
		pendingGen++; // 让仍在思考的旧 SwingWorker 在 done() 时丢弃结果
		for (int i = 0; i < chessList.length; i++) {
			chessList[i] = null;
		}
		for (int i = 0; i <= COLS; i++) {
			for (int j = 0; j <= ROWS; j++) {
				boardStatus[i][j] = 0;
			}
		}
		isGamming = true;
		isBlack = !f.whiteFirst.isSelected();
		isComputerGo = !f.manFirst.isSelected();
		computerColor = isComputerGo ? 1 : 2;
		chessCount = 0;
		manChessCount = 0;
		computerChessCount = 0;

		repaint();
		if (isComputerGo) {
			computerGo();
		}
	}

	public void goback() {
		if (isComputerGo || chessCount < 2) return;
		int i = chessList[chessCount - 1].getCol();
		int j = chessList[chessCount - 1].getRow();
		boardStatus[i][j] = 0;
		i = chessList[chessCount - 2].getCol();
		j = chessList[chessCount - 2].getRow();
		boardStatus[i][j] = 0;
		chessList[chessCount - 1] = null;
		chessList[chessCount - 2] = null;
		chessCount -= 2;
		manChessCount -= 1;
		computerChessCount -= 1;
		repaint();
	}

	public void giveUp() {
		String colorName = (computerColor == 1) ? "黑棋" : "白棋";
		String msg = String.format("恭喜,%s赢了！", colorName);
		isGamming = false;
		JOptionPane.showMessageDialog(AiBoard.this, msg);
	}

	public void about() {
		String msg;
		if (isGamming) {
			String colorName = isBlack ? "黑棋" : "白棋";
			msg = "default: computer first, black first. Now: gaming, current turn = " + colorName;
		} else {
			msg = "default: computer first, black first. Now: not gaming.";
		}
		JOptionPane.showMessageDialog(this, msg);
	}

	private void computerGo() {
		// 在后台线程跑深搜，结果回到 EDT 落子；避免阻塞 UI 1-3 秒
		// 通过快照 + gameGen 隔离与 EDT 的并发写
		final Color color = isBlack ? Color.black : Color.white;
		final int snapshotComputerColor = computerColor;
		final int[][] snapshot = snapshotBoard();
		final long myGen = ++pendingGen;
		new SwingWorker<int[], Void>() {
			@Override
			protected int[] doInBackground() {
				return new Evaluate(snapshot, snapshotComputerColor).getTheBestPosition();
			}

			@Override
			protected void done() {
				try {
					int[] pos = get();
					if (myGen != pendingGen || !isGamming) {
						return; // 用户已重开/退出，丢弃这次结果
					}
					computerChessCount++;
					putChess(pos[0], pos[1], color, computerChessCount);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.execute();
	}

	private long pendingGen = 0;

	private int[][] snapshotBoard() {
		int n = COLS + 1;
		int[][] copy = new int[n][n];
		for (int i = 0; i < n; i++) {
			System.arraycopy(boardStatus[i], 0, copy[i], 0, n);
		}
		return copy;
	}

	public void manGo(int col, int row) {
		manChessCount++;
		putChess(col, row, isBlack ? Color.black : Color.white, manChessCount);
	}

	public void putChess(int col, int row, Color color, int num) {
		AiChess ch = new AiChess(AiBoard.this, col, row, color, num);
		chessList[chessCount++] = ch;
		boardStatus[col][row] = (color == Color.BLACK) ? 1 : 2;
		repaint();
		if (isWin(col, row)) {
			String colorName = isBlack ? "黑棋" : "白棋";
			String msg = String.format("恭喜,%s赢了！", colorName);
			isGamming = false;
			JOptionPane.showMessageDialog(AiBoard.this, msg);
		} else {
			isBlack = !isBlack;
			isComputerGo = !isComputerGo;
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(img, 0, 0, this);
		for (int i = 0; i <= ROWS; i++) {
			g.drawLine(MARGIN, MARGIN + i * SPAN, MARGIN + COLS * SPAN, MARGIN + i * SPAN);
		}
		for (int i = 0; i <= COLS; i++) {
			g.drawLine(MARGIN + i * SPAN, MARGIN, MARGIN + i * SPAN, MARGIN + ROWS * SPAN);
		}
		g.fillRect(MARGIN + 3 * SPAN - 2, MARGIN + 3 * SPAN - 2, 5, 5);
		g.fillRect(MARGIN + (COLS / 2) * SPAN - 2, MARGIN + 3 * SPAN - 2, 5, 5);
		g.fillRect(MARGIN + (COLS - 3) * SPAN - 2, MARGIN + 3 * SPAN - 2, 5, 5);
		g.fillRect(MARGIN + 3 * SPAN - 2, MARGIN + (ROWS / 2) * SPAN - 2, 5, 5);
		g.fillRect(MARGIN + (COLS / 2) * SPAN - 2, MARGIN + (ROWS / 2) * SPAN - 2, 5, 5);
		g.fillRect(MARGIN + (COLS - 3) * SPAN - 2, MARGIN + (ROWS / 2) * SPAN - 2, 5, 5);
		g.fillRect(MARGIN + 3 * SPAN - 2, MARGIN + (ROWS - 3) * SPAN - 2, 5, 5);
		g.fillRect(MARGIN + (COLS / 2) * SPAN - 2, MARGIN + (ROWS - 3) * SPAN - 2, 5, 5);
		g.fillRect(MARGIN + (COLS - 3) * SPAN - 2, MARGIN + (ROWS - 3) * SPAN - 2, 5, 5);
		for (int i = 0; i < chessCount; i++) {
			chessList[i].draw(g);
			if (i == chessCount - 1) {
				int xPos = chessList[i].getCol() * SPAN + MARGIN;
				int yPos = chessList[i].getRow() * SPAN + MARGIN;
				g.setColor(Color.red);
				g.drawRect(xPos - AiChess.DIAMETER / 2, yPos - AiChess.DIAMETER / 2, AiChess.DIAMETER, AiChess.DIAMETER);
			}
		}
	}

	private boolean hasChess(int col, int row) {
		for (int i = 0; i < chessCount; i++) {
			AiChess ch = chessList[i];
			if (ch != null && ch.getCol() == col && ch.getRow() == row) return true;
		}
		return false;
	}

	private boolean hasChess(int col, int row, Color color) {
		for (int i = 0; i < chessCount; i++) {
			AiChess ch = chessList[i];
			if (ch != null && ch.getCol() == col && ch.getRow() == row && ch.getColor() == color) return true;
		}
		return false;
	}

	private boolean isWin(int col, int row) {
		int continueCount = 1;
		Color c = isBlack ? Color.black : Color.white;

		for (int x = col - 1; x >= 0; x--) {
			if (hasChess(x, row, c)) continueCount++; else break;
		}
		for (int x = col + 1; x <= COLS; x++) {
			if (hasChess(x, row, c)) continueCount++; else break;
		}
		if (continueCount >= 5) return true;
		continueCount = 1;

		for (int y = row - 1; y >= 0; y--) {
			if (hasChess(col, y, c)) continueCount++; else break;
		}
		for (int y = row + 1; y <= ROWS; y++) {
			if (hasChess(col, y, c)) continueCount++; else break;
		}
		if (continueCount >= 5) return true;
		continueCount = 1;

		for (int x = col + 1, y = row - 1; y >= 0 && x <= COLS; x++, y--) {
			if (hasChess(x, y, c)) continueCount++; else break;
		}
		for (int x = col - 1, y = row + 1; x >= 0 && y <= ROWS; x--, y++) {
			if (hasChess(x, y, c)) continueCount++; else break;
		}
		if (continueCount >= 5) return true;
		continueCount = 1;

		for (int x = col - 1, y = row - 1; x >= 0 && y >= 0; x--, y--) {
			if (hasChess(x, y, c)) continueCount++; else break;
		}
		for (int x = col + 1, y = row + 1; x <= COLS && y <= ROWS; x++, y++) {
			if (hasChess(x, y, c)) continueCount++; else break;
		}
		return continueCount >= 5;
	}
}
