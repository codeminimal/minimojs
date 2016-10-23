package br.com.kodeless.minimaljs;

import java.io.Serializable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class XCache {

    private static CacheManager cacheManager;

    private static synchronized final void startCacheManager() throws XCacheException {
        if (cacheManager == null) {
            try {
                cacheManager = CacheManager.create();
            } catch (CacheException e) {
                throw new XCacheException("Error creating cache manager", e);
            }
        }
    }

    private static final Cache getCache(final String cacheName) throws XCacheException {
        startCacheManager();
        if (cacheManager.getCache(cacheName) == null) {
            Cache cache = new Cache(cacheName, 100, false, false, 600, 600, false, 0);
            try {
                cacheManager.addCache(cache);
            } catch (CacheException e) {
                throw new XCacheException("Error adding cache '" + cacheName + "' to cache manager.", e);
            }
        }

        return cacheManager.getCache(cacheName);
    }

    private static final void addCache(final Cache cache) throws XCacheException {
        try {
            cacheManager.addCache(cache);
        } catch (CacheException e) {
            throw new XCacheException("Error adding cache object to cache manager.", e);
        }
    }

    public static final void clearAll() throws XCacheException {
        try {
            for (String cacheName : cacheManager.getCacheNames()) {
                cacheManager.getCache(cacheName).removeAll();
            }
        } catch (CacheException e) {
            throw new XCacheException("Error clearing cache manager.", e);
        }
    }

    public static final Object getCaheValue(final String cacheName, final String key) throws XCacheException {
        if (getCache(cacheName).get(key) != null) {
            try {

                return getCache(cacheName).get(key).getValue();
            } catch (CacheException e) {
                throw new XCacheException("Error getting cache value of key '" + key + "' from cache '" + cacheName + "'", e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static final <T extends Serializable> T getCachedValue(final String cacheName, final String key,
                                                                  CacheAction<T> actionOnNotFound) throws XCacheException {
        T value = null;

        if (getCache(cacheName).get(key) != null) {
            value = (T) getCache(cacheName).get(key).getValue();
        } else {
            value = actionOnNotFound.execute();
            if (value != null) {
                addValueToCache(cacheName, key, value);
            }
        }
        return value;
    }

    public static final void addValueToCache(final String cacheName, final String key, final Serializable value) throws XCacheException {
        getCache(cacheName).put(new Element(key, value));
    }

    public static final void removeCaheValue(final String cacheName, final String key) throws XCacheException {
        getCache(cacheName).remove(key);
    }

    public static abstract class CacheAction<T> {
        public abstract T execute();
    }

    public static class XCacheException extends Exception {

        public XCacheException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}