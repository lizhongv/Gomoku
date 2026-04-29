// 用户列表面板：当前在线用户 + 状态
package client;

import java.awt.BorderLayout;
import java.awt.List;
import java.awt.Panel;

public class PanelUserList extends Panel {
	public List userList = new List(8);

	public PanelUserList() {
		setLayout(new BorderLayout());
		add(userList, BorderLayout.CENTER);
	}
}
