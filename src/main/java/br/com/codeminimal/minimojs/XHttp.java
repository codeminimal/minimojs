package br.com.codeminimal.minimojs;

import java.io.IOException;

import javax.servlet.http.Cookie;

public class XHttp {

	public static void setCookie(String domain, String key, String value, int expires) throws IOException {
		XContext.getXResponse().setContentType("text/html");
		Cookie cookie = new Cookie(key, value);
		cookie.setMaxAge(expires);
		cookie.setPath(XContext.getXRequest().getContextPath());
		XContext.getXResponse().addCookie(cookie);
	}

	public static String getCookie(String key) throws IOException {
		String value = null;
		Cookie[] cookie = XContext.getXRequest().getCookies();
		for (Cookie obj : cookie) {
			if (obj.getName().equals(key)) {
				value = obj.getValue();
				break;
			}
		}
		return value;
	}

	public static void setCachedResponseHeader(int seconds) {
		if (!XContext.isDevMode()) {
			XContext.getXResponse().addHeader("Cache-Control", "max-age=" + seconds);
		}
	}

	public static void setAvoidCache() {
		if (!XContext.isDevMode()) {
			XContext.getXResponse().addHeader("Cache-Control", "no-cache");
		}
	}
}
