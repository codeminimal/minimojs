package br.com.kodeless.minimaljs;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class XBeanUtil {

	@SuppressWarnings("rawtypes")
	public static final boolean equals(Object o1, Object o2) {
		if ((o1 == null && o2 == null) || o1.equals(o2)) {
			return true;
		} else if (o1.getClass().equals(o2.getClass())) {
			Class c = o1.getClass();
			if (c.isArray()) {
				if (Array.getLength(o1) != Array.getLength(o2)) {
					return false;
				}
				for (int i = 0; i < Array.getLength(o1); i++) {
					if (!equals(Array.get(o1, i), Array.get(o2, i))) {
						return false;
					}
				}
				return true;
			} else if (Collection.class.isAssignableFrom(o1.getClass())) {
				if (((Collection) o1).size() != ((Collection) o2).size()) {
					return false;
				}
				Iterator it1 = ((Collection) o1).iterator();
				Iterator it2 = ((Collection) o1).iterator();
				while (it1.hasNext()) {
					if (!equals(it1.next(), it2.next())) {
						return false;
					}
				}
				return true;
			} else if (Map.class.isAssignableFrom(o1.getClass())) {
				if (((Map) o1).size() != ((Map) o2).size()) {
					return false;
				}
				Iterator kIt = ((Map) o1).keySet().iterator();
				while (kIt.hasNext()) {
					Object k = kIt.next();
					if (!equals(((Map) o1).get(k), ((Map) o2).get(k))) {
						return false;
					}
				}
				return true;
			}
			try {
				while (c != null) {
					for (Field field : c.getDeclaredFields()) {
						field.setAccessible(true);
						Object v1 = field.get(o1);
						Object v2 = field.get(o2);
						if (v1 != v2) {
							if (!equals(v1, v2)) {
								return false;
							}
						}
					}
					c = c.getSuperclass();
				}
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}
}
