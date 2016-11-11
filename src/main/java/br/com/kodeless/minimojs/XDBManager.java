package br.com.kodeless.minimojs;

import br.com.kodeless.minimojs.model.internal.XSchedule;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;

import javax.persistence.Entity;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

/**
 * Created by eduardo on 8/21/16.
 */
public enum XDBManager {
    instance;

    private Logger logger = Logger.getLogger(XDBManager.class);

    private SessionFactory sessions;

    private XRobotThread robot;

    private List<Object[]> scheduledObjects;

    public void init(Properties properties, List<Object[]> scheduledObjects) {
        String jndiDataSource = properties.getProperty("data.source");
        this.scheduledObjects = scheduledObjects;
        if (jndiDataSource != null) {

            // InitialContext context = new InitialContext();
            // dataSource = (DataSource) context.lookup("java:comp/env"
            // + (jndiDataSource.charAt(0) == '/' ? jndiDataSource : ("/" +
            // jndiDataSource)));

            Configuration configuration = new Configuration();

            configuration.setProperty("hibernate.connection.datasource", jndiDataSource);
            configuration.setProperty("hibernate.hbm2ddl.auto", properties.getProperty("hibernate.hbm2ddl.auto", "update"));
            configuration.setProperty("hibernate.ejb.naming_strategy",
                    "org.hibernate.cfg.DefaultComponentSafeNamingStrategy");
            if (XContext.isDevMode()) {
                configuration.setProperty("show_sql", "true");
            }
            //configuring hibernate to find the entity classes
            configuration.addAnnotatedClass(XSchedule.class);
            String[] packages = properties.getProperty("entity.packages").split(",");
            for (String packageName : packages) {
                List<Class<?>> classes = XClassFinder.find(packageName, Entity.class);
                for (Class<?> clazz : classes) {
                    configuration.addAnnotatedClass(clazz);
                }
            }
            configuration.configure();
            logger.debug("Building hibernate session...");
            sessions = configuration.buildSessionFactory();

            //start robot thread
            robot = new XRobotThread(scheduledObjects, XDBManager.instance.getSessionFactory());
            robot.start();

        }

    }

    public void addScheduledObject(Object instance, Class<? extends Object> cl) {
        for (Method m : cl.getMethods()) {
            XRobot robot = m.getAnnotation(XRobot.class);
            if (robot != null) {
                if (m.getParameterTypes().length > 0) {
                    throw new RuntimeException("XRobot methods must have no parameters! Method: " + m);
                }
                scheduledObjects.add(new Object[]{instance, m});
            }
        }
    }


    public SessionFactory getSessionFactory() {
        return sessions;
    }

    public Session openSession() {
        return getSessionFactory().openSession();
    }

    public boolean isConfigured() {
        return sessions != null;
    }

}
