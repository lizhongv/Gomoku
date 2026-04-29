// 客户端网络层：消息收发、登录 / 注册 / 对局命令封装
package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import game.Game;
import game.Manual;
import protocol.Command;
import protocol.Message;
import protocol.Net;
import protocol.SafeObjectInputStream;
import server.FiveServer;

public class Communication {
	FiveClient fc;
	Socket s;
	TimerThread tt;
	private DataInputStream dis;
	private DataOutputStream dos;

	public Communication(FiveClient fc) {
		this.fc = fc;
	}

	class ReceiveThread extends Thread {
		public void run() {
			try {
				while (true) {
					Message m = Message.read(dis);
					dispatch(m);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void dispatch(Message m) throws IOException {
			String cmd = m.command();
			if (Command.TELLNAME.equals(cmd)) {
				fc.myname = m.arg(0);
				fc.userList.userList.add(fc.myname + ": ready");
				fc.timing.setMyName(fc.myname);
				fc.message.messageArea.append("My name: " + fc.myname + " ready\n");
			} else if (Command.ADD.equals(cmd)) {
				fc.userList.userList.add(m.arg(0) + ": " + m.arg(1));
				fc.message.messageArea.append(m.arg(0) + ": " + m.arg(1) + "\n");
			} else if (Command.JOIN.equals(cmd)) {
				String name = m.arg(0);
				String strTime = m.arg(1);
				TimeDialog d = new TimeDialog();
				fc.playingTime = Integer.parseInt(strTime);
				int select = d.showDialog(fc, name + " 邀请您下棋(用时" + strTime + "秒)是否接受 ？", 100);
				if (select == 0) {
					Message.send(dos, Command.AGREE, name);
				} else {
					Message.send(dos, Command.REFUSE, name);
				}
			} else if (Command.REFUSE.equals(cmd)) {
				JOptionPane.showMessageDialog(fc, m.arg(0) + "拒绝了您的邀请!");
			} else if (Command.CHANGE.equals(cmd)) {
				String name = m.arg(0);
				String state = m.arg(1);
				for (int i = 0; i < fc.userList.userList.getItemCount(); i++) {
					if (fc.userList.userList.getItem(i).startsWith(name + ":")) {
						fc.userList.userList.replaceItem(name + ": " + state, i);
					}
				}
				fc.message.messageArea.append(name + " " + state + "\n");
			} else if (Command.GUESSCOLOR.equals(cmd)) {
				String color = m.arg(0);
				String oppName = m.arg(1);
				fc.board.isGamming = true;
				fc.opname = oppName;
				fc.timing.setOpName(oppName);
				if (color.equals("black")) {
					fc.timing.setMyIcon("black");
					fc.timing.setOpIcon("white");
					fc.board.isBlack = true;
					fc.board.isGoing = true;
				} else {
					fc.timing.setMyIcon("white");
					fc.timing.setOpIcon("black");
					fc.board.isBlack = false;
					fc.board.isGoing = false;
				}
				fc.control.joinGameButton.setEnabled(false);
				fc.control.cancelGameButton.setEnabled(true);
				fc.control.exitGameButton.setEnabled(false);
				fc.message.messageArea.append("My color is " + color + "\n");
				tt = new TimerThread(fc, fc.playingTime);
				tt.start();
			} else if (Command.GO.equals(cmd)) {
				int col = Integer.parseInt(m.arg(0));
				int row = Integer.parseInt(m.arg(1));
				fc.board.addOpponentChess(col, row);
			} else if (Command.TELLRESULT.equals(cmd)) {
				if ("win".equals(m.arg(0))) {
					fc.board.winsGame();
				} else {
					fc.board.lossesGame();
				}
				fc.control.joinGameButton.setEnabled(true);
				fc.control.cancelGameButton.setEnabled(false);
				fc.control.exitGameButton.setEnabled(true);
			} else if (Command.DELETE.equals(cmd)) {
				String name = m.arg(0);
				for (int i = 0; i < fc.userList.userList.getItemCount(); i++) {
					if (fc.userList.userList.getItem(i).startsWith(name + ":")) {
						fc.userList.userList.remove(i);
						break;
					}
				}
				fc.message.messageArea.append(name + " disconnected\n");
			} else if (Command.GAME.equals(cmd)) {
				ObjectInputStream ois = new SafeObjectInputStream(s.getInputStream(), "game.Game");
				try {
					ArrayList<Game> games = (ArrayList<Game>) ois.readObject();
					fc.showGames(games);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else if (Command.MANUAL.equals(cmd)) {
				ObjectInputStream ois = new SafeObjectInputStream(s.getInputStream(), "game.Game", "game.Manual");
				try {
					Manual manual = (Manual) ois.readObject();
					fc.playBack(manual);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else if (Command.TALK.equals(cmd)) {
				fc.message.messageArea.append(m.arg(0) + ": " + m.argOr(1, "") + "\n");
			}
		}
	}

	public boolean login(String iP, String userName, String passWord) {
		try {
			s = Net.newSocket(iP, FiveServer.TCP_PORT);
			dis = new DataInputStream(s.getInputStream());
			dos = new DataOutputStream(s.getOutputStream());
			Message.send(dos, Command.LOGIN, userName, passWord);
			Message reply = Message.read(dis);
			if (Command.LOGIN.equals(reply.command()) && "true".equals(reply.argOr(0, ""))) {
				fc.isConnected = true;
				new ReceiveThread().start();
				fc.control.loginButton.setEnabled(false);
				fc.control.joinGameButton.setEnabled(true);
				fc.control.cancelGameButton.setEnabled(false);
				fc.control.chessManualButton.setEnabled(true);
				fc.control.exitGameButton.setEnabled(true);
				fc.control.talkButton.setEnabled(true);
				return true;
			} else if ("hasLogin".equals(reply.argOr(0, ""))) {
				closeSocket();
				JOptionPane.showMessageDialog(fc, "该用户已经登录!");
				return false;
			} else {
				closeSocket();
				JOptionPane.showMessageDialog(fc, "用户名或密码不符!");
				return false;
			}
		} catch (IOException e) {
			closeSocket();
			e.printStackTrace();
			return false;
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

	public void join(String opponentName, String time) {
		try {
			Message.send(dos, Command.JOIN, opponentName, time);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void talk() {
		String strMessage = JOptionPane.showInputDialog("请输入您要发送的消息：");
		if (strMessage == null) {
			return;
		}
		try {
			Message.send(dos, Command.TALK, strMessage);
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
