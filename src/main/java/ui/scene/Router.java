// 场景路由：维护历史栈
package ui.scene;

import java.awt.CardLayout;
import java.util.Deque;
import java.util.ArrayDeque;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Router {
    private final JFrame frame;
    private final JPanel cardHolder;
    private final CardLayout cards;
    private final Deque<Scene> stack = new ArrayDeque<>();

    public Router(JFrame frame) {
        this.frame = frame;
        this.cards = new CardLayout();
        this.cardHolder = new JPanel(cards);
    }

    public JPanel cardHolder() { return cardHolder; }
    public Scene current() { return stack.peek(); }

    public void push(Scene s) { stack.push(s); showScene(s); }

    public void replace(Scene s) {
        if (!stack.isEmpty()) {
            Scene old = stack.pop();
            old.onLeave();
            cardHolder.remove(old.root());
        }
        stack.push(s);
        showScene(s);
    }

    public void pop() {
        if (stack.size() <= 1) return;
        Scene old = stack.pop();
        old.onLeave();
        cardHolder.remove(old.root());
        showScene(stack.peek());
    }

    private void showScene(Scene s) {
        String key = s.getClass().getName() + "@" + System.identityHashCode(s);
        boolean exists = false;
        for (java.awt.Component c : cardHolder.getComponents()) {
            if (c == s.root()) { exists = true; break; }
        }
        if (!exists) cardHolder.add(s.root(), key);
        cards.show(cardHolder, key);
        if (frame != null) frame.setTitle(s.title());
        s.onEnter();
    }
}
