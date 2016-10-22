package br.com.codeminimal.minimojs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class XStreamUtil {
	public static String inputStreamToString(InputStream is) throws IOException {
		return new String(inputStreamToByteArray(is));
	}

	public static byte[] inputStreamToByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int i;
		while ((i = is.read()) != -1) {
			baos.write(i);
		}
		is.close();
		return baos.toByteArray();
	}

	public static String loadResource(String path) throws IOException {
		return inputStreamToString(XStreamUtil.class.getResourceAsStream(path));
	}
}
