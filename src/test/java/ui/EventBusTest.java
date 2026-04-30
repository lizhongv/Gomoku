package ui;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

public class EventBusTest {
    static int passed = 0, failed = 0;
    static class FooEvent implements Event { public final int v; public FooEvent(int v){this.v=v;} }
    static class BarEvent implements Event {}

    public static void main(String[] args) throws Exception {
        EventBus.reset(); testSubscribePublish();
        EventBus.reset(); testUnsubscribe();
        EventBus.reset(); testWrongTypeNoTrigger();
        System.out.println("结果: passed=" + passed + " failed=" + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void testSubscribePublish() throws Exception {
        AtomicInteger c = new AtomicInteger(0);
        EventBus.subscribe(FooEvent.class, e -> c.addAndGet(e.v));
        EventBus.publish(new FooEvent(7));
        SwingUtilities.invokeAndWait(() -> {});
        ok("收到 FooEvent v=7", c.get() == 7);
    }

    static void testUnsubscribe() throws Exception {
        AtomicInteger c = new AtomicInteger(0);
        java.util.function.Consumer<FooEvent> h = e -> c.incrementAndGet();
        EventBus.subscribe(FooEvent.class, h);
        EventBus.unsubscribe(FooEvent.class, h);
        EventBus.publish(new FooEvent(1));
        SwingUtilities.invokeAndWait(() -> {});
        ok("取消订阅后不再触发", c.get() == 0);
    }

    static void testWrongTypeNoTrigger() throws Exception {
        AtomicInteger c = new AtomicInteger(0);
        EventBus.subscribe(FooEvent.class, e -> c.incrementAndGet());
        EventBus.publish(new BarEvent());
        SwingUtilities.invokeAndWait(() -> {});
        ok("Bar 事件不触发 Foo 订阅", c.get() == 0);
    }

    static void ok(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  ✓ " + name); }
        else      { failed++; System.out.println("  ✗ " + name); }
    }
}
