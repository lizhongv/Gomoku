// 客户端网络层：消息收发、登录 / 注册 / 对局命令封装
package net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;

import game.Game;
import game.Manual;
import protocol.Command;
import protocol.Message;
import protocol.Net;
import protocol.SafeObjectInputStream;
import server.FiveServer;

public class Communication {
	private static volatile Communication current;
	/** 客户端进程内唯一的 Communication 实例（最近一次成功登录的连接）。 */
	public static Communication current() { return current; }

	private GameClient client;
	Socket s;
	private DataInputStream dis;
	private DataOutputStream dos;
	private String myName;
	private boolean connected;

	public Communication(GameClient client) {
		this.client = client;
	}

	public void setClient(GameClient client) {
		this.client = client;
	}

	public String getMyName() { return myName; }
	public boolean isConnected() { return connected; }

	class ReceiveThread extends Thread {
		public void run() {
			try {
				while (true) {
					Message m = Message.read(dis);
					dispatch(m);
				}
			} catch (IOException e) {
				if (client != null) client.onDisconnected();
			}
		}

		@SuppressWarnings("unchecked")
		private void dispatch(Message m) throws IOException {
			String cmd = m.command();
			if (Command.TELLNAME.equals(cmd)) {
				myName = m.arg(0);
				if (client != null) client.onLoggedIn(myName);
			} else if (Command.ADD.equals(cmd)) {
				if (client != null) client.onUserList(m.arg(0), m.arg(1));
			} else if (Command.JOIN.equals(cmd)) {
				String name = m.arg(0);
				String strTime = m.arg(1);
				int seconds = 0;
				try { seconds = Integer.parseInt(strTime); } catch (NumberFormatException ignored) {}
				if (client != null) client.onInvite(name, seconds);
			} else if (Command.REFUSE.equals(cmd)) {
				if (client != null) client.onInviteRefused(m.arg(0));
			} else if (Command.CHANGE.equals(cmd)) {
				if (client != null) client.onUserStateChange(m.arg(0), m.arg(1));
			} else if (Command.GUESSCOLOR.equals(cmd)) {
				String color = m.arg(0);
				String oppName = m.arg(1);
				if (client != null) client.onColor(color, oppName);
			} else if (Command.GO.equals(cmd)) {
				int col = Integer.parseInt(m.arg(0));
				int row = Integer.parseInt(m.arg(1));
				if (client != null) client.onOpponentMove(col, row);
			} else if (Command.TELLRESULT.equals(cmd)) {
				boolean youWon = "win".equals(m.arg(0));
				if (client != null) client.onResult(youWon);
			} else if (Command.DELETE.equals(cmd)) {
				if (client != null) client.onUserLeft(m.arg(0));
			} else if (Command.GAME.equals(cmd)) {
				ObjectInputStream ois = new SafeObjectInputStream(s.getInputStream(), "game.Game");
				try {
					ArrayList<Game> games = (ArrayList<Game>) ois.readObject();
					if (client != null) client.onGameList(games);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else if (Command.MANUAL.equals(cmd)) {
				ObjectInputStream ois = new SafeObjectInputStream(s.getInputStream(), "game.Game", "game.Manual");
				try {
					Manual manual = (Manual) ois.readObject();
					if (client != null) client.onManual(manual);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else if (Command.TALK.equals(cmd)) {
				if (client != null) client.onChat(m.arg(0), m.argOr(1, ""));
			}
		}
	}

	public boolean login(String iP, String userName, String passWord) {
		return "ok".equals(loginStatus(iP, userName, passWord));
	}

	/** 单次握手注册：新建短连接，发送 REGISTER + User 对象，读取应答。 */
	public static boolean register(String ip, String name, String pwd, String email) {
		return "ok".equals(registerStatus(ip, name, pwd, email));
	}

	/** 注册状态字符串：ok / fail / error。 */
	public static String registerStatus(String ip, String name, String pwd, String email) {
		try (Socket regSock = Net.newSocket(ip, FiveServer.TCP_PORT)) {
			DataInputStream in = new DataInputStream(regSock.getInputStream());
			DataOutputStream out = new DataOutputStream(regSock.getOutputStream());
			Message.send(out, Command.REGISTER);
			java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(regSock.getOutputStream());
			oos.writeObject(new user.User(name, pwd, email, 1, new java.sql.Date(System.currentTimeMillis())));
			oos.flush();
			Message reply = Message.read(in);
			if (Command.REGISTER.equals(reply.command()) && "true".equals(reply.argOr(0, ""))) {
				return "ok";
			}
			return "fail";
		} catch (Exception e) {
			return "error";
		}
	}

	public String loginStatus(String iP, String userName, String passWord) {
		try {
			s = Net.newSocket(iP, FiveServer.TCP_PORT);
			dis = new DataInputStream(s.getInputStream());
			dos = new DataOutputStream(s.getOutputStream());
			Message.send(dos, Command.LOGIN, userName, passWord);
			Message reply = Message.read(dis);
			if (Command.LOGIN.equals(reply.command()) && "true".equals(reply.argOr(0, ""))) {
				connected = true;
				current = this;
				new ReceiveThread().start();
				return "ok";
			} else if ("hasLogin".equals(reply.argOr(0, ""))) {
				closeSocket();
				return "hasLogin";
			} else {
				closeSocket();
				return "fail";
			}
		} catch (IOException e) {
			closeSocket();
			return "error";
		}
	}

	private void closeSocket() {
		if (s != null) {
			try {
				s.close();
			} catch (IOException ignored) {
			}
		}
		connected = false;
		if (current == this) current = null;
	}

	public void agree(String name) {
		try { Message.send(dos, Command.AGREE, name); } catch (IOException e) { e.printStackTrace(); }
	}

	public void refuse(String name) {
		try { Message.send(dos, Command.REFUSE, name); } catch (IOException e) { e.printStackTrace(); }
	}

	public void join(String opponentName, String time) {
		try {
			Message.send(dos, Command.JOIN, opponentName, time);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void talk(String text) {
		if (text == null) return;
		try {
			Message.send(dos, Command.TALK, text);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void go(int col, int row) {
		try {
			Message.send(dos, Command.GO, String.valueOf(col), String.valueOf(row));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void wins() {
		try {
			Message.send(dos, Command.WIN);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void giveup() {
		try {
			Message.send(dos, Command.GIVEUP);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void disConnect() {
		try {
			Message.send(dos, Command.QUIT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getGames(String myName) {
		try {
			Message.send(dos, Command.GAME, myName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getManual(String fileName) {
		try {
			Message.send(dos, Command.MANUAL, fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 把本地生成的 Manual（AI 或联机）上传到服务器集中存储。 */
	public void uploadManual(Manual manual) {
		if (manual == null) return;
		try {
			Message.send(dos, Command.UPLOAD_MANUAL);
			java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(s.getOutputStream());
			oos.writeObject(manual);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
