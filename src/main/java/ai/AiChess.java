// 人机对战的棋子：除颜色外还显示手数
package ai;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

public class AiChess {
	public static final int DIAMETER = AiBoard.SPAN - 2;
	private int col;
	private int row;
	private Color color;
	private int num;
	AiBoard cb;

	public AiChess(AiBoard cb, int col, int row, Color color, int num) {
		this.cb = cb;
		this.col = col;
		this.row = row;
		this.color = color;
		this.num = num;
	}

	public int getCol() {
		return col;
	}

	public int getRow() {
		return row;
	}

	public Color getColor() {
		return color;
	}

	public int getNum() {
		return num;
	}

	public void draw(Graphics g) {
		int xPos = col * AiBoard.SPAN + AiBoard.MARGIN;
		int yPos = row * AiBoard.SPAN + AiBoard.MARGIN;
		Graphics2D g2d = (Graphics2D) g;
		RadialGradientPaint paint = null;
		int x = xPos + DIAMETER / 4;
		int y = yPos - DIAMETER / 4;
		float[] f = { 0f, 1f };
		Color[] c = { Color.WHITE, Color.BLACK };
		if (color == Color.black) {
			paint = new RadialGradientPaint(x, y, DIAMETER, f, c);
		} else if (color == Color.white) {
			paint = new RadialGradientPaint(x, y, DIAMETER * 2, f, c);
		}
		g2d.setPaint(paint);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
		Ellipse2D e = new Ellipse2D.Float(xPos - DIAMETER / 2, yPos - DIAMETER / 2, DIAMETER, DIAMETER);
		g2d.fill(e);

		Font drawFont = new Font("Arial", Font.PLAIN, 8);
		g2d.setFont(drawFont);
		g2d.setColor(color == Color.white ? Color.black : Color.RED);
		g2d.drawString(String.valueOf(num), xPos - DIAMETER / 8, yPos + DIAMETER / 8);
	}
}
