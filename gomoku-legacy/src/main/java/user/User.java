// 用户实体
package user;

import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {
	private static final long serialVersionUID = -8418444131403945274L;
	private String userName;
	private String passWord;
	private String email;
	private int level;
	private Date regDate;

	public User(String userName, String passWord, String email, int level, Date regDate) {
		this.userName = userName;
		this.passWord = passWord;
		this.email = email;
		this.level = level;
		this.regDate = regDate;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassWord() {
		return passWord;
	}

	public void setPassWord(String passWord) {
		this.passWord = passWord;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public Date getRegDate() {
		return regDate;
	}

	public void setRegDate(Date regDate) {
		this.regDate = regDate;
	}

	public String toString() {
		return userName + ":" + passWord + ":" + email + ":" + level + ":" + regDate;
	}
}
