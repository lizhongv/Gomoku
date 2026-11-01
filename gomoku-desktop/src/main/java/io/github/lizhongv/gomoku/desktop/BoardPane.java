package io.github.lizhongv.gomoku.desktop;

import javafx.scene.layout.Pane;

/**
 * 按可用空间调整棋盘画布大小的容器。
 *
 * <p>不能把 Canvas 的宽高直接绑定到父容器：Canvas 的 pref 尺寸会反过来撑大父容器，
 * 窗口变小时棋盘溢出、底部行数被裁掉。这里在布局阶段显式设置画布尺寸，父容器始终取可用空间。
 */
public final class BoardPane extends Pane {

  private final BoardCanvas canvas;

  /** 包装一个棋盘画布。 */
  public BoardPane(BoardCanvas canvas) {
    this.canvas = canvas;
    getChildren().add(canvas);
    setMinSize(0, 0);
  }

  /** 内部画布。 */
  public BoardCanvas canvas() {
    return canvas;
  }

  @Override
  protected void layoutChildren() {
    double width = Math.max(1, getWidth());
    double height = Math.max(1, getHeight());
    canvas.setWidth(width);
    canvas.setHeight(height);
    canvas.relocate(0, 0);
    canvas.redraw();
  }
}
