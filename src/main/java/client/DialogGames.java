// 棋局列表对话框：展示用户所有对局，选中后可回放对应棋谱
package client;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import game.Game;

public class DialogGames extends Dialog implements ActionListener {
	FiveClient fc;
	ArrayList<Game> games;
	List gameList;
	Button btPlayback = new Button("回放");
	Button btCancel = new Button("关闭");

	public DialogGames(FiveClient parent, ArrayList<Game> games) {
		super(parent, "棋局列表", true);
		this.fc = parent;
		this.games = games != null ? games : new ArrayList<Game>();

		gameList = new List(Math.max(this.games.size(), 8));
		for (Game g : this.games) {
			gameList.add(g.getDate() + "  " + g.getbUser() + " (黑) vs " + g.getwUer() + " (白)  胜者: " + g.getWinner());
		}

		Panel south = new Panel(new FlowLayout(FlowLayout.CENTER));
		btPlayback.addActionListener(this);
		btCancel.addActionListener(this);
		south.add(btPlayback);
		south.add(btCancel);

		this.setLayout(new BorderLayout());
		this.add(gameList, BorderLayout.CENTER);
		this.add(south, BorderLayout.SOUTH);
		this.setSize(420, 320);
		this.setLocationRelativeTo(parent);
		this.setResizable(false);
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btCancel) {
			this.dispose();
		} else if (e.getSource() == btPlayback) {
			int idx = gameList.getSelectedIndex();
			if (idx < 0) {
				JOptionPane.showMessageDialog(this, "请选择一条棋局");
				return;
			}
			String fileName = games.get(idx).getFileName();
			fc.getC().getManual(fileName);
			this.dispose();
		}
	}
}
