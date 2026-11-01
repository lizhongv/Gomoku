package io.github.lizhongv.gomoku.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 账号存储：PBKDF2-SHA256 加盐哈希，JSON 文件持久化。
 *
 * <p>密码不落明文、不写日志；文件写入先写临时文件再替换，避免写到一半损坏。
 */
final class UserStore {

  private static final Logger LOG = LoggerFactory.getLogger(UserStore.class);

  private static final int ITERATIONS = 120_000;

  private static final int KEY_BITS = 256;

  private static final SecureRandom RANDOM = new SecureRandom();

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** 账号。 */
  record User(String name, String display, String salt, String hash, int wins, int losses) {}

  private final Path file;

  private final Map<String, User> users = new ConcurrentHashMap<>();

  UserStore(Path dataDir) {
    this.file = dataDir.resolve("users.json");
    load();
  }

  /** 注册；用户名或密码不合法、重名时抛 IllegalArgumentException。 */
  synchronized User register(String name, String password, String display) {
    String key = normalize(name);
    if (!key.matches("[a-z0-9_]{3,20}")) {
      throw new IllegalArgumentException("账号须为 3～20 位字母、数字或下划线");
    }
    if (password == null || password.length() < 8) {
      throw new IllegalArgumentException("密码至少 8 位");
    }
    if (users.containsKey(key)) {
      throw new IllegalArgumentException("账号已存在");
    }
    byte[] salt = new byte[16];
    RANDOM.nextBytes(salt);
    User user =
        new User(
            key,
            display == null || display.isBlank() ? name.trim() : display.trim(),
            Base64.getEncoder().encodeToString(salt),
            hash(password, salt),
            0,
            0);
    users.put(key, user);
    persist();
    return user;
  }

  /** 校验密码；失败返回 null。 */
  User authenticate(String name, String password) {
    User user = users.get(normalize(name));
    if (user == null || password == null) {
      return null;
    }
    byte[] expected = Base64.getDecoder().decode(user.hash());
    byte[] actual =
        Base64.getDecoder().decode(hash(password, Base64.getDecoder().decode(user.salt())));
    return MessageDigest.isEqual(expected, actual) ? user : null;
  }

  /** 查找账号。 */
  User find(String name) {
    return users.get(normalize(name));
  }

  /** 记录胜负。 */
  synchronized void recordResult(String name, boolean win) {
    User user = users.get(normalize(name));
    if (user == null) {
      return;
    }
    users.put(
        user.name(),
        new User(
            user.name(),
            user.display(),
            user.salt(),
            user.hash(),
            user.wins() + (win ? 1 : 0),
            user.losses() + (win ? 0 : 1)));
    persist();
  }

  private static String hash(String password, byte[] salt) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
      byte[] key =
          SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
      spec.clearPassword();
      return Base64.getEncoder().encodeToString(key);
    } catch (Exception error) {
      throw new IllegalStateException("密码哈希失败", error);
    }
  }

  private static String normalize(String name) {
    return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
  }

  private void load() {
    if (!Files.exists(file)) {
      return;
    }
    try {
      List<User> list =
          MAPPER.readValue(
              Files.readString(file),
              MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, User.class));
      list.forEach(user -> users.put(user.name(), user));
    } catch (IOException error) {
      LOG.warn("账号文件读取失败，按空库启动：{}", error.getMessage());
    }
  }

  private void persist() {
    try {
      Files.createDirectories(file.getParent());
      String text =
          MAPPER
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(new ArrayList<>(users.values()));
      Path temp = file.resolveSibling(file.getFileName() + ".tmp");
      Files.writeString(temp, text, StandardCharsets.UTF_8);
      Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException error) {
      LOG.error("账号文件写入失败", error);
    }
  }
}
