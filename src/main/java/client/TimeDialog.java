// 倒计时邀请对话框：显示邀请信息，N 秒后自动关闭
package client;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class TimeDialog {
	private String message = null;
	private int seconds = 0;
	private JLabel label = new JLabel();
	private JButton confirm;
	private JButton cancel;
	private JDialog dialog = null;
	int result = -5;

	public int showDialog(Frame father, String message, int sec) {
		this.message = message;
		seconds = sec;
		label.setText(message);
		label.setBounds(80, 6, 200, 20);
		ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
		confirm = new JButton("接受");
		confirm.setBounds(100, 40, 60, 20);
		confirm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = 0;
				TimeDialog.this.dialog.dispose();
			}
		});
		cancel = new JButton("拒绝");
		cancel.setBounds(190, 40, 60, 20);
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = 1;
				TimeDialog.this.dialog.dispose();
			}
		});
		dialog = new JDialog(father, true);
		dialog.setTitle("提示：本窗口将在" + seconds + "秒后自动关闭");
		dialog.setLayout(null);
		dialog.add(label);
		dialog.add(confirm);
		dialog.add(cancel);
		s.scheduleAtFixedRate(new Runnable() {
			public void run() {
				TimeDialog.this.seconds--;
				if (TimeDialog.this.seconds == 0) {
					TimeDialog.this.dialog.dispose();
				} else {
					dialog.setTitle("本窗口将在" + seconds + "秒后关闭");
				}
			}
		}, 1, 1, TimeUnit.SECONDS);
		dialog.pack();
		dialog.setSize(new Dimension(350, 100));
		dialog.setLocationRelativeTo(father);
		dialog.setVisible(true);
		return result;
	}
}
