package br.com.kodeless.minimaljs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

public class XLabels {

	static Map<String, Properties> propertyMap = new HashMap<String, Properties>();
	private static ServletContext context;

	public static void start(ServletContext ctx) {
		context = ctx;
	}

	public static String getLabel(String label) throws XLabelException {
		String[] s = label.split("#");
		if (s.length != 2) {
			throw new XLabelException("Invalid label: " + label);
		}
		Properties p = getProperties(s[0]);
		return p.getProperty(s[1]);
	}

	private static Properties getProperties(String file) throws XLabelException {
		synchronized (propertyMap) {
			Properties p = propertyMap.get(file);
			if (p == null) {
				p = new Properties();
				try {
					p.load(context.getResourceAsStream("/labels/" + file));
				} catch (IOException e) {
					throw new XLabelException("Error reading file " + file, e);
				}
				propertyMap.put(file, p);
			}
			return p;
		}
	}
}
