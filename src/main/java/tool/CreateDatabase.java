// 数据库初始化：创建 fiveChess 库 + user / game 两张表
package tool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import user.DBConnection;

public class CreateDatabase {
	public static void main(String[] args) {
		Connection conn = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			String url = DBConnection.getUrl().replaceAll("/[^/?]+(\\?|$)", "/$1");
			conn = DriverManager.getConnection(url, DBConnection.getUser(), DBConnection.getPassword());
			stmt = conn.createStatement();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (SQLException e) {
			e.printStackTrace();
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException ignored) {
				}
			}
			System.exit(-1);
		}
		createDatabase(stmt, "fiveChess");
		try {
			stmt.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void createDatabase(Statement stmt, String dbName) {
		try {
			stmt.executeUpdate("create database if not exists " + dbName);
			stmt.executeUpdate("use " + dbName);

			stmt.executeUpdate("create table if not exists user("
					+ "id int auto_increment not null primary key,"
					+ "name VARCHAR(32) not null unique,"
					+ "password VARCHAR(160) not null,"
					+ "email VARCHAR(64) not null,"
					+ "level int not null,"
					+ "regDate date not null)");

			stmt.executeUpdate("create table if not exists game("
					+ "id int auto_increment not null primary key,"
					+ "gameDate date not null,"
					+ "playerBlack VARCHAR(32) not null,"
					+ "playerWhite VARCHAR(32) not null,"
					+ "winner VARCHAR(32) not null,"
					+ "manualFileName VARCHAR(160) not null)");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
