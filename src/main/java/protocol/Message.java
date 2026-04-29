// 结构化消息：writeUTF(cmd) + writeInt(N) + writeUTF*N
// 替代以前 ":"-分隔字符串的脆弱协议，消除字段中含特殊字符导致的解析错位
package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Message {
	private static final int MAX_ARGS = 1024;

	private final String command;
	private final List<String> args;

	public Message(String command, List<String> args) {
		this.command = command;
		this.args = Collections.unmodifiableList(args);
	}

	public String command() {
		return command;
	}

	public int argCount() {
		return args.size();
	}

	public String arg(int i) {
		return args.get(i);
	}

	public String argOr(int i, String fallback) {
		return i < args.size() ? args.get(i) : fallback;
	}

	public static Message read(DataInputStream dis) throws IOException {
		String cmd = dis.readUTF();
		int n = dis.readInt();
		if (n < 0 || n > MAX_ARGS) {
			throw new IOException("Invalid arg count: " + n);
		}
		ArrayList<String> a = new ArrayList<String>(n);
		for (int i = 0; i < n; i++) {
			a.add(dis.readUTF());
		}
		return new Message(cmd, a);
	}

	public static void send(DataOutputStream dos, String cmd, String... args) throws IOException {
		synchronized (dos) {
			dos.writeUTF(cmd);
			dos.writeInt(args.length);
			for (String a : args) {
				dos.writeUTF(a == null ? "" : a);
			}
			dos.flush();
		}
	}
}
