package br.com.kodeless.minimojs;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class XProperties {

	private static Map<String, Properties> properties = new HashMap<String, Properties>();

	private static Map<String, Object> propertiesCache = new HashMap<String, Object>();

	public static String getString(String key) {
		String result = (String) propertiesCache.get(key);
		if (result == null) {
			String[] skey = splitKey(key);
			result = getPropertiesByKey(skey[0]).getProperty(skey[1]);
			propertiesCache.put(key, result);
		}
		return result;
	}

	private static String[] splitKey(String key) {
		int ind = key.indexOf('.');
		String[] result = new String[2];
		result[0] = key.substring(0, ind);
		result[1] = key.substring(ind + 1);
		return result;
	}

	public static String getString(String key, String defaultValue) {
		String result = (String) propertiesCache.get(key);
		if (result == null) {
			String value = getString(key);
			result = value != null && !value.trim().equals("") ? value : defaultValue;
			propertiesCache.put(key, result);
		}
		return result;
	}

	public static String[] getStringArray(String key) {
		String[] result = (String[]) propertiesCache.get(key);
		if (result == null) {
			String[] skey = splitKey(key);
			result = getPropertiesByKey(skey[0]).getProperty(skey[1]).split(",");
			for (int i = 0; i < result.length; i++) {
				result[i] = result[i].trim();
			}
			propertiesCache.put(key, result);
		}
		return result;

	}

	public static int[] getIntArray(String key) {
		int[] result = (int[]) propertiesCache.get(key);
		if (result == null) {
			String[] strArray = getStringArray(key);
			result = new int[strArray.length];
			int i = 0;
			for (String str : strArray) {
				result[i++] = Integer.parseInt(str);
			}
			propertiesCache.put(key, result);
		}
		return result;
	}

	public static Properties getPropertiesByKey(String key) {
		Properties result = properties.get(key);
		if (result == null) {
			InputStream is = XProperties.class.getResourceAsStream("/" + key + ".properties");
			if (is == null) {
				throw new RuntimeException("Properties file /" + key + ".properties not found.");
			}
			result = new Properties();
			try {
				result.load(is);
			} catch (IOException e) {
				throw new RuntimeException("Error loading properties file /" + key + ".properties.", e);
			}
			properties.put(key, result);
		}
		return result;
	}

	public static Integer getInt(String key) {
		Integer result = (Integer) propertiesCache.get(key);
		if (result == null) {
			result = Integer.parseInt(getString(key));
			propertiesCache.put(key, result);
		}
		return result;
	}

	public Integer getInt(String key, int defaultValue) {
		Integer result = (Integer) propertiesCache.get(key);
		if (result == null) {
			String value = getString(key);
			result = value != null ? Integer.parseInt(value) : defaultValue;
			propertiesCache.put(key, result);
		}
		return result;
	}

	public static Boolean getBooleanValue(String key) {
		Boolean result = (Boolean) propertiesCache.get(key);
		if (result == null) {
			result = Boolean.parseBoolean(getString(key));
			propertiesCache.put(key, result);
		}
		return result;
	}
}
