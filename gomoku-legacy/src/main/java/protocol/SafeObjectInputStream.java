// 受限反序列化流：只允许预先列入白名单的类型，阻断任意 gadget 链
package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SafeObjectInputStream extends ObjectInputStream {
	private final Set<String> allowed;

	public SafeObjectInputStream(InputStream in, String... allowedClasses) throws IOException {
		super(in);
		this.allowed = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(allowedClasses)));
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		String name = desc.getName();
		if (isAllowed(name)) {
			return super.resolveClass(desc);
		}
		throw new InvalidClassException("Unauthorized class for deserialization: " + name);
	}

	private boolean isAllowed(String name) {
		if (allowed.contains(name)) {
			return true;
		}
		if (name.startsWith("[")) {
			if (name.length() == 2 && "BCDFIJSZ".indexOf(name.charAt(1)) >= 0) {
				return true;
			}
			if (name.startsWith("[L") && name.endsWith(";")) {
				return isAllowed(name.substring(2, name.length() - 1));
			}
			if (name.startsWith("[[")) {
				return isAllowed(name.substring(1));
			}
			return false;
		}
		switch (name) {
			case "java.lang.String":
			case "java.lang.Number":
			case "java.lang.Integer":
			case "java.lang.Long":
			case "java.lang.Boolean":
			case "java.util.ArrayList":
			case "java.util.Date":
			case "java.sql.Date":
				return true;
			default:
				return false;
		}
	}
}
