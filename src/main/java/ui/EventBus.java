// 简单进程内事件总线：发布 / 订阅 / 取消订阅
// 所有回调在 Swing EDT 触发
package ui;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

public final class EventBus {
    private static final Map<Class<? extends Event>, CopyOnWriteArrayList<Consumer<Event>>> SUBS = new HashMap<>();
    private EventBus() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static synchronized <T extends Event> void subscribe(Class<T> type, Consumer<T> handler) {
        SUBS.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add((Consumer) handler);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static synchronized <T extends Event> void unsubscribe(Class<T> type, Consumer<T> handler) {
        List<Consumer<Event>> list = SUBS.get(type);
        if (list != null) list.remove((Consumer) handler);
    }

    public static void publish(Event e) {
        List<Consumer<Event>> list;
        synchronized (EventBus.class) { list = SUBS.get(e.getClass()); }
        if (list == null || list.isEmpty()) return;
        List<Consumer<Event>> snapshot = new ArrayList<>(list);
        Runnable r = () -> {
            for (Consumer<Event> h : snapshot) {
                try { h.accept(e); } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    public static synchronized void reset() { SUBS.clear(); }
}
