package ui.scene;
import javax.swing.JComponent;
public interface Scene {
    JComponent root();
    String title();
    default void onEnter() {}
    default void onLeave() {}
}
