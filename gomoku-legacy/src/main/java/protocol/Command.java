// 客户端 / 服务器之间使用的命令字常量
package protocol;

public class Command {
	public static final String TELLNAME = "tellName";
	public static final String ADD = "add";
	public static final String JOIN = "join";
	public static final String REFUSE = "refuse";
	public static final String AGREE = "agree";
	public static final String CHANGE = "change";
	public static final String GUESSCOLOR = "guessColor";
	public static final String GO = "go";
	public static final String WIN = "win";
	public static final String TELLRESULT = "tellResult";
	public static final String GIVEUP = "giveup";
	public static final String QUIT = "quit";
	public static final String DELETE = "delete";

	public static final String REGISTER = "register";
	public static final String LOGIN = "login";
	public static final String GAME = "game";
	public static final String MANUAL = "manual";
	public static final String UPLOAD_MANUAL = "uploadManual";

	public static final String TALK = "talk";

	private Command() {
	}
}
