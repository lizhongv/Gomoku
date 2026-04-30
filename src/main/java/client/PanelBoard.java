// 棋盘面板：处理鼠标落子 / 绘制棋盘 / 判定五连
package client;

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

public class PanelBoard extends JPanel {
	public static final int MARGIN = 15;
	public static final int SPAN = 20;
	public static final int ROWS = 18;
	public static final int COLS = 18;
	Image img;
	public Chess[] chessList = new Chess[(ROWS + 1) * (COLS + 1)];
	int chessCount;
	boolean isBlack = true;
	boolean isGamming = false;
	boolean isGoing = false;
	FiveClient fc;

	public PanelBoard(FiveClient fc) {
		this.fc = fc;
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
				PanelBoard.this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			else
				PanelBoard.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
		}
	}

	class MouseMonitor extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			if (!isGamming)
				return;
			if (!isGoing)
				return;
			int col = (e.getX() - MARGIN + SPAN / 2) / SPAN;
			int row = (e.getY() - MARGIN + SPAN / 2) / SPAN;
			if (col < 0 || col > COLS || row < 0 || row > ROWS)
				return;
			if (hasChess(col, row))
				return;
			Chess ch = new Chess(PanelBoard.this, col, row, isBlack ? Color.black : Color.white);
			chessList[chessCount++] = ch;
			repaint();
			isGoing = false;
			fc.c.go(col, row);
			if (isWin(col, row)) {
				fc.c.wins();
			}
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
				g.drawRect(xPos - Chess.DIAMETER / 2, yPos - Chess.DIAMETER / 2, Chess.DIAMETER, Chess.DIAMETER);
			}
		}
	}

	private boolean hasChess(int col, int row) {
		for (int i = 0; i < chessCount; i++) {
			Chess ch = chessList[i];
			if (ch != null && ch.getCol() == col && ch.getRow() == row)
				return true;
		}
		return false;
	}

	private boolean hasChess(int col, int row, Color color) {
		for (int i = 0; i < chessCount; i++) {
			Chess ch = chessList[i];
			if (ch != null && ch.getCol() == col && ch.getRow() == row && ch.getColor() == color)
				return true;
		}
		return false;
	}

	private boolean isWin(int col, int row) {
		int continueCount = 1;
		Color c = isBlack ? Color.black : Color.white;

		for (int x = col - 1; x >= 0; x--) {
			if (hasChess(x, row, c))
				continueCount++;
			else
				break;
		}
		for (int x = col + 1; x <= COLS; x++) {
			if (hasChess(x, row, c))
				continueCount++;
			else
				break;
		}
		if (continueCount >= 5)
			return true;
		else
			continueCount = 1;

		for (int y = row - 1; y >= 0; y--) {
			if (hasChess(col, y, c))
				continueCount++;
			else
				break;
		}
		for (int y = row + 1; y <= ROWS; y++) {
			if (hasChess(col, y, c))
				continueCount++;
			else
				break;
		}
		if (continueCount >= 5)
			return true;
		else
			continueCount = 1;

		for (int x = col + 1, y = row - 1; y >= 0 && x <= COLS; x++, y--) {
			if (hasChess(x, y, c))
				continueCount++;
			else
				break;
		}
		for (int x = col - 1, y = row + 1; x >= 0 && y <= ROWS; x--, y++) {
			if (hasChess(x, y, c))
				continueCount++;
			else
				break;
		}
		if (continueCount >= 5)
			return true;
		else
			continueCount = 1;

		for (int x = col - 1, y = row - 1; x >= 0 && y >= 0; x--, y--) {
			if (hasChess(x, y, c))
				continueCount++;
			else
				break;
		}
		for (int x = col + 1, y = row + 1; x <= COLS && y <= ROWS; x++, y++) {
			if (hasChess(x, y, c))
				continueCount++;
			else
				break;
		}
		if (continueCount >= 5)
			return true;
		else
			return false;
	}

	public void addOpponentChess(int col, int row) {
		Chess ch = new Chess(this, col, row, isBlack ? Color.white : Color.black);
		chessList[chessCount++] = ch;
		isGoing = true;
		repaint();
	}

	public void winsGame() {
		resetGame();
		String colorName = isBlack ? "黑棋" : "白棋";
		String msg = String.format("恭喜，%s赢了!", colorName);
		JOptionPane.showMessageDialog(PanelBoard.this, msg);
	}

	public void lossesGame() {
		resetGame();
		String colorName = isBlack ? "黑棋" : "白棋";
		String msg = String.format("遗憾，%s输了！", colorName);
		JOptionPane.showMessageDialog(PanelBoard.this, msg);
	}

	public void resetGame() {
		chessCount = 0;
		isGamming = false;
		for (int i = 0; i < chessList.length; i++) {
			chessList[i] = null;
		}
		repaint();
		fc.control.joinGameButton.setEnabled(true);
	}

	public int getChessCount() {
		return chessCount;
	}

	public void setChessCount(int chessCount) {
		this.chessCount = chessCount;
	}
}
