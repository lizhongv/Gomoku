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

	public String loginStatus(String iP, String userName, String passWord) {
		try {
			s = Net.newSocket(iP, FiveServer.TCP_PORT);
			dis = new DataInputStream(s.getInputStream());
			dos = new DataOutputStream(s.getOutputStream());
			Message.send(dos, Command.LOGIN, userName, passWord);
			Message reply = Message.read(dis);
			if (Command.LOGIN.equals(reply.command()) && "true".equals(reply.argOr(0, ""))) {
				connected = true;
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
}
