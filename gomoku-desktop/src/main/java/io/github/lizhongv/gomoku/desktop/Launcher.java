package io.github.lizhongv.gomoku.desktop;

/**
 * 启动器：jpackage 以 classpath 方式运行时，若主类直接继承 Application， JavaFX 会检查模块路径并报「缺少 JavaFX
 * 运行时组件」。用一个普通类转发即可绕开该检查。
 */
public final class Launcher {

  private Launcher() {}

  /** 入口。 */
  public static void main(String[] args) {
    GomokuApp.main(args);
  }
}
