// 登录对话框
package client;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class DialogLogin extends JDialog implements ActionListener {
	JTextField tfUserName = new JTextField(14);
	JTextField tfPassWord = new JTextField(14);
	JButton jbLogin = new JButton("登录");
	JButton jbRegister = new JButton("注册");
	JButton jbCancel = new JButton("取消");
	String ip;
	FiveClient fc;

	public DialogLogin(Window parent, String ip) {
		super(parent, "用户登录", Dialog.ModalityType.APPLICATION_MODAL);
		fc = (FiveClient) parent;
		this.ip = ip;
		createGUI();
	}

	public void actionPerformed(ActionEvent e) {
		String str = e.getActionCommand();
		if ("登录".equals(str)) {
			String userName = tfUserName.getText();
			String passWord = tfPassWord.getText();
			if ((userName == null) || (userName.isEmpty()) || (passWord == null) || (passWord.isEmpty())) {
				JOptionPane.showMessageDialog(this, "各项数据不能为空！");
				return;
			}
			if (fc.getC().login(ip, userName, passWord)) {
				JOptionPane.showMessageDialog(this, "登录成功！");
				this.dispose();
			} else {
				this.dispose();
			}
		} else if ("注册".equals(str)) {
			new DialogRegister(this, ip);
		} else if ("取消".equals(str)) {
			this.dispose();
		}
	}

	public void createGUI() {
		this.setLayout(new BorderLayout());
		JPanel jpWest = new JPanel();
		JPanel jpCenter = new JPanel();
		JPanel jpSouth = new JPanel();
		jpWest.setLayout(new GridLayout(3, 1));
		jpCenter.setLayout(new GridLayout(3, 1));
		jpSouth.setLayout(new FlowLayout());
		jpWest.add(new JLabel("用 户 名:"));
		jpWest.add(new JLabel("密    码:"));
		jpCenter.add(tfUserName);
		jpCenter.add(tfPassWord);
		this.add(new JPanel(), BorderLayout.NORTH);
		this.add(jpCenter, BorderLayout.CENTER);
		this.add(jpWest, BorderLayout.WEST);
		jpSouth.add(jbLogin);
		jpSouth.add(jbRegister);
		jpSouth.add(jbCancel);
		jbLogin.addActionListener(this);
		jbRegister.addActionListener(this);
		jbCancel.addActionListener(this);
		this.add(jpSouth, BorderLayout.SOUTH);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo(fc);
		this.setVisible(true);
	}
}
