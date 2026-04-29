// 网络工厂：根据系统属性 five.tls 选择是否启用 TLS
// 启用 TLS 时按 JSSE 约定读取 -Djavax.net.ssl.keyStore / -Djavax.net.ssl.trustStore
package protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

public final class Net {
	public static final int DEFAULT_PORT = 4801;

	private Net() {
	}

	public static boolean tlsEnabled() {
		return Boolean.parseBoolean(System.getProperty("five.tls", "false"));
	}

	public static ServerSocket newServerSocket(int port) throws IOException {
		if (tlsEnabled()) {
			return SSLServerSocketFactory.getDefault().createServerSocket(port);
		}
		return new ServerSocket(port);
	}

	public static Socket newSocket(String host, int port) throws IOException {
		if (tlsEnabled()) {
			return SSLSocketFactory.getDefault().createSocket(host, port);
		}
		return new Socket(host, port);
	}
}
