package br.com.codeminimal.minimojs;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

public class XTests {

	public List<XTests> list;

	public static void main(String[] args) {
		try {
			Type[] types = XTests.class.getField("list").getType().getGenericInterfaces();
			for (Type type : types) {
				System.out.println(type);
			}
			Type t = XTests.class.getField("list").getGenericType();
			System.out.println(t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main1(String[] args) {
		Gson g = new Gson();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("a", 1);
		map.put("b", 2);
		map.put("c", 3);
		map.put("d", null);
		System.out.println(g.toJson(map));
	}

}
