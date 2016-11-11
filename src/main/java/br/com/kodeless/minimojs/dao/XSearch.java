package br.com.kodeless.minimojs.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class XSearch<T> {

	protected Map<String, Object> eq = new HashMap<String, Object>();
	protected Map<String, Object[]> in = new HashMap<String, Object[]>();
	protected Map<String, Object> like = new HashMap<String, Object>();
	protected Map<String, String> join = new HashMap<String, String>();
	protected Map<String, Object> gt = new HashMap<String, Object>();
	protected Map<String, Object> lt = new HashMap<String, Object>();
	protected Map<String, Object> ge = new HashMap<String, Object>();
	protected Map<String, Object> le = new HashMap<String, Object>();
	protected XOrderBy order;

	protected XPagination pagination;

	public XSearch<T> eq(String name, Object value) {
		eq.put(name, value);
		return this;
	}

	public XSearch<T> gt(String name, Object value) {
		gt.put(name, value);
		return this;
	}

	public XSearch<T> lt(String name, Object value) {
		lt.put(name, value);
		return this;
	}
	
	public XSearch<T> ge(String name, Object value) {
		ge.put(name, value);
		return this;
	}

	public XSearch<T> le(String name, Object value) {
		le.put(name, value);
		return this;
	}

	public XSearch<T> in(String nome, Object... values) {
		in.put(nome, values);
		return this;
	}

	public XSearch<T> like(String nome, Object value) {
		like.put(nome, value);
		return this;
	}

	public XSearch<T> join(String property, String alias) {
		join.put(property, alias);
		return this;
	}

	public XSearch<T> setOrder(XOrderBy order) {
		this.order = order;
		return this;
	}

	public XSearch<T> setPagination(XPagination pagination) {
		this.pagination = pagination;
		return this;
	}

	public abstract List<T> execute();

	public abstract long count();
}
