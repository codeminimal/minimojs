package br.com.kodeless.minimaljs.dao;

public class XDAOException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public XDAOException(String msg, Throwable t) {
		super(msg, t);
	}

	public XDAOException(String msg) {
		super(msg);
	}

	public XDAOException(Throwable t) {
		super(t);
	}

}
