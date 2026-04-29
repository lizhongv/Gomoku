// 信息面板：显示来自服务器和聊天的文本
package client;

import java.awt.BorderLayout;
import java.awt.Panel;
import java.awt.TextArea;

public class PanelMessage extends Panel {
	public TextArea messageArea;

	public PanelMessage() {
		setLayout(new BorderLayout());
		messageArea = new TextArea("", 12, 20, TextArea.SCROLLBARS_VERTICAL_ONLY);
		add(messageArea, BorderLayout.CENTER);
	}
}
