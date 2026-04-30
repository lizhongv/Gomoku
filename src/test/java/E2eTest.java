// 端到端测试：模拟两个客户端走完 注册→登录→邀请→对局→胜负→记录 全流程
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import protocol.Command;
import protocol.Message;
import protocol.Net;
import server.FiveServer;
import user.User;

public class E2eTest {
	static final String HOST = "127.0.0.1";
	static final int PORT = FiveServer.TCP_PORT;
	static int passed = 0;
	static int failed = 0;

	public static void main(String[] args) throws Exception {
		String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
		String aliceName = "e2e_a_" + suffix;
		String bobName = "e2e_b_" + suffix;
		String pwd = "p_" + new Random().nextInt(1000);

		log("=== Phase 1: 注册两个用户 ===");
		assertTrue("注册 alice", register(aliceName, pwd, "alice@e.com"));
		assertTrue("注册 bob", register(bobName, pwd, "bob@e.com"));
		assertFalse("重复注册应失败", register(aliceName, pwd, "alice@e.com"));
		log("用户名校验：");
		assertFalse("含冒号的用户名应被拒", register("bad:name_" + suffix, pwd, "x@e.com"));

		log("=== Phase 2: 登录 + 用户列表广播 ===");
		Object[] aliceConn = login(aliceName, pwd);
		TestClient alice = (TestClient) aliceConn[1];
		assertTrue("alice 登录成功", aliceConn[0] != null);
		alice.startReader();
		Message tn1 = alice.expect(Command.TELLNAME);
		assertEq("TELLNAME(alice)", aliceName, tn1.arg(0));

		Object[] bobConn = login(bobName, pwd);
		TestClient bob = (TestClient) bobConn[1];
		assertTrue("bob 登录成功", bobConn[0] != null);
		bob.startReader();
		Message tn2 = bob.expect(Command.TELLNAME);
		assertEq("TELLNAME(bob)", bobName, tn2.arg(0));
		Message bobAddsAlice = bob.expect(Command.ADD);
		assertEq("bob 看到 alice", aliceName, bobAddsAlice.arg(0));
		Message aliceAddsBob = alice.expect(Command.ADD);
		assertEq("alice 看到 bob", bobName, aliceAddsBob.arg(0));

		log("=== Phase 3: 重复登录拒绝 ===");
		Object[] dup = login(aliceName, pwd);
		assertTrue("重复登录被拒绝", dup[0] == null);

		log("=== Phase 4: 邀请 + 接受 + 颜色分配 ===");
		Message.send(alice.dos, Command.JOIN, bobName, "300");
		Message bobJoin = bob.expect(Command.JOIN);
		assertEq("bob 收到邀请", aliceName, bobJoin.arg(0));
		assertEq("时长", "300", bobJoin.arg(1));
		Message.send(bob.dos, Command.AGREE, aliceName);

		alice.expect(Command.CHANGE);
		alice.expect(Command.CHANGE);
		bob.expect(Command.CHANGE);
		bob.expect(Command.CHANGE);
		Message aliceColor = alice.expect(Command.GUESSCOLOR);
		Message bobColor = bob.expect(Command.GUESSCOLOR);
		String aColor = aliceColor.arg(0);
		String bColor = bobColor.arg(0);
		assertTrue("alice 颜色合法", "black".equals(aColor) || "white".equals(aColor));
		assertTrue("颜色互斥", !aColor.equals(bColor));
		log("alice=" + aColor + ", bob=" + bColor);

		TestClient black = "black".equals(aColor) ? alice : bob;
		TestClient white = "black".equals(aColor) ? bob : alice;

		log("=== Phase 5: 落子（黑横向五连） ===");
		int[][] blackMoves = { { 5, 5 }, { 6, 5 }, { 7, 5 }, { 8, 5 }, { 9, 5 } };
		int[][] whiteMoves = { { 5, 6 }, { 6, 6 }, { 7, 6 }, { 8, 6 } };
		for (int i = 0; i < 4; i++) {
			Message.send(black.dos, Command.GO, "" + blackMoves[i][0], "" + blackMoves[i][1]);
			Message gOnW = white.expect(Command.GO);
			assertEq("white 收到黑子 col", "" + blackMoves[i][0], gOnW.arg(0));
			Message.send(white.dos, Command.GO, "" + whiteMoves[i][0], "" + whiteMoves[i][1]);
			Message gOnB = black.expect(Command.GO);
			assertEq("black 收到白子 col", "" + whiteMoves[i][0], gOnB.arg(0));
		}
		Message.send(black.dos, Command.GO, "9", "5");
		white.expect(Command.GO);

		log("=== Phase 6: 黑方宣布胜利 ===");
		Message.send(black.dos, Command.WIN);
		black.expect(Command.CHANGE);
		black.expect(Command.CHANGE);
		Message blackRes = black.expect(Command.TELLRESULT);
		assertEq("黑方收到 win", "win", blackRes.arg(0));
		white.expect(Command.CHANGE);
		white.expect(Command.CHANGE);
		Message whiteRes = white.expect(Command.TELLRESULT);
		assertEq("白方收到 losses", "losses", whiteRes.arg(0));

		log("=== Phase 7: 数据库 + 棋谱文件落地 ===");
		Thread.sleep(200);
		String winner = "black".equals(aColor) ? aliceName : bobName;
		if (user.StorageMode.useFiles()) {
			assertTrue("文件存储中存在该胜局", game.FileGameStore.countWins(winner) >= 1);
		} else {
			assertTrue("DB 中存在该胜局", dbHasGame(winner));
		}
		assertTrue("manuals/ 下有 .fiv 棋谱", manualFileExists(winner));

		log("=== Phase 8: 历史棋局查询 ===");
		Message.send(alice.dos, Command.GAME, aliceName);
		Message gameAck = alice.expect(Command.GAME);
		assertTrue("收到 GAME 应答", gameAck != null);

		log("=== Phase 9: 退出 ===");
		Message.send(alice.dos, Command.QUIT);
		Message.send(bob.dos, Command.QUIT);
		Thread.sleep(300);
		alice.close();
		bob.close();

		log("=== 清理测试数据 ===");
		cleanupDb(aliceName, bobName);

		log("");
		log("结果: passed=" + passed + " failed=" + failed);
		System.exit(failed == 0 ? 0 : 1);
	}

	static boolean register(String name, String pwd, String email) {
		try (Socket s = new Socket(HOST, PORT)) {
			DataInputStream dis = new DataInputStream(s.getInputStream());
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			Message.send(dos, Command.REGISTER);
			ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			oos.writeObject(new User(name, pwd, email, 1, new java.sql.Date(System.currentTimeMillis())));
			oos.flush();
			Message reply = Message.read(dis);
			return Command.REGISTER.equals(reply.command()) && "true".equals(reply.argOr(0, ""));
		} catch (IOException e) {
			return false;
		}
	}

	static Object[] login(String name, String pwd) {
		try {
			Socket s = new Socket(HOST, PORT);
			DataInputStream dis = new DataInputStream(s.getInputStream());
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			Message.send(dos, Command.LOGIN, name, pwd);
			Message reply = Message.read(dis);
			if (Command.LOGIN.equals(reply.command()) && "true".equals(reply.argOr(0, ""))) {
				TestClient tc = new TestClient(s, dis, dos);
				return new Object[] { Boolean.TRUE, tc };
			}
			s.close();
			return new Object[] { null, null };
		} catch (IOException e) {
			return new Object[] { null, null };
		}
	}

	static boolean dbHasGame(String winner) {
		try (java.sql.Connection c = java.sql.DriverManager.getConnection(
				user.DBConnection.getUrl(), user.DBConnection.getUser(), user.DBConnection.getPassword());
				java.sql.PreparedStatement p = c.prepareStatement("select count(*) from game where winner = ?")) {
			p.setString(1, winner);
			java.sql.ResultSet rs = p.executeQuery();
			return rs.next() && rs.getInt(1) >= 1;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	static boolean manualFileExists(String winner) {
		java.io.File dir = game.ManualDao.getDir();
		java.io.File[] files = dir.listFiles();
		if (files == null) return false;
		for (java.io.File f : files) {
			if (f.getName().endsWith(".fiv") && f.getName().contains(winner)) {
				return true;
			}
		}
		return false;
	}

	static void cleanupDb(String... users) {
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
					p1.setString(1, u);
					p1.executeUpdate();
				}
				try (java.sql.PreparedStatement p2 = c.prepareStatement(
						"delete from game where playerBlack = ? or playerWhite = ?")) {
					p2.setString(1, u);
					p2.setString(2, u);
					p2.executeUpdate();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class TestClient {
		Socket s;
		DataInputStream dis;
		DataOutputStream dos;
		LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();

		TestClient(Socket s, DataInputStream dis, DataOutputStream dos) {
			this.s = s;
			this.dis = dis;
			this.dos = dos;
		}

		void startReader() {
			Thread t = new Thread(() -> {
				try {
					while (true) {
						queue.put(Message.read(dis));
					}
				} catch (Exception e) {
				}
			}, "reader");
			t.setDaemon(true);
			t.start();
		}

		Message expect(String cmd) throws InterruptedException {
			long deadline = System.currentTimeMillis() + 5000;
			while (System.currentTimeMillis() < deadline) {
				Message m = queue.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (m == null) break;
				if (cmd.equals(m.command())) return m;
				log("  跳过非预期帧: " + m.command());
			}
			fail("超时等待 " + cmd);
			return null;
		}

		void close() throws IOException {
			s.close();
		}
	}

	static void assertTrue(String name, boolean cond) {
		if (cond) {
			passed++;
			log("  ✓ " + name);
		} else {
			failed++;
			log("  ✗ " + name);
		}
	}

	static void assertFalse(String name, boolean cond) {
		assertTrue(name, !cond);
	}

	static void assertEq(String name, String expected, String actual) {
		if (expected == null ? actual == null : expected.equals(actual)) {
			passed++;
			log("  ✓ " + name + " = " + actual);
		} else {
			failed++;
			log("  ✗ " + name + " expected=[" + expected + "] actual=[" + actual + "]");
		}
	}

	static void fail(String msg) {
		failed++;
		log("  ✗ " + msg);
	}

	static void log(String msg) {
		System.out.println(msg);
	}
}
