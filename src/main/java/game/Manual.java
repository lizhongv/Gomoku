// 棋谱：在 Game 基础上额外存储完整落子坐标序列
package game;

import java.io.Serializable;

public class Manual extends Game implements Serializable {
	private static final long serialVersionUID = -7422915588944816730L;
	int totalStep;
	int[][] chessList;

	public Manual(String bUser, String wUser, java.sql.Date date, String winner, int totalStep, int[][] chessList) {
		super(bUser, wUser, date, winner);
		this.totalStep = totalStep;
		this.chessList = chessList;
	}

	public int getTotalStep() {
		return totalStep;
	}

	public void setTotalStep(int totalStep) {
		this.totalStep = totalStep;
	}

	public int[][] getChessList() {
		return chessList;
	}

	public void setChessList(int[][] chessList) {
		this.chessList = chessList;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
