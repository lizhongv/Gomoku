package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.Stone;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * 棋盘绘制与点击命中。
 *
 * <p>棋盘尺寸随窗口缩放：以较短边为准留出边距，保证横竖比例一致。棋子用径向渐变绘制高光， 最后一手标红点，提示点画光环，悬停显示半透明落子预览。
 */
public final class BoardCanvas extends Canvas {

  private static final Color WOOD_LIGHT = Color.web("#E9CB93");

  private static final Color WOOD_DARK = Color.web("#D3AE72");

  private static final Color GRID = Color.web("#6C4B26");

  private Board board = new Board(15);

  private boolean showCoordinates = true;

  private boolean readOnly;

  private Move lastMove;

  private Move hover;

  private List<Move> hints = List.of();

  private BiConsumer<Move, Boolean> onPlace = (move, valid) -> {};

  /** 创建棋盘画布。 */
  public BoardCanvas() {
    widthProperty().addListener((observable, oldValue, newValue) -> redraw());
    heightProperty().addListener((observable, oldValue, newValue) -> redraw());
    setOnMouseMoved(
        event -> {
          Move candidate = pointAt(event.getX(), event.getY());
          if (candidate == null ? hover != null : !candidate.equals(hover)) {
            hover = candidate;
            redraw();
          }
        });
    setOnMouseExited(
        event -> {
          hover = null;
          redraw();
        });
    setOnMouseClicked(
        event -> {
          if (readOnly || board == null) {
            return;
          }
          Move candidate = pointAt(event.getX(), event.getY());
          if (candidate != null) {
            onPlace.accept(candidate, board.isEmpty(candidate.row(), candidate.col()));
          }
        });
  }

  /** 设置棋盘引用。 */
  public void setBoard(Board board) {
    this.board = board;
    redraw();
  }

  /** 是否只读（复盘时）。 */
  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  /** 坐标显示开关。 */
  public void setShowCoordinates(boolean show) {
    this.showCoordinates = show;
    redraw();
  }

  /** 最后一手标记。 */
  public void setLastMove(Move move) {
    this.lastMove = move;
    redraw();
  }

  /** 提示点。 */
  public void setHints(List<Move> hints) {
    this.hints = hints == null ? List.of() : List.copyOf(hints);
    redraw();
  }

  /** 点击回调：是否落在合法空点。 */
  public void setOnPlace(BiConsumer<Move, Boolean> onPlace) {
    this.onPlace = onPlace == null ? (move, valid) -> {} : onPlace;
  }

  /** 重绘。 */
  public void redraw() {
    GraphicsContext gc = getGraphicsContext2D();
    double width = getWidth();
    double height = getHeight();
    if (width <= 0 || height <= 0) {
      return;
    }
    gc.clearRect(0, 0, width, height);
    double margin = Math.min(width, height) * 0.065;
    double cell = (Math.min(width, height) - 2 * margin) / Math.max(1, board.size() - 1);
    double boardSize = cell * (board.size() - 1);
    double originX = (width - boardSize) / 2;
    double originY = (height - boardSize) / 2;

    gc.setFill(
        new LinearGradient(
            0,
            0,
            1,
            1,
            true,
            CycleMethod.NO_CYCLE,
            new Stop(0, WOOD_LIGHT),
            new Stop(1, WOOD_DARK)));
    gc.fillRoundRect(
        originX - margin * 0.55,
        originY - margin * 0.55,
        boardSize + margin * 1.1,
        boardSize + margin * 1.1,
        18,
        18);

    gc.setStroke(GRID);
    gc.setLineWidth(1.1);
    for (int i = 0; i < board.size(); i++) {
      double offset = i * cell;
      gc.strokeLine(originX, originY + offset, originX + boardSize, originY + offset);
      gc.strokeLine(originX + offset, originY, originX + offset, originY + boardSize);
    }
    drawStarPoints(gc, originX, originY, cell, board.size());

    if (showCoordinates) {
      drawCoordinates(gc, originX, originY, cell, board.size());
    }

    double radius = Math.max(4, cell * 0.42);
    for (int row = 0; row < board.size(); row++) {
      for (int col = 0; col < board.size(); col++) {
        Stone stone = board.get(row, col);
        if (stone != Stone.EMPTY) {
          drawStone(gc, originX + col * cell, originY + row * cell, radius, stone, false);
        }
      }
    }
    if (hover != null && board.isEmpty(hover.row(), hover.col()) && !readOnly) {
      drawStone(
          gc,
          originX + hover.col() * cell,
          originY + hover.row() * cell,
          radius,
          board.moveCount() % 2 == 0 ? Stone.BLACK : Stone.WHITE,
          true);
    }
    if (lastMove != null) {
      double x = originX + lastMove.col() * cell;
      double y = originY + lastMove.row() * cell;
      gc.setFill(Color.web("#E5484D"));
      gc.fillOval(x - radius * 0.22, y - radius * 0.22, radius * 0.44, radius * 0.44);
    }
    gc.setStroke(Color.web("#2F80ED"));
    gc.setLineWidth(2.4);
    for (Move hint : hints) {
      double x = originX + hint.col() * cell;
      double y = originY + hint.row() * cell;
      gc.strokeOval(x - radius * 1.15, y - radius * 1.15, radius * 2.3, radius * 2.3);
    }
  }

  private void drawStone(
      GraphicsContext gc, double x, double y, double radius, Stone stone, boolean ghost) {
    double alpha = ghost ? 0.35 : 1;
    gc.setGlobalAlpha(alpha);
    gc.setFill(Color.rgb(0, 0, 0, 0.18));
    gc.fillOval(x - radius * 1.02, y - radius * 0.92, radius * 2.04, radius * 2.04);
    if (stone == Stone.BLACK) {
      gc.setFill(
          new RadialGradient(
              0,
              0,
              0.35,
              0.3,
              0.9,
              true,
              CycleMethod.NO_CYCLE,
              new Stop(0, Color.web("#6B6B6B")),
              new Stop(0.45, Color.web("#1E1E1E")),
              new Stop(1, Color.web("#050505"))));
    } else {
      gc.setFill(
          new RadialGradient(
              0,
              0,
              0.35,
              0.3,
              0.95,
              true,
              CycleMethod.NO_CYCLE,
              new Stop(0, Color.WHITE),
              new Stop(0.65, Color.web("#EDEDED")),
              new Stop(1, Color.web("#B9B9B9"))));
      gc.setStroke(Color.web("#9A9A9A"));
      gc.setLineWidth(0.8);
      gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
    }
    gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    gc.setGlobalAlpha(1);
  }

  private void drawStarPoints(GraphicsContext gc, double ox, double oy, double cell, int size) {
    int[] points = size >= 19 ? new int[] {3, 9, 15} : new int[] {3, 7, 11};
    gc.setFill(Color.web("#4A3218"));
    for (int row : points) {
      for (int col : points) {
        if (row >= size || col >= size) {
          continue;
        }
        double r = Math.max(2, cell * 0.11);
        gc.fillOval(ox + col * cell - r, oy + row * cell - r, r * 2, r * 2);
      }
    }
  }

  private void drawCoordinates(GraphicsContext gc, double ox, double oy, double cell, int size) {
    gc.setFill(Color.web("#5A4123"));
    gc.setFont(Font.font(10));
    gc.setTextAlign(TextAlignment.CENTER);
    for (int i = 0; i < size; i++) {
      char letter = (char) ('A' + i);
      gc.fillText(String.valueOf(letter), ox + i * cell, oy - cell * 0.55);
      gc.fillText(String.valueOf(size - i), ox - cell * 0.55, oy + i * cell + 3.5);
    }
    gc.setTextAlign(TextAlignment.LEFT);
  }

  private Move pointAt(double x, double y) {
    double width = getWidth();
    double height = getHeight();
    double margin = Math.min(width, height) * 0.065;
    double cell = (Math.min(width, height) - 2 * margin) / Math.max(1, board.size() - 1);
    double boardSize = cell * (board.size() - 1);
    double originX = (width - boardSize) / 2;
    double originY = (height - boardSize) / 2;
    int col = (int) Math.round((x - originX) / cell);
    int row = (int) Math.round((y - originY) / cell);
    if (row < 0 || row >= board.size() || col < 0 || col >= board.size()) {
      return null;
    }
    if (Math.hypot(x - (originX + col * cell), y - (originY + row * cell)) > cell * 0.62) {
      return null;
    }
    return new Move(row, col);
  }
}
