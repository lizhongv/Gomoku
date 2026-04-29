// 棋谱回放对话框：上一步 / 下一步 走完整局
package client;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import game.Manual;

public class DialogPlayback extends Dialog implements ActionListener {
	Manual manual;
	Chess[] chessList;
	PanelBoard board;
	PanelTiming timing;
	Button btNext = new Button("下一步");
	Button btPre = new Button("上一步");
	Button btCancel = new Button("关闭");

	public DialogPlayback(FiveClient parent, Manual manual) {
		super(parent, "棋谱欣赏", true);
		this.manual = manual;
		chessList = new Chess[manual.getTotalStep()];
		board = new PanelBoard(null);
		timing = new PanelTiming();
		timing.setMyName(manual.getbUser());
		timing.setOpName(manual.getwUer());
		this.add(board, BorderLayout.CENTER);
		Panel east = new Panel(new BorderLayout());
		east.add(timing, BorderLayout.NORTH);
		Panel control = new Panel(new GridLayout(3, 1));
		btNext.addActionListener(this);
		btPre.addActionListener(this);
		btCancel.addActionListener(this);
		control.add(btNext);
		control.add(btPre);
		control.add(btCancel);
		east.add(control, BorderLayout.CENTER);
		this.add(east, BorderLayout.EAST);
		this.setLocation(300, 100);
		pack();
		this.setResizable(false);
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btCancel) {
			this.dispose();
		} else if (e.getSource() == btNext) {
			int step = board.getChessCount();
			if (step >= manual.getTotalStep()) {
				return;
			}
			Color color = step % 2 == 0 ? Color.BLACK : Color.WHITE;
			int col = manual.getChessList()[step][0];
			int row = manual.getChessList()[step][1];
			board.chessList[step] = new Chess(board, col, row, color);
			board.setChessCount(step + 1);
			board.repaint();
		} else if (e.getSource() == btPre) {
			int step = board.getChessCount();
			step--;
			if (step < 0) {
				step = 0;
				return;
			}
			board.setChessCount(step);
			board.chessList[step] = null;
			board.repaint();
		}
	}
}
