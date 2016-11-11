package br.com.kodeless.minimojs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class XResponse implements HttpServletResponse {

	private HttpServletResponse originalResponse;
	private boolean outputUsed;

	XResponse(HttpServletResponse response) {
		this.originalResponse = response;
	}

	public boolean isOutputUsed() {
		return outputUsed;
	}

	public void addCookie(Cookie cookie) {
		originalResponse.addCookie(cookie);
	}

	public boolean containsHeader(String name) {
		return originalResponse.containsHeader(name);
	}

	public String encodeURL(String url) {
		return originalResponse.encodeURL(url);
	}

	public String getCharacterEncoding() {
		return originalResponse.getCharacterEncoding();
	}

	public String encodeRedirectURL(String url) {
		return originalResponse.encodeRedirectURL(url);
	}

	public String getContentType() {
		return originalResponse.getContentType();
	}

	public String encodeUrl(String url) {
		return originalResponse.encodeUrl(url);
	}

	public String encodeRedirectUrl(String url) {
		return originalResponse.encodeRedirectUrl(url);
	}

	public ServletOutputStream getOutputStream() throws IOException {
		this.outputUsed = true;
		return originalResponse.getOutputStream();
	}

	public void sendError(int sc, String msg) throws IOException {
		originalResponse.sendError(sc, msg);
	}

	public PrintWriter getWriter() throws IOException {
		this.outputUsed = true;
		return originalResponse.getWriter();
	}

	public void sendError(int sc) throws IOException {
		originalResponse.sendError(sc);
	}

	public void sendRedirect(String location) throws IOException {
		originalResponse.sendRedirect(location);
	}

	public void setCharacterEncoding(String charset) {
		originalResponse.setCharacterEncoding(charset);
	}

	public void setDateHeader(String name, long date) {
		originalResponse.setDateHeader(name, date);
	}

	public void addDateHeader(String name, long date) {
		originalResponse.addDateHeader(name, date);
	}

	public void setHeader(String name, String value) {
		originalResponse.setHeader(name, value);
	}

	public void setContentLength(int len) {
		originalResponse.setContentLength(len);
	}

	public void setContentType(String type) {
		originalResponse.setContentType(type);
	}

	public void addHeader(String name, String value) {
		originalResponse.addHeader(name, value);
	}

	public void setIntHeader(String name, int value) {
		originalResponse.setIntHeader(name, value);
	}

	public void addIntHeader(String name, int value) {
		originalResponse.addIntHeader(name, value);
	}

	public void setBufferSize(int size) {
		originalResponse.setBufferSize(size);
	}

	public void setStatus(int sc) {
		originalResponse.setStatus(sc);
	}

	public void setStatus(int sc, String sm) {
		originalResponse.setStatus(sc, sm);
	}

	public int getBufferSize() {
		return originalResponse.getBufferSize();
	}

	public void flushBuffer() throws IOException {
		originalResponse.flushBuffer();
	}

	public void resetBuffer() {
		originalResponse.resetBuffer();
	}

	public boolean isCommitted() {
		return originalResponse.isCommitted();
	}

	public void reset() {
		originalResponse.reset();
	}

	public void setLocale(Locale loc) {
		originalResponse.setLocale(loc);
	}

	public Locale getLocale() {
		return originalResponse.getLocale();
	}
}
