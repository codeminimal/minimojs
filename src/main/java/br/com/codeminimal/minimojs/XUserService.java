package br.com.codeminimal.minimojs;

import java.util.ArrayList;
import java.util.List;

import br.com.codeminimal.minimojs.dao.XDAO;
import br.com.codeminimal.minimojs.model.XUser;

@XObject(alias = "XUserService")
public class XUserService {

	private static List<XLoginListener> list = new ArrayList<XUserService.XLoginListener>();
	private static GetUser getUser;

	public static void addLoginListener(XLoginListener listener) {
		list.add(listener);
	}

	@XMethod(loginRequired = false, useWebObjects = true)
	public XUser login(String userLogin, String password) {
		XUser user;
		String[] login = userLogin.split("@");
		if (getUser == null) {
			XDAO<XUser> userDAO = new XDAO<XUser>(XUser.class);
			String username = login[0];
			String domain = "";
			if (login.length == 2) {
				domain = login[1];
			}
			user = userDAO.unique("select u from XUser u where u.login = ? and u.domain = ?", username, domain);
		} else {
			user = getUser.unique(userLogin);
		}
		String hash = XStringUtil.generateHashMD5(password);
		if (user != null && hash.equals(user.getPassword())) {
			boolean allOk = true;
			for (XLoginListener listener : list) {
				if (!listener.onLogin(user)) {
					allOk = false;
					break;
				}
			}
			if (allOk) {
				XContext.setUser(user);
				return user;
			}
		}
		return null;
	}

	@XMethod(loginRequired = true, useWebObjects = true)
	public String getSessionId() {
		return XContext.getSession().getId();
	}

	@XMethod(useWebObjects = true)
	public void logout() {
		XContext.setUser(null);
	}

	public void create(XUser user) {
		XDAO<XUser> userDAO = new XDAO<XUser>(XUser.class);
		user.setPassword(XStringUtil.generateHashMD5(user.getPassword()));
		userDAO.insert(user);
	}

	public boolean exists(String name) {
		XDAO<XUser> userDAO = new XDAO<XUser>(XUser.class);
		return userDAO.uniqueBy("login", name) != null;
	}

	public static interface XLoginListener {
		boolean onLogin(XUser user);
	}

	public static void setGetUserForLogin(GetUser g) {
		getUser = g;
	}

	public static interface GetUser {

		public XUser unique(String login);

	}
}
