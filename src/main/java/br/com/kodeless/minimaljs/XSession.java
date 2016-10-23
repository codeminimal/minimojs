package br.com.kodeless.minimaljs;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

public class XSession implements HttpSession {

	private HttpSession originalSession;

	XSession(HttpSession session) {
		this.originalSession = session;
	}

	public long getCreationTime() {
		return originalSession.getCreationTime();
	}

	public String getId() {
		return originalSession.getId();
	}

	public long getLastAccessedTime() {
		return originalSession.getLastAccessedTime();
	}

	public ServletContext getServletContext() {
		return originalSession.getServletContext();
	}

	public void setMaxInactiveInterval(int interval) {
		originalSession.setMaxInactiveInterval(interval);
	}

	public int getMaxInactiveInterval() {
		return originalSession.getMaxInactiveInterval();
	}

	public HttpSessionContext getSessionContext() {
		return originalSession.getSessionContext();
	}

	public Object getAttribute(String name) {
		return originalSession.getAttribute(name);
	}

	public Object getValue(String name) {
		return originalSession.getValue(name);
	}

	public Enumeration getAttributeNames() {
		return originalSession.getAttributeNames();
	}

	public String[] getValueNames() {
		return originalSession.getValueNames();
	}

	public void setAttribute(String name, Object value) {
		originalSession.setAttribute(name, value);
	}

	public void putValue(String name, Object value) {
		originalSession.putValue(name, value);
	}

	public void removeAttribute(String name) {
		originalSession.removeAttribute(name);
	}

	public void removeValue(String name) {
		originalSession.removeValue(name);
	}

	public void invalidate() {
		originalSession.invalidate();
	}

	public boolean isNew() {
		return originalSession.isNew();
	}
}
