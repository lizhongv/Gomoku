// 补充端到端测试：GIVEUP + TALK + 含特殊字符的聊天内容
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import protocol.Command;
import protocol.Message;
import server.FiveServer;
import user.User;

public class E2eTest2 {
	static final String HOST = "127.0.0.1";
	static final int PORT = FiveServer.TCP_PORT;
	static int passed = 0, failed = 0;

	public static void main(String[] args) throws Exception {
		String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
		String aName = "e2e2_a_" + suffix;
		String bName = "e2e2_b_" + suffix;
		String pwd = "p123";

		register(aName, pwd);
		register(bName, pwd);

		TestClient a = login(aName, pwd);
		TestClient b = login(bName, pwd);
		a.startReader();
		b.startReader();
		a.expect(Command.TELLNAME);
		b.expect(Command.TELLNAME);
		b.expect(Command.ADD);
		a.expect(Command.ADD);

		System.out.println("=== TALK with special chars ===");
		String special = "hello: world :: test : 中文 :end:";
		Message.send(a.dos, Command.TALK, special);
		Message t1 = a.expect(Command.TALK);
		Message t2 = b.expect(Command.TALK);
		eq("alice 收到 sender", aName, t1.arg(0));
		eq("alice 收到 content（含冒号原样）", special, t1.arg(1));
		eq("bob 收到 sender", aName, t2.arg(0));
		eq("bob 收到 content（含冒号原样）", special, t2.arg(1));

		System.out.println("=== 邀请 → 接受 ===");
		Message.send(a.dos, Command.JOIN, bName, "60");
		b.expect(Command.JOIN);
		Message.send(b.dos, Command.AGREE, aName);
		a.expect(Command.CHANGE); a.expect(Command.CHANGE);
		b.expect(Command.CHANGE); b.expect(Command.CHANGE);
		Message ac = a.expect(Command.GUESSCOLOR);
		Message bc = b.expect(Command.GUESSCOLOR);
		TestClient black = "black".equals(ac.arg(0)) ? a : b;
		TestClient white = "black".equals(ac.arg(0)) ? b : a;
		System.out.println("alice=" + ac.arg(0) + ", bob=" + bc.arg(0));

		System.out.println("=== 黑下一手 → 白认输 ===");
		Message.send(black.dos, Command.GO, "9", "9");
		white.expect(Command.GO);
		Message.send(white.dos, Command.GIVEUP);
		white.expect(Command.CHANGE); white.expect(Command.CHANGE);
		Message wRes = white.expect(Command.TELLRESULT);
		eq("认输方收到 losses", "losses", wRes.arg(0));
		black.expect(Command.CHANGE); black.expect(Command.CHANGE);
		Message bRes = black.expect(Command.TELLRESULT);
		eq("对手收到 win", "win", bRes.arg(0));

		Thread.sleep(200);
		String winner = "black".equals(ac.arg(0)) ? aName : bName;
		ok("GIVEUP 也写库", dbHasGame(winner));

		System.out.println("=== 重复对局：回到 ready 后再战一次 ===");
		Message.send(a.dos, Command.JOIN, bName, "60");
		b.expect(Command.JOIN);
		Message.send(b.dos, Command.AGREE, aName);
		a.expect(Command.CHANGE); a.expect(Command.CHANGE);
		b.expect(Command.CHANGE); b.expect(Command.CHANGE);
		Message ac2 = a.expect(Command.GUESSCOLOR);
		b.expect(Command.GUESSCOLOR);
		ok("第二局成功开始", ac2 != null);
		Message.send(a.dos, Command.QUIT);
		Thread.sleep(300);
		Message.send(b.dos, Command.QUIT);
		Thread.sleep(300);
		a.close(); b.close();

		cleanup(aName, bName);
		System.out.println("\n结果: passed=" + passed + " failed=" + failed);
		System.exit(failed == 0 ? 0 : 1);
	}

	static void register(String n, String p) throws IOException {
		try (Socket s = new Socket(HOST, PORT)) {
			DataInputStream dis = new DataInputStream(s.getInputStream());
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			Message.send(dos, Command.REGISTER);
			ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			oos.writeObject(new User(n, p, n + "@e.com", 1, new java.sql.Date(System.currentTimeMillis())));
			oos.flush();
			Message.read(dis);
		}
	}

	static TestClient login(String n, String p) throws IOException {
		Socket s = new Socket(HOST, PORT);
		DataInputStream dis = new DataInputStream(s.getInputStream());
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		Message.send(dos, Command.LOGIN, n, p);
		Message.read(dis);
		return new TestClient(s, dis, dos);
	}

	static boolean dbHasGame(String winner) {
		if (user.StorageMode.useFiles()) {
			return game.FileGameStore.countWins(winner) >= 1;
		}
		try (java.sql.Connection c = java.sql.DriverManager.getConnection(
				user.DBConnection.getUrl(), user.DBConnection.getUser(), user.DBConnection.getPassword());
				java.sql.PreparedStatement p = c.prepareStatement("select count(*) from game where winner = ?")) {
			p.setString(1, winner);
			java.sql.ResultSet rs = p.executeQuery();
			return rs.next() && rs.getInt(1) >= 1;
		} catch (Exception e) {
			return false;
		}
	}

	static void cleanup(String... users) {
		if (user.StorageMode.useFiles()) {
			for (String u : users) {
				user.FileUserStore.removeUser(u);
				game.FileGameStore.removeAllForUser(u);
			}
			return;
		}
		try (java.sql.Connection c = java.sql.DriverManager.getConnection(
				user.DBConnection.getUrl(), user.DBConnection.getUser(), user.DBConnection.getPassword())) {
			for (String u : users) {
				try (java.sql.PreparedStatement p1 = c.prepareStatement("delete from user where name = ?")) {
					p1.setString(1, u); p1.executeUpdate();
				}
				try (java.sql.PreparedStatement p2 = c.prepareStatement(
						"delete from game where playerBlack = ? or playerWhite = ?")) {
					p2.setString(1, u); p2.setString(2, u); p2.executeUpdate();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class TestClient {
		Socket s; DataInputStream dis; DataOutputStream dos;
		LinkedBlockingQueue<Message> q = new LinkedBlockingQueue<>();
		TestClient(Socket s, DataInputStream dis, DataOutputStream dos) { this.s = s; this.dis = dis; this.dos = dos; }
		void startReader() {
			Thread t = new Thread(() -> {
				try { while (true) q.put(Message.read(dis)); } catch (Exception ignored) { }
			}); t.setDaemon(true); t.start();
		}
		Message expect(String cmd) throws InterruptedException {
			long dl = System.currentTimeMillis() + 5000;
			while (System.currentTimeMillis() < dl) {
				Message m = q.poll(dl - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (m == null) break;
				if (cmd.equals(m.command())) return m;
			}
			failed++; System.out.println("  ✗ 超时: " + cmd); return null;
		}
		void close() throws IOException { s.close(); }
	}

	static void ok(String n, boolean c) { if (c) { passed++; System.out.println("  ✓ " + n); } else { failed++; System.out.println("  ✗ " + n); } }
	static void eq(String n, String e, String a) { if (e.equals(a)) { passed++; System.out.println("  ✓ " + n); } else { failed++; System.out.println("  ✗ " + n + " expected=[" + e + "] actual=[" + a + "]"); } }
}
