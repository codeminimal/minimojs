package br.com.codeminimal.minimojs;

import br.com.codeminimal.minimojs.dao.XDAO;
import br.com.codeminimal.minimojs.model.XUser;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;

import java.lang.reflect.*;
import java.util.*;

/**
 * Created by eduardo on 8/21/16.
 */
public enum XObjectsManager {

    instance;

    private Logger logger = Logger.getLogger(XScriptManager.class);

    private Map<String, Object> instancesMap;

    private Map<String, String> metaMap;

    private Map<String, Map<String, Method>> GETMethodsMap;

    private Map<String, Map<String, Method>> POSTMethodsMap;

    private List<Object[]> scheduledObjects = new ArrayList<Object[]>();

    private Map<String, Invoker> urlMethods;

    private String allMetaClasses;

    private XAppLifecycle lifecicle;

    private String masterUser;

    private String masterPassword;

    private String masterRoleName;

    public void init(Properties properties) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        instancesMap = new HashMap<String, Object>();
        metaMap = new HashMap<String, String>();
        GETMethodsMap = new HashMap<String, Map<String, Method>>();
        POSTMethodsMap = new HashMap<String, Map<String, Method>>();
        synchronized (scheduledObjects) {
            scheduledObjects.clear();
            urlMethods = new HashMap<String, Invoker>();
            Set<Class<? extends Object>> allClasses = new HashSet<Class<? extends Object>>();
            if (properties.getProperty("service.packages") != null) {
                logger.debug("service.packages " + properties.getProperty("service.packages"));
                String[] scanPackages = properties.getProperty("service.packages").split(",");
                for (String packageName : scanPackages) {
                    logger.debug("Scanning service package " + scanPackages);
                    allClasses.addAll(XClassFinder.find(packageName, XObject.class));
                }
            }
            if (properties.getProperty("objects") != null) {
                String[] objects = properties.getProperty("objects").split(",");
                for (String obj : objects) {
                    allClasses.add(Class.forName(obj));
                }
            }
            StringBuilder allMeta = new StringBuilder("{");
            logger.debug("Initializing XObjects...");
            instantiateXObject(allMeta, XUserService.class);
            for (Class<? extends Object> cl : allClasses) {
                logger.debug("Initializing XObject " + cl.getName());
                instantiateXObject(allMeta, cl);
            }
            logger.debug("Initializing XObject's DAO variables...");
            for (Object xobj : instancesMap.values()) {
                checkXRef(xobj);
            }
            allMeta.append("}");
            allMetaClasses = allMeta.toString();

        }

        if (properties.get("lifecycle.class") != null) {
            logger.debug("Starting Life cycle object");
            @SuppressWarnings("unchecked")
            Class<? extends XAppLifecycle> cl = (Class<? extends XAppLifecycle>) Class
                    .forName((String) properties.get("lifecycle.class"));
            lifecicle = (XAppLifecycle) cl.newInstance();
            configFields(cl, lifecicle);
            lifecicle.onInit();
        }

        masterRoleName = properties.getProperty("master.role");

        masterUser = properties.getProperty("master.user");

        masterPassword = properties.getProperty("master.password");

        if (masterUser != null) {
            XContext.setInUserContext(false);
            Session session = XDBManager.instance.openSession();
            XContext.setPersistenceSession(session);
            XUserService userService = (XUserService) instancesMap.get("XUserService");
            if (!userService.exists(masterUser)) {
                Transaction tx = session.beginTransaction();
                XUser user = new XUser();
                user.setLogin(masterUser);
                user.setRole(masterRoleName);
                user.setPassword(masterPassword);
                userService.create(user);
                tx.commit();
            }
            session.close();
        }
    }


    private void checkXRef(Object xobj) {
        for (Field f : xobj.getClass().getFields()) {
            checkXField(xobj, f);
        }
        for (Field f : xobj.getClass().getDeclaredFields()) {
            checkXField(xobj, f);
        }
    }


    private void checkXField(Object xobj, Field f) {
        XObject annot = f.getType().getAnnotation(XObject.class);
        if (annot != null) {
            try {
                f.setAccessible(true);
                f.set(xobj, instancesMap.get(annot.alias()));
            } catch (Exception e) {
                String msg = "Error initiating service refs in instance.";
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
    }

    private void instantiateXObject(StringBuilder allMeta, Class<? extends Object> cl)
            throws InstantiationException, IllegalAccessException {
        XObject annot = cl.getAnnotation(XObject.class);
        String alias = annot.alias();
        configMeta(alias, cl, allMeta);
        Object instance = cl.newInstance();
        configUrls(instance);
        configFields(cl, instance);
        XDBManager.instance.addScheduledObject(instance, cl);
        instancesMap.put(alias, instance);
    }

    private void configUrls(Object instance) {
        for (Method m : instance.getClass().getMethods()) {
            XMethod annot = m.getAnnotation(XMethod.class);
            if (annot != null && !annot.url().trim().equals("")) {
                if (m.getParameterTypes().length > 0) {
                    throw new RuntimeException(
                            "WEB Methods cannot have arguments. URL: " + annot.url() + ", Method: " + m);
                } else if (!m.getReturnType().equals(void.class)) {
                    throw new RuntimeException("WEB Methods must return void. URL: " + annot.url() + ", Method: " + m);
                }
                urlMethods.put(annot.url(), new Invoker(instance, m));
            }
        }
    }

    private void configFields(Class<? extends Object> cl, Object instance) {
        for (Field field : cl.getFields()) {
            checkField(field, instance);
        }
        for (Field field : cl.getDeclaredFields()) {
            checkField(field, instance);
        }
    }

    private void checkField(Field field, Object instance) {
        if (XDAO.class.isAssignableFrom(field.getType())) {
            Type c = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            @SuppressWarnings("all")
            XDAO<?> xdao = new XDAO((Class<?>) c);
            try {
                field.setAccessible(true);
                field.set(instance, xdao);
            } catch (Exception e) {
                String msg = "Error instantiating class XDAO<" + c + ">";
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
    }

    private void configMeta(String alias, Class<? extends Object> cl, StringBuilder allMeta) {
        Map<String, Object> meta = new HashMap<String, Object>();
        List<Map<String, String>> methodsList = new ArrayList<Map<String, String>>();
        Map<String, Method> GETMethods = new HashMap<String, Method>();
        Map<String, Method> POSTMethods = new HashMap<String, Method>();
        GETMethodsMap.put(alias, GETMethods);
        POSTMethodsMap.put(alias, POSTMethods);
        meta.put("methods", methodsList);
        for (Method m : cl.getMethods()) {
            XMethod annot = m.getAnnotation(XMethod.class);
            if (annot != null) {
                if (annot.url().trim().equals("")) {
                    int cache = annot.cacheExpires();
                    boolean isGET = (cache > 0 || annot.forceMethod().equals(XMethod.WEBMethod.GET)) && !annot.upload()
                            && !annot.transacted();
                    Map<String, String> info = new HashMap<String, String>();
                    methodsList.add(info);
                    info.put("name", m.getName());
                    if (isGET) {
                        info.put("type", "GET");
                        GETMethods.put(m.getName(), m);
                    } else {
                        info.put("type", "POST");
                        POSTMethods.put(m.getName(), m);
                    }
                    info.put("nocache", Boolean.toString(cache == 0));
                    info.put("responseInOutputStream", String.valueOf(annot.responseInOutputStream()));
                }
            }
        }
        String json = new Gson().toJson(meta);
        metaMap.put(alias, json);
        allMeta.append(alias).append(":").append(json).append(",");
    }

    public String getScriptMetaClasses() {
        return allMetaClasses;
    }

    public List<Object[]> getScheduledObjects() {
        return scheduledObjects;
    }

    public String getStringMetaClass(String alias) {
        return metaMap.get(alias);
    }

    public Object getManagedObject(String alias) {
        return instancesMap.get(alias);
    }

    public Invoker getGetMethod(String alias, String methodName) {
        Object obj = instancesMap.get(alias);
        if (obj != null) {
            Method m = GETMethodsMap.get(alias).get(methodName);
            if (m != null) {
                return new Invoker(obj, m);
            }
        }
        return null;
    }

    public Invoker getPostMethod(String alias, String methodName) {
        Object obj = instancesMap.get(alias);
        if (obj != null) {
            Method m = POSTMethodsMap.get(alias).get(methodName);
            if (m != null) {
                return new Invoker(obj, m);
            }
        }
        return null;
    }

    public Invoker getUrlMethod(String url) {
        return urlMethods.get(url);
    }

    public void addUrlMethod(String url, Invoker invoker) {
        urlMethods.put(url, invoker);
    }

    public int getManagedObjectsCount() {
        return instancesMap != null ? instancesMap.size() : 0;
    }

    public String getMasterRoleName() {
        return masterRoleName;
    }
}

