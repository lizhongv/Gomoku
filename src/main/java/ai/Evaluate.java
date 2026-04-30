// 棋局估值 + Alpha-Beta 剪枝
package ai;

public class Evaluate {
	private static final int COLS = 14;
	private static final int ROWS = 14;
	private static final int FIVE = 50000;
	private static final int HUO_FOUR = 5000;
	private static final int CHONG_FOUR = 1000;
	private static final int HUO_THREE = 500;
	private static final int MIAN_THREE = 100;
	private static final int HUO_TWO = 50;

	private int[][] blackValue;
	private int[][] whiteValue;
	private int[][] staticValue;

	private static final int LARGE_NUMBER = 10000000;
	private static final int SEARCH_DEPTH = 3;
	private static final int SAMPLE_NUMBER = 10;

	// 取自 AiBoard 的快照，AI 在自己的副本上推演，避免与 EDT 写入产生数据竞争
	private final int[][] boardStatus;
	private final int computerColor;
	private final int searchDepth;
	private final int sampleNumber;

	public Evaluate(int[][] boardSnapshot, int computerColor) {
		this(boardSnapshot, computerColor, SEARCH_DEPTH, SAMPLE_NUMBER);
	}

	public Evaluate(int[][] boardSnapshot, int computerColor, int searchDepth, int sampleNumber) {
		int n = COLS + 1;
		this.boardStatus = new int[n][n];
		for (int i = 0; i < n; i++) {
			System.arraycopy(boardSnapshot[i], 0, this.boardStatus[i], 0, n);
		}
		this.computerColor = computerColor;
		this.searchDepth = searchDepth;
		this.sampleNumber = sampleNumber;

		blackValue = new int[n][n];
		whiteValue = new int[n][n];
		staticValue = new int[n][n];
		for (int i = 0; i <= COLS / 2; i++) {
			for (int j = 0; j <= ROWS / 2; j++) {
				staticValue[i][j] = i < j ? i : j;
				staticValue[COLS - i][j] = staticValue[i][j];
				staticValue[i][ROWS - j] = staticValue[i][j];
				staticValue[COLS - i][ROWS - j] = staticValue[i][j];
			}
		}
	}

	public int[] getTheBestPosition() {
		if (searchDepth <= 0) return new int[]{7, 7};
		for (int i = 0; i <= COLS; i++) {
			for (int j = 0; j <= ROWS; j++) {
				blackValue[i][j] = 0;
				whiteValue[i][j] = 0;
				if (boardStatus[i][j] == 0) {
					for (int m = 1; m <= 4; m++) {
						blackValue[i][j] += evaluateValue(1, i, j, m);
						whiteValue[i][j] += evaluateValue(2, i, j, m);
					}
				}
			}
		}

		int maxValue = -LARGE_NUMBER;
		int value;
		int[] position = new int[2];
		int[][] valuablePositions = getTheMostValuablePositions();
		for (int i = 0; i < valuablePositions.length; i++) {
			if (valuablePositions[i][2] >= FIVE) {
				position[0] = valuablePositions[i][0];
				position[1] = valuablePositions[i][1];
				break;
			}
			boardStatus[valuablePositions[i][0]][valuablePositions[i][1]] = computerColor;
			value = min(searchDepth, -LARGE_NUMBER, LARGE_NUMBER);
			boardStatus[valuablePositions[i][0]][valuablePositions[i][1]] = 0;
			if (value > maxValue) {
				maxValue = value;
				position[0] = valuablePositions[i][0];
				position[1] = valuablePositions[i][1];
			}
		}
		return position;
	}

	public int snapshotScore() {
		return evaluateGame();
	}

	private int min(int depth, int alpha, int beta) {
		if (depth == 0) {
			return evaluateGame();
		}
		for (int i = 0; i <= COLS; i++) {
			for (int j = 0; j <= ROWS; j++) {
				blackValue[i][j] = 0;
				whiteValue[i][j] = 0;
				if (boardStatus[i][j] == 0) {
					for (int m = 1; m <= 4; m++) {
						blackValue[i][j] += evaluateValue(1, i, j, m);
						whiteValue[i][j] += evaluateValue(2, i, j, m);
					}
				}
			}
		}
		int value;
		int[][] valuablePositions = getTheMostValuablePositions();
		for (int i = 0; i < valuablePositions.length; i++) {
			if (computerColor == 1) {
				if (whiteValue[valuablePositions[i][0]][valuablePositions[i][1]] >= FIVE) {
					return -10 * FIVE;
				}
			} else {
				if (blackValue[valuablePositions[i][0]][valuablePositions[i][1]] >= FIVE) {
					return -10 * FIVE;
				}
			}
			boardStatus[valuablePositions[i][0]][valuablePositions[i][1]] = computerColor == 1 ? 2 : 1;
			value = max(depth - 1, alpha, beta);
			boardStatus[valuablePositions[i][0]][valuablePositions[i][1]] = 0;
			if (value < beta) {
				beta = value;
				if (alpha >= beta) {
					return alpha;
				}
			}
		}
		return beta;
	}

	private int max(int depth, int alpha, int beta) {
		if (depth == 0) {
			return evaluateGame();
		}
		for (int i = 0; i <= COLS; i++) {
			for (int j = 0; j <= ROWS; j++) {
				blackValue[i][j] = 0;
				whiteValue[i][j] = 0;
				if (boardStatus[i][j] == 0) {
					for (int m = 1; m <= 4; m++) {
						blackValue[i][j] += evaluateValue(1, i, j, m);
						whiteValue[i][j] += evaluateValue(2, i, j, m);
					}
				}
			}
		}
		int value;
		int[][] valuablePositions = getTheMostValuablePositions();
		for (int i = 0; i < valuablePositions.length; i++) {
			if (computerColor == 1) {
				if (blackValue[valuablePositions[i][0]][valuablePositions[i][1]] >= FIVE) {
					return 10 * FIVE;
				}
			} else {
				if (whiteValue[valuablePositions[i][0]][valuablePositions[i][1]] >= FIVE) {
					return 10 * FIVE;
				}
			}
			boardStatus[valuablePositions[i][0]][valuablePositions[i][1]] = computerColor;
			value = min(depth - 1, alpha, beta);
			boardStatus[valuablePositions[i][0]][valuablePositions[i][1]] = 0;
			if (value > alpha) {
				alpha = value;
				if (alpha >= beta) {
					return beta;
				}
			}
		}
		return alpha;
	}

	private int evaluateGame() {
		int value = 0;
		int i, j, k;
		int[] line = new int[COLS + 1];
		for (j = 0; j <= ROWS; j++) {
			for (i = 0; i <= COLS; i++) {
				line[i] = boardStatus[i][j];
			}
			value += evaluateLine(line, COLS + 1, 1);
			value -= evaluateLine(line, COLS + 1, 2);
		}
		for (i = 0; i <= COLS; i++) {
			for (j = 0; j <= ROWS; j++) {
				line[j] = boardStatus[i][j];
			}
			value += evaluateLine(line, ROWS + 1, 1);
			value -= evaluateLine(line, ROWS + 1, 2);
		}
		for (j = 4; j <= ROWS; j++) {
			for (k = 0; k <= j; k++) {
				line[k] = boardStatus[k][j - k];
			}
			value += evaluateLine(line, j + 1, 1);
			value -= evaluateLine(line, j + 1, 2);
		}
		for (j = 1; j <= ROWS - 4; j++) {
			for (k = 0; k <= COLS - j; k++) {
				line[k] = boardStatus[k + j][ROWS - k];
			}
			value += evaluateLine(line, ROWS + 1 - j, 1);
			value -= evaluateLine(line, ROWS + 1 - j, 2);
		}
		for (j = 0; j <= ROWS - 4; j++) {
			for (k = 0; k <= ROWS - j; k++) {
				line[k] = boardStatus[k][k + j];
			}
			value += evaluateLine(line, ROWS + 1 - j, 1);
			value -= evaluateLine(line, ROWS + 1 - j, 2);
		}
		for (i = 1; i <= COLS - 4; i++) {
			for (k = 0; k <= ROWS - i; k++) {
				line[k] = boardStatus[k + i][k];
			}
			value += evaluateLine(line, ROWS + 1 - i, 1);
			value -= evaluateLine(line, ROWS + 1 - i, 2);
		}
		return computerColor == 1 ? value : -value;
	}

	private int evaluateLine(int[] lineState, int num, int color) {
		int chess, space1, space2;
		int i, j;
		int value = 0;
		int begin, end;
		for (i = 0; i < num; i++) {
			if (lineState[i] == color) {
				chess = 1;
				begin = i;
				for (j = begin + 1; (j < num) && (lineState[j] == color); j++) {
					chess++;
				}
				if (chess < 2) {
					continue;
				}
				end = j - 1;
				space1 = 0;
				space2 = 0;
				for (j = begin - 1; (j >= 0) && ((lineState[j] == 0) || (lineState[j] == color)); j--) {
					space1++;
				}
				for (j = end + 1; (j < num) && ((lineState[j] == 0) || (lineState[j] == color)); j++) {
					space2++;
				}
				if (chess + space1 + space2 >= 5) {
					value += getValue(chess, space1, space2);
				}
				i = end + 1;
			}
		}
		return value;
	}

	private int getValue(int chessCount, int spaceCount1, int spaceCount2) {
		int value = 0;
		switch (chessCount) {
			case 5:
				value = FIVE;
				break;
			case 4:
				value = (spaceCount1 > 0 && spaceCount2 > 0) ? HUO_FOUR : CHONG_FOUR;
				break;
			case 3:
				value = (spaceCount1 > 0 && spaceCount2 > 0) ? HUO_THREE : MIAN_THREE;
				break;
			case 2:
				if (spaceCount1 > 0 && spaceCount2 > 0) value = HUO_TWO;
				break;
			default:
				value = 0;
				break;
		}
		return value;
	}

	private int evaluateValue(int color, int col, int row, int dir) {
		int k, m;
		int value = 0;
		int chessCount1 = 1;
		int chessCount2 = 0;
		int chessCount3 = 0;
		int spaceCount1 = 0;
		int spaceCount2 = 0;
		int spaceCount3 = 0;
		int spaceCount4 = 0;
		switch (dir) {
			case 1:
				for (k = col + 1; k <= COLS; k++) {
					if (boardStatus[k][row] == color) chessCount1++;
					else break;
				}
				while ((k <= COLS) && (boardStatus[k][row] == 0)) {
					spaceCount1++; k++;
				}
				if (spaceCount1 == 1) {
					while ((k <= COLS) && (boardStatus[k][row] == color)) { chessCount2++; k++; }
					while ((k <= COLS) && (boardStatus[k][row] == 0)) { spaceCount2++; k++; }
				}
				for (k = col - 1; k >= 0; k--) {
					if (boardStatus[k][row] == color) chessCount1++;
					else break;
				}
				while ((k >= 0) && (boardStatus[k][row] == 0)) { spaceCount3++; k--; }
				if (spaceCount3 == 1) {
					while ((k >= 0) && (boardStatus[k][row] == color)) { chessCount3++; k--; }
					while ((k >= 0) && (boardStatus[k][row] == 0)) { spaceCount4++; k--; }
				}
				break;
			case 2:
				for (k = row + 1; k <= ROWS; k++) {
					if (boardStatus[col][k] == color) chessCount1++;
					else break;
				}
				while ((k <= ROWS) && (boardStatus[col][k] == 0)) { spaceCount1++; k++; }
				if (spaceCount1 == 1) {
					while ((k <= ROWS) && (boardStatus[col][k] == color)) { chessCount2++; k++; }
					while ((k <= ROWS) && (boardStatus[col][k] == 0)) { spaceCount2++; k++; }
				}
				for (k = row - 1; k >= 0; k--) {
					if (boardStatus[col][k] == color) chessCount1++;
					else break;
				}
				while ((k >= 0) && (boardStatus[col][k] == 0)) { spaceCount3++; k--; }
				if (spaceCount3 == 1) {
					while ((k >= 0) && (boardStatus[col][k] == color)) { chessCount3++; k--; }
					while ((k >= 0) && (boardStatus[col][k] == 0)) { spaceCount4++; k--; }
				}
				break;
			case 3:
				for (k = col + 1, m = row + 1; (k <= COLS) && (m <= ROWS); k++, m++) {
					if (boardStatus[k][m] == color) chessCount1++;
					else break;
				}
				while ((k <= COLS) && (m <= ROWS) && (boardStatus[k][m] == 0)) {
					spaceCount1++; k++; m++;
				}
				if (spaceCount1 == 1) {
					while ((k <= COLS) && (m <= ROWS) && (boardStatus[k][m] == color)) {
						chessCount2++; k++; m++;
					}
					while ((k <= COLS) && (m <= ROWS) && (boardStatus[k][m] == 0)) {
						spaceCount2++; k++; m++;
					}
				}
				for (k = col - 1, m = row - 1; (k >= 0) && (m >= 0); k--, m--) {
					if (boardStatus[k][m] == color) chessCount1++;
					else break;
				}
				while ((k >= 0) && (m >= 0) && (boardStatus[k][m] == 0)) { spaceCount3++; k--; m--; }
				if (spaceCount3 == 1) {
					while ((k >= 0) && (m >= 0) && (boardStatus[k][m] == color)) {
						chessCount3++; k--; m--;
					}
					while ((k >= 0) && (m >= 0) && (boardStatus[k][m] == 0)) {
						spaceCount4++; k--; m--;
					}
				}
				break;
			case 4:
				for (k = col + 1, m = row - 1; (k <= COLS) && (m >= 0); k++, m--) {
					if (boardStatus[k][m] == color) chessCount1++;
					else break;
				}
				while ((k <= COLS) && (m >= 0) && (boardStatus[k][m] == 0)) {
					spaceCount1++; k++; m--;
				}
				if (spaceCount1 == 1) {
					while ((k <= COLS) && (m >= 0) && (boardStatus[k][m] == color)) {
						chessCount2++; k++; m--;
					}
					while ((k <= COLS) && (m >= 0) && (boardStatus[k][m] == 0)) {
						spaceCount2++; k++; m--;
					}
				}
				for (k = col - 1, m = row + 1; (k >= 0) && (m <= ROWS); k--, m++) {
					if (boardStatus[k][m] == color) chessCount1++;
					else break;
				}
				while ((k >= 0) && (m <= ROWS) && (boardStatus[k][m] == 0)) {
					spaceCount3++; k--; m++;
				}
				if (spaceCount3 == 1) {
					while ((k >= 0) && (m <= ROWS) && (boardStatus[k][m] == color)) {
						chessCount3++; k--; m++;
					}
					while ((k >= 0) && (m <= ROWS) && (boardStatus[k][m] == 0)) {
						spaceCount4++; k--; m++;
					}
				}
				break;
		}
		if (chessCount1 + chessCount2 + chessCount3 + spaceCount1 + spaceCount2 + spaceCount3 + spaceCount4 >= 5) {
			value = getValueDetailed(chessCount1, chessCount2, chessCount3, spaceCount1, spaceCount2, spaceCount3, spaceCount4);
		}
		return value;
	}

	private int getValueDetailed(int chessCount1, int chessCount2, int chessCount3, int spaceCount1, int spaceCount2,
			int spaceCount3, int spaceCount4) {
		int value = 0;
		switch (chessCount1) {
			case 5:
				value = FIVE;
				break;
			case 4:
				value = (spaceCount1 > 0 && spaceCount3 > 0) ? HUO_FOUR : CHONG_FOUR;
				break;
			case 3:
				if (((spaceCount1 == 1) && (chessCount2 >= 1)) && ((spaceCount3 == 1) && (chessCount3 >= 1))) {
					value = HUO_FOUR;
				} else if (((spaceCount1 == 1) && (chessCount2 >= 1)) || ((spaceCount3 == 1) && (chessCount3 >= 1))) {
					value = CHONG_FOUR;
				} else if (((spaceCount1 > 1) && (spaceCount3 > 0)) || ((spaceCount1 > 0) && (spaceCount3 > 1))) {
					value = HUO_THREE;
				} else {
					value = MIAN_THREE;
				}
				break;
			case 2:
				if ((spaceCount1 == 1) && (chessCount2 >= 2) && (spaceCount3 == 1) && (chessCount3 >= 2)) {
					value = HUO_FOUR;
				} else if (((spaceCount1 == 1) && (chessCount2 >= 2)) || ((spaceCount3 == 1) && (chessCount3 >= 2))) {
					value = CHONG_FOUR;
				} else if (((spaceCount1 == 1) && (chessCount2 == 1) && (spaceCount2 > 0) && (spaceCount3 > 0))
						|| (((spaceCount3 == 1) && (chessCount3 == 1)) && (spaceCount1 > 0) && (spaceCount4 > 0))) {
					value = HUO_THREE;
				} else if ((spaceCount1 > 0) && (spaceCount4 > 0)) {
					value = HUO_TWO;
				}
				break;
			case 1:
				// 修复：原来是 (spaceCount3 == 1) && (spaceCount3 >= 3) —— 同一变量自相矛盾，永远为 false
				if (((spaceCount1 == 1) && (chessCount2 >= 3)) || ((spaceCount3 == 1) && (chessCount3 >= 3))) {
					value = CHONG_FOUR;
				} else if (((spaceCount1 == 1) && (chessCount2 == 2) && (spaceCount2 >= 1) && (spaceCount3 >= 1))
						|| ((spaceCount3 == 1) && (chessCount3 == 2) && (spaceCount1 >= 1) && (spaceCount4 >= 1))) {
					value = HUO_THREE;
				} else if (((spaceCount1 == 1) && (chessCount2 == 2) && ((spaceCount2 >= 1) || (spaceCount3 >= 1)))
						|| ((spaceCount3 == 1) && (chessCount3 == 2) && ((spaceCount1 >= 1) || (spaceCount4 >= 1)))) {
					value = MIAN_THREE;
				} else if (((spaceCount1 == 1) && (chessCount2 == 2) && (spaceCount2 > 1) && (spaceCount3 > 0))
						|| ((spaceCount3 == 1) && (chessCount3 == 1) && (spaceCount1 > 0) && (spaceCount4 > 1))) {
					value = HUO_TWO;
				}
				break;
			default:
				value = 0;
				break;
		}
		return value;
	}

	private int[][] getTheMostValuablePositions() {
		int i, j, k = 0;
		int[][] allValue = new int[(COLS + 1) * (ROWS + 1)][3];
		for (i = 0; i <= COLS; i++) {
			for (j = 0; j <= ROWS; j++) {
				if (boardStatus[i][j] == 0) {
					allValue[k][0] = i;
					allValue[k][1] = j;
					allValue[k][2] = blackValue[i][j] + whiteValue[i][j] + staticValue[i][j];
					k++;
				}
			}
		}

		sort(allValue, k);
		int size = Math.min(k, sampleNumber);
		int[][] valuablePositions = new int[size][3];
		for (i = 0; i < size; i++) {
			valuablePositions[i][0] = allValue[i][0];
			valuablePositions[i][1] = allValue[i][1];
			valuablePositions[i][2] = allValue[i][2];
		}
		return valuablePositions;
	}

	private void sort(int[][] allValue, int size) {
		for (int i = 0; i < size - 1; i++) {
			for (int j = 0; j < size - 1 - i; j++) {
				if (allValue[j][2] < allValue[j + 1][2]) {
					int[] tmp = allValue[j];
					allValue[j] = allValue[j + 1];
					allValue[j + 1] = tmp;
				}
			}
		}
	}
}
