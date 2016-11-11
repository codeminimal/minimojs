package br.com.kodeless.minimojs.dao;

import org.hibernate.Criteria;

public class XPagination {

	protected int pageSize;
	protected int pageIndex;

	public XPagination(int pageSize, int pageIndex) {
		this.pageSize = pageSize;
		this.pageIndex = pageIndex;
	}

	protected void configCriteria(Criteria criteria) {
		criteria.setFirstResult(pageIndex * pageSize).setMaxResults(pageSize);
	}

	public static final int countPages(long qtd, int pageSize) {
		return (int) Math.ceil(((double) qtd) / pageSize);
	}
}
