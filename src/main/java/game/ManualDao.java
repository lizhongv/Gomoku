// 棋谱文件访问：序列化到 manuals/<fileName>
package game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ManualDao {
	private static final String DIR = "manuals";

	public boolean addManual(Manual manual) {
		File dir = new File(DIR);
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
			fis = new FileInputStream(new File(DIR, fileName));
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
