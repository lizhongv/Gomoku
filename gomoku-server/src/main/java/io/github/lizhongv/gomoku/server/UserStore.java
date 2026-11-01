package io.github.lizhongv.gomoku.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lizhongv.gomoku.core.GameResult;
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

  /** 账号。rating 缺失（旧数据）时按 1200 处理。 */
  record User(
      String name, String display, String salt, String hash, int wins, int losses, int rating) {
    User {
      if (rating <= 0) {
        rating = 1200;
      }
    }
  }

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
            0,
            1200);
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

  /** 按 ELO 记录一局结果，返回双方的新积分。 */
  synchronized Map<String, Integer> recordGame(
      String blackName, String whiteName, GameResult result) {
    User black = users.get(normalize(blackName));
    User white = users.get(normalize(whiteName));
    if (black == null || white == null) {
      return Map.of();
    }
    double scoreBlack =
        result == GameResult.BLACK_WIN ? 1.0 : (result == GameResult.DRAW ? 0.5 : 0.0);
    double expectedBlack = 1.0 / (1.0 + Math.pow(10.0, (white.rating() - black.rating()) / 400.0));
    int delta = (int) Math.round(32 * (scoreBlack - expectedBlack));
    int blackRating = Math.max(100, black.rating() + delta);
    int whiteRating = Math.max(100, white.rating() - delta);
    users.put(
        black.name(),
        new User(
            black.name(),
            black.display(),
            black.salt(),
            black.hash(),
            black.wins() + (result == GameResult.BLACK_WIN ? 1 : 0),
            black.losses() + (result == GameResult.WHITE_WIN ? 1 : 0),
            blackRating));
    users.put(
        white.name(),
        new User(
            white.name(),
            white.display(),
            white.salt(),
            white.hash(),
            white.wins() + (result == GameResult.WHITE_WIN ? 1 : 0),
            white.losses() + (result == GameResult.BLACK_WIN ? 1 : 0),
            whiteRating));
    persist();
    return Map.of(black.name(), blackRating, white.name(), whiteRating);
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
