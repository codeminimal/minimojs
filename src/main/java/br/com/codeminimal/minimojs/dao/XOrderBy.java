package br.com.codeminimal.minimojs.dao;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;

public class XOrderBy {

    private boolean asc;
    private String[] properties;

    public XOrderBy(String... properties) {
        this(true, properties);
    }

    public XOrderBy(boolean asc, String... properties) {
        this.asc = asc;
        this.properties = properties;
    }

    protected void configCriteria(DetachedCriteria criteria) {
        for (String property : properties) {
            if (asc) {
                criteria.addOrder(Order.asc(property));
            } else {
                criteria.addOrder(Order.desc(property));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(" order by ");
        for (int i = 0; i < properties.length; i++) {
            sb.append(properties[i]).append(asc ? " asc" : " desc");
            if (i < properties.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
