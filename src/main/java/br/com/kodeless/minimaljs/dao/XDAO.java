package br.com.kodeless.minimaljs.dao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import br.com.kodeless.minimaljs.XContext;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;

import br.com.kodeless.minimaljs.model.XEntity;

@SuppressWarnings("unchecked")
public class XDAO<T extends XEntity> {

    private Class<T> clazz;

    public XDAO(Class<T> cl) {
        this.clazz = cl;
    }

    public List<T> findAll() {
        return findAll(null, null, null);
    }

    public List<T> findAll(XPagination pagination) {
        return findAll(null, null, pagination);
    }

    public List<T> findAll(XOrderBy orderBy) {
        return findAll(orderBy, null, null);
    }

    public List<T> findAll(XOrderBy orderBy, XPagination pagination) {
        return findAll(orderBy, null, pagination);
    }

    public <C> List<C> findAll(Class<? extends T> cl) {
        return (List<C>) findAll(null, cl, null);
    }

    public <C> List<C> findAll(Class<? extends T> cl, XPagination pagination) {
        return (List<C>) findAll(null, cl, pagination);
    }

    public <C> List<C> findAll(XOrderBy orderBy, Class<? extends T> cl) {
        return findAll(orderBy, cl, null);
    }

    public <C> List<C> findAll(XOrderBy orderBy, Class<? extends T> cl, XPagination pagination) {
        DetachedCriteria criteria = createCriteria(cl);
        if (orderBy != null) {
            orderBy.configCriteria(criteria);
        }
        return (List<C>) find(criteria, pagination);
    }

    public long countAll() {
        return countAll(null);
    }

    public long countAll(Class<? extends T> cl) {
        return countCriteria(createCriteria(cl));
    }

    public List<T> searchLike(String property, String value) {
        return searchLike(property, value, null, null, null);
    }

    public List<T> searchLike(String property, String value, XPagination pagination) {
        return searchLike(property, value, null, null, pagination);
    }

    public List<T> searchLike(String property, String value, XOrderBy orderBy) {
        return searchLike(property, value, orderBy, null, null);
    }

    public List<T> searchLike(String property, String value, XOrderBy orderBy, XPagination pagination) {
        return searchLike(property, value, orderBy, null, pagination);
    }

    public <C> List<C> searchLike(String property, String value, Class<? extends T> cl) {
        return (List<C>) searchLike(property, value, null, cl, null);
    }

    public <C> List<C> searchLike(String property, String value, Class<? extends T> cl, XPagination pagination) {
        return (List<C>) searchLike(property, value, null, cl, pagination);
    }

    public <C> List<C> searchLike(String property, String value, XOrderBy orderBy, Class<? extends T> cl) {
        return (List<C>) searchLike(property, value, orderBy, cl, null);
    }

    public <C> List<C> searchLike(String property, String value, XOrderBy orderBy, Class<? extends T> cl,
                                  XPagination pagination) {
        DetachedCriteria criteria = createCriteria(cl);
        criteria.add(Property.forName(property).like(value, MatchMode.ANYWHERE));
        if (orderBy != null) {
            orderBy.configCriteria(criteria);
        }
        return (List<C>) find(criteria, pagination);
    }

    public long countSearchLike(String property, String value) {
        return countSearchLike(property, value, null);
    }

    public long countSearchLike(String property, String value, Class<? extends T> cl) {
        DetachedCriteria criteria = createCriteria(cl);
        criteria.add(Property.forName(property).like(value, MatchMode.ANYWHERE));
        return countCriteria(criteria);
    }

    private long countCriteria(DetachedCriteria criteria) {
        long result = (Long) criteria.setProjection(Projections.rowCount())
                .getExecutableCriteria(XContext.getPersistenceSession()).uniqueResult();
        criteria.setProjection(null);
        criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        return result;
    }

    public XSearch<T> search() {
        return new XSearch<T>() {

            @Override
            public List<T> execute() {
                return execSearch(this);
            }

            @Override
            public long count() {
                return countSearch(this);
            }
        };
    }

    private List<T> execSearch(XSearch<T> search) {
        DetachedCriteria criteria = createCriteria((Class) null);
        configProperties(criteria, search);

        if (search.order != null) {
            search.order.configCriteria(criteria);
        }
        List<T> result = (List<T>) find(criteria, search.pagination);
        XContext.getPersistenceSession().evict(result);
        return result;
    }

    private long countSearch(XSearch<T> search) {
        DetachedCriteria criteria = createCriteria((Class) null);
        configProperties(criteria, search);

        return countCriteria(criteria);
    }

    public <C> XSearch<C> search(final Class<? extends T> cl) {
        return new XSearch<C>() {

            @Override
            public List<C> execute() {
                DetachedCriteria criteria = createCriteria(cl);
                configProperties(criteria, this);
                if (this.order != null) {
                    order.configCriteria(criteria);
                }
                return (List<C>) find(criteria, this.pagination);
            }

            @Override
            public long count() {
                DetachedCriteria criteria = createCriteria(cl);
                configProperties(criteria, this);
                return countCriteria(criteria);
            }
        };
    }

    private void configProperties(DetachedCriteria criteria, XSearch<?> search) {
        for (Map.Entry<String, String> entry : search.join.entrySet()) {
            criteria.createAlias(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Object> entry : search.eq.entrySet()) {
            criteria.add(Property.forName(entry.getKey()).eq(entry.getValue()));
        }
        for (Map.Entry<String, Object[]> entry : search.in.entrySet()) {
            criteria.add(Property.forName(entry.getKey()).in(entry.getValue()));
        }
        for (Map.Entry<String, Object> entry : search.like.entrySet()) {
            criteria.add(Property.forName(entry.getKey()).like((String) entry.getValue(), MatchMode.ANYWHERE));
        }
        for (Map.Entry<String, Object> entry : search.gt.entrySet()) {
            criteria.add(Property.forName(entry.getKey()).gt(entry.getValue()));
        }
        for (Map.Entry<String, Object> entry : search.ge.entrySet()) {
            criteria.add(Property.forName(entry.getKey()).ge(entry.getValue()));
        }
        for (Map.Entry<String, Object> entry : search.lt.entrySet()) {
            criteria.add(Property.forName(entry.getKey()).lt(entry.getValue()));
        }
        for (Map.Entry<String, Object> entry : search.le.entrySet()) {
            criteria.add(Property.forName(entry.getKey()).le(entry.getValue()));
        }
    }

    public List<T> findBy(String property, Object value) {
        return findBy(property, value, null, null, null);
    }

    public List<T> findBy(String property, Object value, XPagination pagination) {
        return findBy(property, value, null, null, pagination);
    }

    public List<T> findBy(String property, Object value, XOrderBy orderBy) {
        return findBy(property, value, orderBy, null, null);
    }

    public List<T> findBy(String property, Object value, XOrderBy orderBy, XPagination pagination) {
        return findBy(property, value, orderBy, null, pagination);
    }

    public <C> List<C> findBy(String property, Object value, Class<? extends T> cl) {
        return (List<C>) findBy(property, value, null, cl, null);
    }

    public <C> List<C> findBy(String property, Object value, Class<? extends T> cl, XPagination pagination) {
        return (List<C>) findBy(property, value, null, cl, pagination);
    }

    public <C> List<C> findBy(String property, Object value, XOrderBy orderBy, Class<? extends T> cl) {
        return findBy(property, value, orderBy, cl, null);
    }

    public <C> List<C> findBy(String property, Object value, XOrderBy orderBy, Class<? extends T> cl,
                              XPagination pagination) {
        DetachedCriteria criteria = createCriteria(cl);
        criteria.add(Property.forName(property).eq(value));
        if (orderBy != null) {
            orderBy.configCriteria(criteria);
        }
        List<C> result = (List<C>) findByCriteria(criteria, pagination);
        for (C c : result) {
            XContext.getPersistenceSession().evict(c);
        }
        return result;
    }

    public long countBy(String property, Object value) {
        return countBy(property, value, null);
    }

    public long countBy(String property, Object value, Class<? extends T> cl) {
        DetachedCriteria criteria = createCriteria(cl);
        criteria.add(Property.forName(property).eq(value));
        return countCriteria(criteria);
    }

    public List<T> findByCriteria(final DetachedCriteria criteria, final int firstResult, final int maxResults) {

        Criteria executableCriteria = criteria.getExecutableCriteria(XContext.getPersistenceSession());
        if (firstResult >= 0) {
            executableCriteria.setFirstResult(firstResult);
        }
        if (maxResults > 0) {
            executableCriteria.setMaxResults(maxResults);
        }
        return (List<T>) executableCriteria.list();
    }

    public List<? extends XEntity> findByCriteria(DetachedCriteria criteria, XPagination pagination) {
        Criteria executableCriteria = criteria.getExecutableCriteria(XContext.getPersistenceSession());
        if (pagination != null) {
            pagination.configCriteria(executableCriteria);
        }
        return executableCriteria.list();
    }

    public long countByCriteria(final DetachedCriteria criteria) {
        return countCriteria(criteria);
    }

    public T uniqueById(Long id) {
        return uniqueById(id, null);
    }

    public <C> C uniqueById(Long id, Class<? extends T> cl) {
        DetachedCriteria criteria = createCriteria(cl);
        criteria.add(Property.forName("id").eq(id));
        return (C) unique(criteria);
    }

    public T evictedById(Long id) {
        T result = uniqueById(id, null);
        XContext.getPersistenceSession().evict(result);
        return result;
    }

    public <C> C evictedById(Long id, Class<? extends T> cl) {
        DetachedCriteria criteria = createCriteria(cl);
        criteria.add(Property.forName("id").eq(id));
        C result = (C) unique(criteria);
        XContext.getPersistenceSession().evict(result);
        return result;
    }

    public DetachedCriteria createCriteria() {
        return createCriteria((Class) null);
    }

    public DetachedCriteria createCriteria(String alias) {
        return DetachedCriteria.forClass(clazz, alias);
    }

    public DetachedCriteria createCriteria(Class<? extends T> cl) {
        if (cl == null) {
            return DetachedCriteria.forClass(clazz);
        } else {
            return DetachedCriteria.forClass(cl);
        }
    }

    public T uniqueBy(String property, Object value) {
        return uniqueBy(property, value, null);
    }

    public <C> C uniqueBy(String property, Object value, Class<? extends T> cl) {
        DetachedCriteria criteria = createCriteria(cl);
        criteria.add(Property.forName(property).eq(value));

        return (C) unique(criteria);
    }

    public void saveOrUpdate(T entity) {
        checkTransaction("saveOrUpdate");
        XContext.getPersistenceSession().saveOrUpdate(entity);
    }

    private void checkTransaction(String methodName) {
        if (!XContext.isInTransaction()) {
            throw new XDAOException("Not in trasaction request! There must be a transcation to call " + methodName);
        }
    }

    protected T unique(DetachedCriteria criteria) {

        List<T> resultList = (List<T>) findByCriteria(criteria, null);

        if (!resultList.isEmpty())
            return (T) resultList.get(0);

        return null;

    }

    public List<T> find(String query) {
        return find(query, (XPagination) null);
    }

    public List<T> find(String query, XPagination pagination) {
        Query q = XContext.getPersistenceSession().createQuery(query);
        if (pagination != null) {
            q.setFirstResult(pagination.pageIndex * pagination.pageSize);
            q.setMaxResults(pagination.pageSize);
        }
        return (List<T>) q.list();
    }

    public long count(StringBuilder query) {
        Query q = XContext.getPersistenceSession().createQuery(query.toString());
        return (Long) q.uniqueResult();
    }

    public T unique(String query) {

        List<T> resultList = find(query);

        if (!resultList.isEmpty())
            return (T) resultList.get(0);

        return null;
    }

    public T unique(String query, Object... values) {

        List<T> resultList = find(query, values);

        if (!resultList.isEmpty())
            return (T) resultList.get(0);

        return null;
    }

    protected List<T> find(DetachedCriteria criteria, XPagination pagination) {
        return (List<T>) findByCriteria(criteria, pagination);
    }

    protected List<T> find(DetachedCriteria criteria, int start, int max) {
        return (List<T>) findByCriteria(criteria, start, max);
    }

    public List<T> find(String query, List<?> values) {
        return (List<T>) exec(query, values, null, false);
    }

    public List<T> find(String query, XPagination pagination, List<?> values) {
        return (List<T>) exec(query, values, pagination, false);
    }

    public List<T> find(String query, Object... values) {
        return (List<T>) exec(query, values, null, false);
    }

    public List<T> find(String query, XPagination pagination, Object... values) {
        return (List<T>) exec(query, values, pagination, false);
    }

    private Object exec(String query, Object valueList, XPagination pagination, boolean unique) {
        Query q = XContext.getPersistenceSession().createQuery(query);
        int count = -1;
        if (valueList instanceof List) {
            for (Object param : (List<?>) valueList) {
                count = setParameter(q, count, param);
            }
        } else {
            for (Object param : (Object[]) valueList) {
                count = setParameter(q, count, param);
            }
        }
        if (pagination != null) {
            q.setFirstResult(pagination.pageIndex * pagination.pageSize);
            q.setMaxResults(pagination.pageSize);
        }
        if (unique) {
            return q.uniqueResult();
        } else {
            return q.list();
        }
    }

    private int setParameter(Query q, int count, Object param) {
        if (param instanceof BigDecimal) {
            q.setBigDecimal(++count, (BigDecimal) param);
        } else if (param instanceof String) {
            q.setString(++count, (String) param);
        } else if (param instanceof BigInteger) {
            q.setBigInteger(++count, (BigInteger) param);
        } else if (param instanceof byte[]) {
            q.setBinary(++count, (byte[]) param);
        } else if (param instanceof Boolean) {
            q.setBoolean(++count, (Boolean) param);
        } else if (param instanceof Calendar) {
            q.setCalendar(++count, (Calendar) param);
        } else if (param instanceof Date) {
            q.setDate(++count, (Date) param);
        } else if (param instanceof Double) {
            q.setDouble(++count, (Double) param);
        } else if (param instanceof Integer) {
            q.setInteger(++count, (Integer) param);
        } else if (param instanceof Long) {
            q.setLong(++count, (Long) param);
        } else if (param instanceof XEntity) {
            q.setEntity(++count, param);
        } else {
            q.setParameter(++count, param);
        }
        return count;
    }

    public long count(String query, Object... values) {
        return (Long) exec(query, values, null, true);
    }

    public long count(String query, List<?> values) {
        return (Long) exec(query, values, null, true);
    }

    public void insert(final T entity) {
        checkTransaction("insert");
        XContext.getPersistenceSession().persist(entity);
    }

    public void update(final T entity) {
        checkTransaction("update");
        XContext.getPersistenceSession().update(entity);
    }

    public void merge(final T entity) {
        checkTransaction("merge");
        XContext.getPersistenceSession().merge(entity);
    }

    public void delete(final T entity) {
        checkTransaction("delete");
        XContext.getPersistenceSession().delete(entity);
    }

    public void deleteAll(final List<T> entities) {
        checkTransaction("deleteAll");
        for (T t : entities) {
            delete(t);
        }
    }

    public XSearch<T> createSearchFromHQL(final String query, final String aliasToSelect, final Object... parameters) {
        return new XSearch<T>() {

            @Override
            public List<T> execute() {
                String select = "select " + aliasToSelect + " " + query;
                if (this.order != null) {
                    select += this.order;
                }
                return pagination != null ? find(select, pagination, parameters) : find(select, parameters);
            }

            @Override
            public long count() {
                String select = "select count(" + aliasToSelect + ") " + query;
                return XDAO.this.count(select, parameters);
            }
        };
    }

    public XSearch<T> createSearchFromHQL(final String query, final String aliasToSelect, final List<?> parameters) {
        Object[] array = new Object[parameters.size()];
        parameters.toArray(array);
        return createSearchFromHQL(query, aliasToSelect, array);
    }

}
