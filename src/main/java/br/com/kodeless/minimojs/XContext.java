package br.com.kodeless.minimojs;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import br.com.kodeless.minimojs.model.XUser;
import org.hibernate.Session;

public class XContext {

	private static final ThreadLocal<Boolean> threadUseWebObjects = new ThreadLocal<Boolean>();
	private static final ThreadLocal<XFile> threadXFile = new ThreadLocal<XFile>();
	private static final ThreadLocal<HttpSession> threadSession = new ThreadLocal<HttpSession>();
	private static final ThreadLocal<HttpServletRequest> threadRequest = new ThreadLocal<HttpServletRequest>();
	private static final ThreadLocal<HttpServletResponse> threadResponse = new ThreadLocal<HttpServletResponse>();
	private static final ThreadLocal<Session> threadHibernateSession = new ThreadLocal<Session>();
	private static final ThreadLocal<Boolean> hasUserContext = new ThreadLocal<Boolean>();
	private static final ThreadLocal<XUser> noContextUser = new ThreadLocal<XUser>();
	private static final ThreadLocal<Boolean> inTransaction = new ThreadLocal<Boolean>();

	private static final List<LoginCallback<?>> onLoginList = new ArrayList<LoginCallback<?>>();

	public static interface LoginCallback<T extends XUser> {
		void onLogin(T user);

		void onLogout(T user);
	}

	private static boolean devMode;

	protected static final void setUseWebObjects(boolean use) {
		threadUseWebObjects.set(use);
	}

	protected static final Boolean getUseWebObjects() {
		Boolean use = threadUseWebObjects.get();
		return use != null && use;
	}

	protected static final void setXSession(HttpSession session) {
		threadSession.set(session);
	}

	protected static final HttpSession getXSession() {
		return threadSession.get();
	}

	protected static final void setXRequest(HttpServletRequest request) {
		threadRequest.set(request);
	}

	protected static final HttpServletRequest getXRequest() {
		return threadRequest.get();
	}

	protected static final void setXResponse(HttpServletResponse response) {
		threadResponse.set(response);
	}

	protected static final HttpServletResponse getXResponse() {
		return threadResponse.get();
	}

	public static final XUser getUser() {
		if (!getInUserContext()) {
			return noContextUser.get();
		} else if (getXSession() != null) {
			return (XUser) getXSession().getAttribute("__x_user");
		}
		return null;
	}

	public static final boolean isInTransaction() {
		return inTransaction.get() == null ? false : inTransaction.get();
	}

	protected static final void setInTransaction(boolean b) {
		inTransaction.set(b);
	}

	@SuppressWarnings("all")
	protected static final void setUser(XUser user) {
		if (getXSession() != null) {
			for (LoginCallback onLogin : onLoginList) {
				if (user != null) {
					onLogin.onLogin(user);
				} else {
					onLogin.onLogout(getUser());
				}
			}
			getXSession().setAttribute("__x_user", user);
		}
	}

	public static final void setContextUser(XUser user) {
		if (!getInUserContext()) {
			noContextUser.set(user);
		} else {
			throw new RuntimeException("Cant change user in this context!");
		}
	}

	public static final void setObject(String name, Object o) {
		if (getXSession() != null) {
			getXSession().setAttribute("__x_object_" + name, o);
		} else {
			throw new RuntimeException("WebObjects are not enabled in this method.");
		}
	}

	public static final Object getObject(String name) {
		if (getXSession() != null) {
			return getXSession().getAttribute("__x_object_" + name);
		}
		return null;
	}

	public static final Object popObject(String name) {
		if (getXSession() != null) {
			Object result = getXSession().getAttribute("__x_object_" + name);
			getXSession().removeAttribute("__x_object_" + name);
			return result;
		}
		return null;
	}

	public static final HttpSession getSession() {
		if (!getUseWebObjects()) {
			throw new RuntimeException("Method do not allow web objects");
		}
		return getXSession();
	}

	public static final HttpServletRequest getRequest() {
		if (!getUseWebObjects()) {
			throw new RuntimeException("Method do not allow web objects");
		}
		return getXRequest();
	}

	public static final HttpServletResponse getResponse() {
		if (!getUseWebObjects()) {
			throw new RuntimeException("Method do not allow web objects");
		}
		return getXResponse();
	}

	protected static final void setFileUpload(XFile file) {
		threadXFile.set(file);
	}

	public static final XFile getFileUpload() {
		return threadXFile.get();
	}

	public static final Session getPersistenceSession() {
		return threadHibernateSession.get();
	}

	protected static final void setPersistenceSession(Session s) {
		threadHibernateSession.set(s);
	}

	public static final Boolean getInUserContext() {
		return hasUserContext.get() == null || hasUserContext.get();
	}

	protected static final void setInUserContext(Boolean hasCtx) {
		hasUserContext.set(hasCtx);
	}

	protected static final void setDevMode(boolean b) {
		devMode = b;
	}

	public static final boolean isDevMode() {
		return devMode;
	}

	public static final void addLoginCallback(LoginCallback<?> onLogin) {
		onLoginList.add(onLogin);
	}

	public static final void removeLoginCallback(LoginCallback<?> onLogin) {
		onLoginList.remove(onLogin);
	}

	public static final boolean isUserAllowedTo(String function) {
		return getUser().getAvailableFunctions() != null && getUser().getAvailableFunctions().contains(function);
	}
	
	public static final boolean isUserInRole(String role) {
		return getUser().getRole() != null && getUser().getRole().equals(role);
	}
}
