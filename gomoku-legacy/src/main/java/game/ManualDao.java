// 棋谱文件访问：序列化到用户目录 ~/.gomoku/manuals/
// 安装到 Program Files 后程序无法写自身目录，所以放到用户家目录
package game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ManualDao {

	public static File getDir() {
		// 优先级：环境变量 FIVE_MANUAL_DIR > ~/.gomoku/manuals > 工作目录 manuals
		String env = System.getenv("FIVE_MANUAL_DIR");
		if (env != null && !env.isEmpty()) {
			return new File(env);
		}
		String home = System.getProperty("user.home");
		if (home != null && !home.isEmpty()) {
			return new File(home, ".gomoku/manuals");
		}
		return new File("manuals");
	}

	public boolean addManual(Manual manual) {
		File dir = getDir();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream(new File(dir, manual.getFileName()));
			oos = new ObjectOutputStream(fos);
			oos.writeObject(manual);
			oos.close();
			fos.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return false;
		}
	}

	public Manual getManual(String fileName) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(new File(getDir(), fileName));
			ois = new ObjectInputStream(fis);
			Manual manual = (Manual) ois.readObject();
			ois.close();
			fis.close();
			return manual;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
}
