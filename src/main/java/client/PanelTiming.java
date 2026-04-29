// 计时面板：显示双方头像 / 名字 / 剩余时间
package client;

import java.awt.GridLayout;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class PanelTiming extends JPanel {
	JLabel myIcon;
	JLabel opIcon;
	JLabel myName;
	JLabel opName;
	JLabel myTimer;
	JLabel opTimer;
	Icon blackIcon;
	Icon whiteIcon;

	public PanelTiming() {
		blackIcon = new ImageIcon("assets/black.jpg");
		whiteIcon = new ImageIcon("assets/white.jpg");

		this.myIcon = new JLabel(blackIcon, SwingConstants.CENTER);
		this.opIcon = new JLabel(whiteIcon, SwingConstants.CENTER);
		this.myName = new JLabel("My Name", SwingConstants.CENTER);
		this.opName = new JLabel("Op Name", SwingConstants.CENTER);
		this.myTimer = new JLabel("00:00:00");
		this.opTimer = new JLabel("00:00:00");

		JPanel myText = new JPanel(new GridLayout(2, 1));
		myText.add(myName);
		myText.add(myTimer);
		JPanel opText = new JPanel(new GridLayout(2, 1));
		opText.add(opName);
		opText.add(opTimer);
		this.add(myIcon);
		this.add(myText);
		this.add(opIcon);
		this.add(opText);
	}

	public void setMyIcon(String color) {
		this.myIcon.setIcon("black".equals(color) ? blackIcon : whiteIcon);
	}

	public void setOpIcon(String color) {
		this.opIcon.setIcon("black".equals(color) ? blackIcon : whiteIcon);
	}

	public void setMyName(String name) {
		this.myName.setText(name);
	}

	public void setOpName(String name) {
		this.opName.setText(name);
	}

	public void setMyTime(int time) {
		int h = time / 3600;
		int m = (time - h * 3600) / 60;
		int s = time % 60;
		this.myTimer.setText(h + ":" + m + ":" + s);
	}

	public void setOpTime(int time) {
		int h = time / 3600;
		int m = (time - h * 3600) / 60;
		int s = time % 60;
		this.opTimer.setText(h + ":" + m + ":" + s);
	}
}
