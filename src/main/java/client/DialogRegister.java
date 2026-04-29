// 注册对话框
package client;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import protocol.Command;
import protocol.Message;
import protocol.Net;
import server.FiveServer;
import user.User;

public class DialogRegister extends JDialog implements ActionListener {
	JTextField tfUserName = new JTextField(20);
	JTextField tfPassWord = new JTextField(20);
	JTextField tfRePassWord = new JTextField(20);
	JTextField tfEmail = new JTextField(20);
	JButton jbRegister = new JButton("注册");
	JButton jbCancel = new JButton("取消");
	String ip;

	public DialogRegister(Window parent, String ip) {
		super(parent, "用户注册", Dialog.ModalityType.APPLICATION_MODAL);
		this.ip = ip;
		createGUI();
	}

	public void actionPerformed(ActionEvent e) {
		String str = e.getActionCommand();
		if ("注册".equals(str)) {
			String userName = tfUserName.getText();
			String passWord = tfPassWord.getText();
			String rePassWord = tfRePassWord.getText();
			String email = tfEmail.getText();
			if (userName == null || userName.isEmpty() || passWord == null || passWord.isEmpty()
					|| rePassWord == null || rePassWord.isEmpty() || email == null || email.isEmpty()) {
				JOptionPane.showMessageDialog(this, "各项数据不能为空！");
				return;
			}
			if (!isValidUserName(userName)) {
				JOptionPane.showMessageDialog(this, "用户名不能包含空格、冒号、控制字符，长度 1-32");
				return;
			}
			if (!passWord.equals(rePassWord)) {
				JOptionPane.showMessageDialog(this, "两次密码不一致！");
				return;
			}
			if (register(new User(userName, passWord, email, 1, new java.sql.Date(new java.util.Date().getTime())))) {
				JOptionPane.showMessageDialog(this, "注册成功！");
				this.dispose();
			} else {
				JOptionPane.showMessageDialog(this, "注册失败！可能重名。");
			}
		} else if ("取消".equals(str)) {
			this.dispose();
		}
	}

	private static boolean isValidUserName(String name) {
		if (name.length() > 32) {
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

	public boolean register(User u) {
		Socket s = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;
		ObjectOutputStream oos = null;
		try {
			s = Net.newSocket(ip, FiveServer.TCP_PORT);
			dis = new DataInputStream(s.getInputStream());
			dos = new DataOutputStream(s.getOutputStream());
			Message.send(dos, Command.REGISTER);
			oos = new ObjectOutputStream(s.getOutputStream());
			oos.writeObject(u);
			oos.flush();
			Message reply = Message.read(dis);
			return Command.REGISTER.equals(reply.command()) && "true".equals(reply.argOr(0, ""));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			closeQuietly(oos);
			closeQuietly(dos);
			closeQuietly(dis);
			closeSocket(s);
		}
	}

	private static void closeQuietly(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException ignored) {
			}
		}
	}

	private static void closeSocket(Socket s) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException ignored) {
			}
		}
	}

	public void createGUI() {
		this.setLayout(new BorderLayout());
		JPanel jpWest = new JPanel();
		JPanel jpCenter = new JPanel();
		JPanel jpSouth = new JPanel();
		jpWest.setLayout(new GridLayout(4, 1));
		jpCenter.setLayout(new GridLayout(4, 1));
		jpSouth.setLayout(new FlowLayout());
		jpWest.add(new JLabel("用 户 名："));
		jpWest.add(new JLabel("密    码："));
		jpWest.add(new JLabel("确认密码："));
		jpWest.add(new JLabel("邮    箱："));
		jpCenter.add(tfUserName);
		jpCenter.add(tfPassWord);
		jpCenter.add(tfRePassWord);
		jpCenter.add(tfEmail);
		this.add(jpWest, BorderLayout.WEST);
		this.add(jpCenter, BorderLayout.CENTER);
		jpSouth.add(jbRegister);
		jpSouth.add(jbCancel);
		jbRegister.addActionListener(this);
		jbCancel.addActionListener(this);
		this.add(jpSouth, BorderLayout.SOUTH);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setLocation(450, 250);
		this.pack();
		this.setVisible(true);
	}
}
