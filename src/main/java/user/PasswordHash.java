// 密码哈希工具：sha256(salt || password)，存储格式 "sha256$<saltHex>$<hashHex>"
// 不会自动加载历史明文密码——由 UserDao 在登录成功时按需迁移。
package user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordHash {
	private static final String PREFIX = "sha256$";
	private static final SecureRandom RNG = new SecureRandom();

	private PasswordHash() {
	}

	public static String hash(String password) {
		byte[] salt = new byte[16];
		RNG.nextBytes(salt);
		return PREFIX + toHex(salt) + "$" + toHex(digest(salt, password));
	}

	public static boolean verify(String password, String stored) {
		if (stored == null) {
			return false;
		}
		if (!stored.startsWith(PREFIX)) {
			return constantTimeEquals(stored, password);
		}
		String[] parts = stored.split("\\$");
		if (parts.length != 3) {
			return false;
		}
		byte[] salt = fromHex(parts[1]);
		byte[] expected = fromHex(parts[2]);
		byte[] actual = digest(salt, password);
		if (expected.length != actual.length) {
			return false;
		}
		int diff = 0;
		for (int i = 0; i < expected.length; i++) {
			diff |= expected[i] ^ actual[i];
		}
		return diff == 0;
	}

	public static boolean isHashed(String stored) {
		return stored != null && stored.startsWith(PREFIX);
	}

	private static byte[] digest(byte[] salt, String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			md.update(password.getBytes(StandardCharsets.UTF_8));
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	private static String toHex(byte[] data) {
		StringBuilder sb = new StringBuilder(data.length * 2);
		for (byte b : data) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static byte[] fromHex(String hex) {
		int len = hex.length();
		byte[] out = new byte[len / 2];
		for (int i = 0; i < out.length; i++) {
			out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		}
		return out;
	}

	private static boolean constantTimeEquals(String a, String b) {
		if (a.length() != b.length()) {
			return false;
		}
		int diff = 0;
		for (int i = 0; i < a.length(); i++) {
			diff |= a.charAt(i) ^ b.charAt(i);
		}
		return diff == 0;
	}
}
