package br.com.kodeless.minimaljs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class XRequest implements HttpServletRequest {

	private HttpServletRequest originalRequest;
	private XSession session;

	XRequest(HttpServletRequest request) {
		this.originalRequest = request;
	}

	public Object getAttribute(String name) {
		return originalRequest.getAttribute(name);
	}

	public String getAuthType() {
		return originalRequest.getAuthType();
	}

	public Cookie[] getCookies() {
		return originalRequest.getCookies();
	}

	public Enumeration getAttributeNames() {
		return originalRequest.getAttributeNames();
	}

	public long getDateHeader(String name) {
		return originalRequest.getDateHeader(name);
	}

	public String getCharacterEncoding() {
		return originalRequest.getCharacterEncoding();
	}

	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
		originalRequest.setCharacterEncoding(env);
	}

	public String getHeader(String name) {
		return originalRequest.getHeader(name);
	}

	public int getContentLength() {
		return originalRequest.getContentLength();
	}

	public String getContentType() {
		return originalRequest.getContentType();
	}

	public Enumeration getHeaders(String name) {
		return originalRequest.getHeaders(name);
	}

	public ServletInputStream getInputStream() throws IOException {
		return originalRequest.getInputStream();
	}

	public String getParameter(String name) {
		return originalRequest.getParameter(name);
	}

	public Enumeration getHeaderNames() {
		return originalRequest.getHeaderNames();
	}

	public int getIntHeader(String name) {
		return originalRequest.getIntHeader(name);
	}

	public Enumeration getParameterNames() {
		return originalRequest.getParameterNames();
	}

	public String getMethod() {
		return originalRequest.getMethod();
	}

	public String[] getParameterValues(String name) {
		return originalRequest.getParameterValues(name);
	}

	public String getPathInfo() {
		return originalRequest.getPathInfo();
	}

	public Map getParameterMap() {
		return originalRequest.getParameterMap();
	}

	public String getProtocol() {
		return originalRequest.getProtocol();
	}

	public String getPathTranslated() {
		return originalRequest.getPathTranslated();
	}

	public String getScheme() {
		return originalRequest.getScheme();
	}

	public String getContextPath() {
		return originalRequest.getContextPath();
	}

	public String getServerName() {
		return originalRequest.getServerName();
	}

	public int getServerPort() {
		return originalRequest.getServerPort();
	}

	public BufferedReader getReader() throws IOException {
		return originalRequest.getReader();
	}

	public String getQueryString() {
		return originalRequest.getQueryString();
	}

	public String getRemoteUser() {
		return originalRequest.getRemoteUser();
	}

	public String getRemoteAddr() {
		return originalRequest.getRemoteAddr();
	}

	public String getRemoteHost() {
		return originalRequest.getRemoteHost();
	}

	public boolean isUserInRole(String role) {
		return originalRequest.isUserInRole(role);
	}

	public void setAttribute(String name, Object o) {
		originalRequest.setAttribute(name, o);
	}

	public Principal getUserPrincipal() {
		return originalRequest.getUserPrincipal();
	}

	public String getRequestedSessionId() {
		return originalRequest.getRequestedSessionId();
	}

	public void removeAttribute(String name) {
		originalRequest.removeAttribute(name);
	}

	public String getRequestURI() {
		return originalRequest.getRequestURI();
	}

	public Locale getLocale() {
		return originalRequest.getLocale();
	}

	public Enumeration getLocales() {
		return originalRequest.getLocales();
	}

	public StringBuffer getRequestURL() {
		return originalRequest.getRequestURL();
	}

	public boolean isSecure() {
		return originalRequest.isSecure();
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		return originalRequest.getRequestDispatcher(path);
	}

	public String getServletPath() {
		return originalRequest.getServletPath();
	}

	public HttpSession getSession(boolean create) {
		return originalRequest.getSession(create);
	}

	public String getRealPath(String path) {
		return originalRequest.getRealPath(path);
	}

	public int getRemotePort() {
		return originalRequest.getRemotePort();
	}

	public String getLocalName() {
		return originalRequest.getLocalName();
	}

	public synchronized HttpSession getSession() {
		if (this.session == null) {
			this.session = new XSession(originalRequest.getSession());
		}
		return session;
	}

	public String getLocalAddr() {
		return originalRequest.getLocalAddr();
	}

	public boolean isRequestedSessionIdValid() {
		return originalRequest.isRequestedSessionIdValid();
	}

	public int getLocalPort() {
		return originalRequest.getLocalPort();
	}

	public boolean isRequestedSessionIdFromCookie() {
		return originalRequest.isRequestedSessionIdFromCookie();
	}

	public boolean isRequestedSessionIdFromURL() {
		return originalRequest.isRequestedSessionIdFromURL();
	}

	public boolean isRequestedSessionIdFromUrl() {
		return originalRequest.isRequestedSessionIdFromUrl();
	}

}
