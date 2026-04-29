// 棋局数据访问
package game;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import user.DBConnection;

public class GameDao {

	public boolean addGame(Game game) {
		String sql = "insert into game(gameDate,playerBlack,playerWhite,winner,manualFileName) values(?,?,?,?,?)";
		try (Connection conn = new DBConnection().getConn();
				PreparedStatement pst = conn.prepareStatement(sql)) {
			pst.setDate(1, new java.sql.Date(game.getDate().getTime()));
			pst.setString(2, game.getbUser());
			pst.setString(3, game.getwUer());
			pst.setString(4, game.getWinner());
			pst.setString(5, game.getFileName());
			pst.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public ArrayList<Game> getGame(String userName) {
		ArrayList<Game> games = new ArrayList<Game>();
		String sql = "select playerBlack, playerWhite, gameDate, winner, manualFileName "
				+ "from game where playerBlack = ? or playerWhite = ?";
		try (Connection conn = new DBConnection().getConn();
				PreparedStatement pst = conn.prepareStatement(sql)) {
			pst.setString(1, userName);
			pst.setString(2, userName);
			try (ResultSet rs = pst.executeQuery()) {
				while (rs.next()) {
					String bUser = rs.getString("playerBlack");
					String wUser = rs.getString("playerWhite");
					java.sql.Date date = new java.sql.Date(rs.getDate("gameDate").getTime());
					String winner = rs.getString("winner");
					String fileName = rs.getString("manualFileName");
					games.add(new Game(bUser, wUser, date, winner, fileName));
				}
				return games;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
}
