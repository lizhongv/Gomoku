// 客户端主入口：组装窗口 / 装配监听器
// 注意：此类是阶段一前的旧 UI 入口，Phase 5.7 删除；保留是为过渡期可编译。
package client;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import game.Game;
import game.Manual;
import net.Communication;
import net.NoopGameClient;

public class FiveClient extends Frame {
	PanelBoard board;
	PanelTiming timing;
	PanelUserList userList;
	PanelMessage message;
	PanelControl control;

	String myname;
	String opname;
	public boolean isConnected = false;
	Communication c = null;
	int playingTime;

	public static void main(String[] args) {
		new FiveClient();
	}

	public FiveClient() {
		super("五子棋客户端");
		board = new PanelBoard(this);
		timing = new PanelTiming();
		userList = new PanelUserList();
		message = new PanelMessage();
		control = new PanelControl();

		Panel east = new Panel();
		east.setLayout(new BorderLayout());
		east.add(userList, BorderLayout.CENTER);
		east.add(message, BorderLayout.SOUTH);
		east.add(timing, BorderLayout.NORTH);

		this.add(east, BorderLayout.EAST);
		this.add(control, BorderLayout.SOUTH);
		this.add(board, BorderLayout.CENTER);

		this.pack();
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setVisible(true);

		ActionMonitor monitor = new ActionMonitor();
		control.exitGameButton.addActionListener(monitor);
		control.loginButton.addActionListener(monitor);
		control.joinGameButton.addActionListener(monitor);
		control.chessManualButton.addActionListener(monitor);
		control.cancelGameButton.addActionListener(monitor);
		control.talkButton.addActionListener(monitor);
		control.aiButton.addActionListener(monitor);

		control.loginButton.setEnabled(true);
		control.joinGameButton.setEnabled(false);
		control.cancelGameButton.setEnabled(false);
		control.chessManualButton.setEnabled(false);
		control.exitGameButton.setEnabled(true);
		control.talkButton.setEnabled(false);
	}

	class ActionMonitor implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == control.exitGameButton) {
				if (isConnected) {
					c.disConnect();
				}
				System.exit(0);
			} else if (e.getSource() == control.loginButton) {
				c = new Communication(new NoopGameClient());
				new DialogLogin(FiveClient.this, FiveClient.this.control.inputIP.getText());
			} else if (e.getSource() == control.joinGameButton) {
				String select = userList.userList.getSelectedItem();
				if (select == null) {
					message.messageArea.append("请选择一个对手\n");
					return;
				}
				if (!select.endsWith("ready")) {
					message.messageArea.append("请选择ready状态的对手\n");
					return;
				}
				int index = select.indexOf(":");
				if (index < 0) {
					return;
				}
				String name = select.substring(0, index).trim();
				if (name.equals(myname)) {
					message.messageArea.append("不能选自己作为对手\n");
					return;
				}
				join(name);
			} else if (e.getSource() == control.cancelGameButton) {
				c.giveup();
			} else if (e.getSource() == control.chessManualButton) {
				c.getGames(myname);
			} else if (e.getSource() == control.talkButton) {
				String text = JOptionPane.showInputDialog("请输入您要发送的消息："); if (text != null) c.talk(text);
			} else if (e.getSource() == control.aiButton) {
				new ai.AiGame();
			}
		}
	}

	public void join(String opponentName) {
		String strTime = JOptionPane.showInputDialog("请输入下棋总用时(秒)：");
		playingTime = Integer.parseInt(strTime);
		c.join(opponentName, strTime);
	}

	public Communication getC() {
		return c;
	}

	public void showGames(ArrayList<Game> games) {
		new DialogGames(this, games);
	}

	public void playBack(Manual manual) {
		new DialogPlayback(this, manual);
	}
}
