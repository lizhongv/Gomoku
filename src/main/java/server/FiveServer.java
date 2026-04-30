// 五子棋服务器：监听 4801（默认），处理登录 / 注册 / 对局 / 棋谱写库
package server;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import game.Game;
import game.GameDao;
import game.Manual;
import game.ManualDao;
import protocol.Command;
import protocol.Message;
import protocol.Net;
import protocol.SafeObjectInputStream;
import user.User;
import user.UserDao;

public class FiveServer extends Frame implements ActionListener {
	Label lStatus = new Label("当前连接数:0", Label.LEFT);
	TextArea taMessage = new TextArea("", 22, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
	Button btServerClose = new Button("关闭服务器");

	ServerSocket ss = null;
	public static final int TCP_PORT = Net.DEFAULT_PORT;
	static int clientNum = 0;
	CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<Client>();

	public static void main(String[] args) {
		System.out.println("[FiveServer] 存储模式: " + user.StorageMode.current());
		FiveServer fs = new FiveServer();
		fs.startServser();
	}

	public FiveServer() {
		super("JAVA 五子棋服务器");
		btServerClose.addActionListener(this);
		add(lStatus, BorderLayout.NORTH);
		add(taMessage, BorderLayout.CENTER);
		add(btServerClose, BorderLayout.SOUTH);
		setLocation(500, 200);
		pack();
		setVisible(true);
		setResizable(false);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btServerClose) {
			System.exit(0);
		}
	}

	class Client {
		String name;
		Socket s;
		DataInputStream dis;
		DataOutputStream dos;
		String state;
		Client opponent;
		String chessColor;
		int step;
		int[][] coordinates;

		Client(String name, Socket s, DataInputStream dis, DataOutputStream dos) {
			this.name = name;
			this.s = s;
			this.dis = dis;
			this.dos = dos;
			this.state = "ready";
			this.opponent = null;
			this.step = 0;
			this.coordinates = new int[19 * 19][2];
		}
	}

	class ClientThread extends Thread {
		private final Client c;

		ClientThread(Client c) {
			this.c = c;
		}

		public void run() {
			try {
				while (true) {
					Message m = Message.read(c.dis);
					if (!handle(m)) {
						return;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				cleanupOnDrop();
			}
		}

		private boolean handle(Message m) throws IOException {
			String cmd = m.command();
			if (Command.JOIN.equals(cmd)) {
				String opponentName = m.arg(0);
				String playingTime = m.arg(1);
				Client opp = findByName(opponentName);
				if (opp != null) {
					Message.send(opp.dos, Command.JOIN, c.name, playingTime);
				}
			} else if (Command.REFUSE.equals(cmd)) {
				Client opp = findByName(m.arg(0));
				if (opp != null) {
					Message.send(opp.dos, Command.REFUSE, c.name);
				}
			} else if (Command.AGREE.equals(cmd)) {
				Client opp = findByName(m.arg(0));
				if (opp == null) {
					return true;
				}
				c.state = "playing";
				opp.state = "playing";
				c.opponent = opp;
				opp.opponent = c;
				broadcast(Command.CHANGE, c.name, "playing");
				broadcast(Command.CHANGE, opp.name, "playing");

				if (Math.random() < 0.5) {
					assignColor(c, "black", opp);
					assignColor(opp, "white", c);
				} else {
					assignColor(c, "white", opp);
					assignColor(opp, "black", c);
				}
				taMessage.append(c.name + "  playing\n");
				taMessage.append(opp.name + "  playing\n");
			} else if (Command.GO.equals(cmd)) {
				Client opp = c.opponent;
				if (opp == null) {
					return true;
				}
				int x = Integer.parseInt(m.arg(0));
				int y = Integer.parseInt(m.arg(1));
				Message.send(opp.dos, Command.GO, m.arg(0), m.arg(1));
				taMessage.append(c.name + " go " + x + "," + y + "\n");
				recordStep(c, x, y);
				recordStep(opp, x, y);
			} else if (Command.WIN.equals(cmd)) {
				Client opp = c.opponent;
				if (opp == null) {
					return true;
				}
				broadcast(Command.CHANGE, c.name, "ready");
				broadcast(Command.CHANGE, opp.name, "ready");
				Message.send(c.dos, Command.TELLRESULT, "win");
				Message.send(opp.dos, Command.TELLRESULT, "losses");
				c.state = "ready";
				opp.state = "ready";
				taMessage.append(c.name + "  win\n");
				taMessage.append(opp.name + "  losses\n");
				Date date = new Date(new java.util.Date().getTime());
				recordGame(date, c.name);
				recordManual(date, c.name);
				resetMatchState(c, opp);
			} else if (Command.GIVEUP.equals(cmd)) {
				Client opp = c.opponent;
				if (opp == null) {
					return true;
				}
				broadcast(Command.CHANGE, c.name, "ready");
				broadcast(Command.CHANGE, opp.name, "ready");
				Message.send(c.dos, Command.TELLRESULT, "losses");
				Message.send(opp.dos, Command.TELLRESULT, "win");
				c.state = "ready";
				opp.state = "ready";
				taMessage.append(c.name + "  loss\n");
				taMessage.append(opp.name + "  win\n");
				Date date = new Date(new java.util.Date().getTime());
				recordGame(date, opp.name);
				recordManual(date, opp.name);
				resetMatchState(c, opp);
			} else if (Command.QUIT.equals(cmd)) {
				Client opp = c.opponent;
				if (opp != null && "playing".equals(c.state)) {
					try {
						Message.send(opp.dos, Command.TELLRESULT, "win");
					} catch (IOException ignore) {
					}
					opp.state = "ready";
					opp.opponent = null;
					broadcast(Command.CHANGE, opp.name, "ready");
				}
				broadcastExcept(c, Command.DELETE, c.name);
				clients.remove(c);
				closeQuietly(c.s);
				taMessage.append(c.name + "  quit\n");
				clientNum--;
				lStatus.setText("连接数:" + clientNum);
				return false;
			} else if (Command.TALK.equals(cmd)) {
				broadcast(Command.TALK, c.name, m.argOr(0, ""));
				taMessage.append(c.name + " talk\n");
			} else if (Command.GAME.equals(cmd)) {
				ArrayList<Game> games = new GameDao().getGame(m.arg(0));
				Message.send(c.dos, Command.GAME);
				ObjectOutputStream oos = new ObjectOutputStream(c.s.getOutputStream());
				oos.writeObject(games);
				oos.flush();
			} else if (Command.MANUAL.equals(cmd)) {
				Manual manual = new ManualDao().getManual(m.arg(0));
				if (manual != null) {
					Message.send(c.dos, Command.MANUAL);
					ObjectOutputStream oos = new ObjectOutputStream(c.s.getOutputStream());
					oos.writeObject(manual);
					oos.flush();
				}
			}
			return true;
		}

		private void assignColor(Client target, String color, Client opp) throws IOException {
			Message.send(target.dos, Command.GUESSCOLOR, color, opp.name);
			target.chessColor = color;
		}

		private void recordStep(Client x, int col, int row) {
			if (x.step >= x.coordinates.length) {
				return;
			}
			x.coordinates[x.step][0] = col;
			x.coordinates[x.step][1] = row;
			x.step++;
		}

		private void cleanupOnDrop() {
			Client opp = c.opponent;
			if (opp != null && "playing".equals(c.state)) {
				try {
					Message.send(opp.dos, Command.TELLRESULT, "win");
				} catch (IOException ignored) {
				}
				opp.state = "ready";
				opp.opponent = null;
				try {
					broadcast(Command.CHANGE, opp.name, "ready");
				} catch (IOException ignored) {
				}
			}
			try {
				broadcastExcept(c, Command.DELETE, c.name);
			} catch (IOException ignored) {
			}
			if (clients.remove(c)) {
				clientNum--;
				lStatus.setText("连接数:" + clientNum);
			}
			closeQuietly(c.s);
		}

		void recordGame(Date date, String winner) {
			String blackUser;
			String whiteUser;
			if ("black".equals(c.chessColor)) {
				blackUser = c.name;
				whiteUser = c.opponent.name;
			} else {
				blackUser = c.opponent.name;
				whiteUser = c.name;
			}
			new GameDao().addGame(new Game(blackUser, whiteUser, date, winner));
		}

		void recordManual(Date date, String winner) {
			String blackUser;
			String whiteUser;
			if ("black".equals(c.chessColor)) {
				blackUser = c.name;
				whiteUser = c.opponent.name;
			} else {
				blackUser = c.opponent.name;
				whiteUser = c.name;
			}
			new ManualDao().addManual(new Manual(blackUser, whiteUser, date, winner, c.step, c.coordinates));
		}

		private void resetMatchState(Client a, Client b) {
			a.opponent = null;
			b.opponent = null;
			a.step = 0;
			b.step = 0;
			a.coordinates = new int[19 * 19][2];
			b.coordinates = new int[19 * 19][2];
		}
	}

	private Client findByName(String name) {
		for (Client x : clients) {
			if (x.name.equals(name)) {
				return x;
			}
		}
		return null;
	}

	private void broadcast(String cmd, String... args) throws IOException {
		for (Client x : clients) {
			try {
				Message.send(x.dos, cmd, args);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void broadcastExcept(Client skip, String cmd, String... args) throws IOException {
		for (Client x : clients) {
			if (x == skip) {
				continue;
			}
			try {
				Message.send(x.dos, cmd, args);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void closeQuietly(Socket s) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException ignored) {
			}
		}
	}

	private static boolean validUserName(String name) {
		if (name == null || name.isEmpty() || name.length() > 32) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (Character.isISOControl(ch) || ch == ':' || ch == ' ') {
				return false;
			}
		}
		return true;
	}

	public void startServser() {
		try {
			ss = Net.newServerSocket(TCP_PORT);
			while (true) {
				Socket s = ss.accept();
				new Thread(new HandshakeRunner(s)).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class HandshakeRunner implements Runnable {
		private final Socket s;

		HandshakeRunner(Socket s) {
			this.s = s;
		}

		public void run() {
			DataInputStream dis = null;
			DataOutputStream dos = null;
			try {
				dis = new DataInputStream(s.getInputStream());
				dos = new DataOutputStream(s.getOutputStream());
				Message m = Message.read(dis);
				if (Command.LOGIN.equals(m.command())) {
					handleLogin(m, dis, dos);
				} else if (Command.REGISTER.equals(m.command())) {
					handleRegister(dos);
				} else {
					closeQuietly(s);
				}
			} catch (IOException e) {
				e.printStackTrace();
				closeQuietly(s);
			}
		}

		private void handleLogin(Message m, DataInputStream dis, DataOutputStream dos) throws IOException {
			String userName = m.argOr(0, "");
			String passWord = m.argOr(1, "");
			if (!validUserName(userName)) {
				Message.send(dos, Command.LOGIN, "false");
				closeQuietly(s);
				return;
			}
			for (Client existing : clients) {
				if (existing.name.equalsIgnoreCase(userName)) {
					Message.send(dos, Command.LOGIN, "hasLogin");
					closeQuietly(s);
					return;
				}
			}
			UserDao ud = new UserDao();
			if (ud.getUser(userName, passWord) == null) {
				Message.send(dos, Command.LOGIN, "false");
				closeQuietly(s);
				return;
			}
			Message.send(dos, Command.LOGIN, "true");
			clientNum++;
			Client c = new Client(userName, s, dis, dos);
			clients.add(c);
			lStatus.setText("连接数:" + clientNum);
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			taMessage.append(userName + "  login  " + s.getInetAddress().getHostAddress() + "  <"
					+ df.format(new java.util.Date()) + ">\n");
			Message.send(dos, Command.TELLNAME, c.name);
			for (Client other : clients) {
				if (other != c) {
					Message.send(c.dos, Command.ADD, other.name, other.state);
					Message.send(other.dos, Command.ADD, c.name, "ready");
				}
			}
			new ClientThread(c).start();
		}

		private void handleRegister(DataOutputStream dos) throws IOException {
			ObjectInputStream ois = null;
			try {
				ois = new SafeObjectInputStream(s.getInputStream(), "user.User");
				User u = (User) ois.readObject();
				if (!validUserName(u.getUserName())) {
					Message.send(dos, Command.REGISTER, "false");
					return;
				}
				UserDao ud = new UserDao();
				Message.send(dos, Command.REGISTER, ud.addUser(u) ? "true" : "false");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				Message.send(dos, Command.REGISTER, "false");
			} finally {
				closeQuietly(s);
			}
		}
	}
}
