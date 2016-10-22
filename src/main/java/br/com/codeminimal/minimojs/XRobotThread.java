package br.com.codeminimal.minimojs;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;

import br.com.codeminimal.minimojs.dao.XDAO;
import br.com.codeminimal.minimojs.model.internal.XSchedule;
import br.com.codeminimal.minimojs.model.internal.XScheduledExecution;

class XRobotThread {

    private static final Logger logger = Logger.getLogger(XRobotThread.class);

    XDAO<XSchedule> schedDAO = new XDAO<XSchedule>(XSchedule.class);

    XDAO<XScheduledExecution> execDAO = new XDAO<XScheduledExecution>(XScheduledExecution.class);

    DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    long minute = 1000 * 60;

    long hour = 1000 * 60 * 60;

    long day = hour * 24;

    long week = day * 7;

    long month = day * 30;

    long year = day * 365;

    private List<Object[]> scheduledObjects;

    private Map<String, Object[]> scheduleObjectsMap = new HashMap<String, Object[]>();

    private SessionFactory sessions;

    private Session session;

    XRobotThread(List<Object[]> scheduledObjects, SessionFactory sessions) {
        if (scheduledObjects.size() > 0 && sessions != null) {

            for (Object[] objects : scheduledObjects) {
                Method method = (Method) objects[1];
                XRobot robot = method.getAnnotation(XRobot.class);
                scheduleObjectsMap.put(robot.name(), objects);
            }

            this.scheduledObjects = scheduledObjects;
            this.sessions = sessions;
            this.session = sessions.openSession();
        }
    }

    public void start() {
        if (scheduledObjects != null && scheduledObjects.size() > 0 && sessions != null) {
            this.threadCron.setDaemon(true);
            this.threadCron.start();
            this.threadExec.setDaemon(true);
            this.threadExec.start();
        }
    }

    Thread threadCron = new Thread() {
        public void run() {
            XContext.setPersistenceSession(session);
            XContext.setInUserContext(false);
            try {
                Thread.sleep(1000 * 60 * 1);
            } catch (InterruptedException e) {
            }
            while (true) {

                synchronized (scheduledObjects) {
                    for (Object[] objects : scheduledObjects) {
                        Object instance = objects[0];
                        Method method = (Method) objects[1];
                        XRobot robot = method.getAnnotation(XRobot.class);

                        long now = System.currentTimeMillis();
                        Transaction tx = null;
                        boolean transacted = method.getAnnotation(XMethod.class) != null
                                && method.getAnnotation(XMethod.class).transacted();
                        boolean commited = false;
                        try {
                            XSchedule schedule = schedDAO.uniqueBy("scheduleName", robot.name());
                            if (schedule == null) {
                                schedule = new XSchedule();
                                schedule.setLastExecution(0l);
                                schedule.setScheduleName(robot.name());
                            }
                            long lastExecution = schedule.getLastExecution();
                            long totalMiliSec = now - lastExecution;
                            boolean execute = false;
                            if (robot.scheduleType().equals(XRobot.ScheculeType.HOURLY)) {
                                execute = true;
                            } else if (robot.scheduleType().equals(XRobot.ScheculeType.DAILY) && totalMiliSec >= day) {
                                execute = true;
                            } else if (robot.scheduleType().equals(XRobot.ScheculeType.WEEKLY) && totalMiliSec >= week) {
                                execute = true;
                            } else if (robot.scheduleType().equals(XRobot.ScheculeType.MONTHLY)
                                    && totalMiliSec >= month) {
                                execute = true;
                            } else if (robot.scheduleType().equals(XRobot.ScheculeType.YEARLY) && totalMiliSec >= year) {
                                execute = true;
                            }
                            if (execute) {
                                if (transacted) {
                                    tx = session.beginTransaction();
                                }
                                method.invoke(instance, new Object[0]);
                                schedule.setLastExecution(now);
                                schedDAO.saveOrUpdate(schedule);
                                if (tx != null && transacted) {
                                    tx.commit();
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error executing robot (schedule name: " + robot.name()
                                    + ", executiong date: " + df.format(new Date(now)) + ").", e);
                        } finally {
                            if (session != null) {
                                if (tx != null && tx.isActive() && !commited) {
                                    tx.rollback();
                                }
                                session.close();
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(hour);
                } catch (InterruptedException e) {
                    logger.error("Error during sleep.", e);
                }
            }

        }
    };

    Thread threadExec = new Thread() {
        public void run() {
            XContext.setPersistenceSession(session);
            XContext.setInUserContext(false);
            try {
                Thread.sleep(1000 * 60 * 1);
            } catch (InterruptedException e) {
            }
            while (true) {
                Transaction tx = null;
                boolean commited = false;
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.MILLISECOND, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.add(Calendar.MINUTE, 1);
                    List<XScheduledExecution> list = execDAO
                            .find("select * from XScheduledExecution x where x.executionDate >= ? and x.executionDate < ? and x.executed = false",
                                    cal.getTime());
                    long now = System.currentTimeMillis();
                    for (XScheduledExecution exec : list) {
                        exec.setExecuted(true);
                        Object[] objects = scheduleObjectsMap.get(exec.getScheduleName());
                        Object instance = objects[0];
                        Method method = (Method) objects[1];
                        boolean transacted = method.getAnnotation(XMethod.class) != null
                                && method.getAnnotation(XMethod.class).transacted();
                        commited = false;
                        if (transacted) {
                            tx = session.beginTransaction();
                        }
                        method.invoke(instance, new Object[0]);
                        exec.setRealExecutionDate(new Date());
                        execDAO.saveOrUpdate(exec);
                    }
                } catch (Exception e) {
                    logger.error("Error executing robot (schedule name: , executiong date: " + df.format(new Date())
                            + ").", e);
                } finally {
                    if (session != null) {
                        if (tx != null && tx.isActive() && !commited) {
                            tx.rollback();
                        }
                        session.close();
                    }
                }

                try {
                    Thread.sleep(minute);
                } catch (InterruptedException e) {
                    logger.error("Error during sleep.", e);
                }
            }

        }
    };

}
