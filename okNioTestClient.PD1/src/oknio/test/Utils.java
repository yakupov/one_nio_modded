package oknio.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Utils {
	public static int sizeof(Object obj) {
		if (obj == null)
			return 0;
		
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			int size = 0;
			try {
				oos.writeObject(obj);
				size = bos.toByteArray().length;
			} finally {
				oos.close();
			}
			return size;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
}
