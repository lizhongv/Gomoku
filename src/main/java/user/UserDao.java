// 用户管理类：在 user 表中添加 / 查找用户
package user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class UserDao {

	public boolean addUser(User user) {
		String userName = user.getUserName();
		String hashedPassword = PasswordHash.hash(user.getPassWord());
		String email = user.getEmail();
		int level = user.getLevel();
		Date regDate = user.getRegDate();

		String checkSql = "select id from user where name = ?";
		String insertSql = "insert into user(name,password,email,level,regDate) values(?,?,?,?,?)";
		try (Connection conn = new DBConnection().getConn();
				PreparedStatement check = conn.prepareStatement(checkSql)) {
			check.setString(1, userName);
			try (ResultSet rs = check.executeQuery()) {
				if (rs.next()) {
					return false;
				}
			}
			try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
				insert.setString(1, userName);
				insert.setString(2, hashedPassword);
				insert.setString(3, email);
				insert.setInt(4, level);
				insert.setDate(5, new java.sql.Date(regDate.getTime()));
				insert.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public User getUser(String userName, String passWord) {
		String sql = "select password, email, level, regDate from user where name = ?";
		try (Connection conn = new DBConnection().getConn();
				PreparedStatement pst = conn.prepareStatement(sql)) {
			pst.setString(1, userName);
			try (ResultSet rs = pst.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				String stored = rs.getString("password");
				if (!PasswordHash.verify(passWord, stored)) {
					return null;
				}
				String email = rs.getString("email");
				int level = rs.getInt("level");
				Date regDate = rs.getDate("regDate");
				if (!PasswordHash.isHashed(stored)) {
					upgradeLegacyPassword(conn, userName, passWord);
				}
				return new User(userName, passWord, email, level, regDate);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void upgradeLegacyPassword(Connection conn, String userName, String plain) {
		String sql = "update user set password = ? where name = ?";
		try (PreparedStatement pst = conn.prepareStatement(sql)) {
			pst.setString(1, PasswordHash.hash(plain));
			pst.setString(2, userName);
			pst.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
