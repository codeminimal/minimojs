package br.com.kodeless.minimojs;

import br.com.kodeless.minimojs.model.XUser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Created by eduardo on 8/21/16.
 */
public class Invoker {

    private final Object obj;
    private final Method method;

    Invoker(Object obj, Method m) {
        this.obj = obj;
        this.method = m;
    }

    public Object invoke(List<String> parameterList) throws InvocationTargetException, IllegalAccessException, XNotAuthenticatedException, XNotAuthException {

        boolean loginReq = method.getAnnotation(XMethod.class).loginRequired();
        XUser user = XContext.getUser();
        if (user == null && loginReq) {
            throw new XNotAuthenticatedException("The method " + method.getName() + " requires a logged user");
        }
        boolean auth = !method.getAnnotation(XMethod.class).functionAllowed().trim().equals("")
                || method.getAnnotation(XMethod.class).rolesAllowed().length > 0;
        if (loginReq && auth) {
            boolean allowed = false;
            String userRole = user.getRole();
            if (userRole != null && !userRole.trim().equals("")) {
                if (userRole.equals(XObjectsManager.instance.getMasterRoleName())
                        || (method.getAnnotation(XMethod.class).rolesAllowed() != null
                        && method.getAnnotation(XMethod.class).rolesAllowed().length > 0
                        && Arrays.binarySearch(method.getAnnotation(XMethod.class).rolesAllowed(),
                        userRole) >= 0)) {
                    allowed = true;
                }
            }
            if (!allowed) {
                String functionAllowed = method.getAnnotation(XMethod.class).functionAllowed();
                if (functionAllowed != null && !functionAllowed.trim().equals("")
                        && user.getAvailableFunctions() != null && user.getAvailableFunctions().size() > 0
                        && user.getAvailableFunctions().contains(functionAllowed)) {
                    allowed = true;
                }
            }
            if (!allowed) {
                throw new XNotAuthException("User not authorized to execute the operation");
            }
        }

        int count = 0;
        Object[] parameters = new Object[method.getParameterTypes().length];
        for (String param : parameterList) {
            Class<?> cl;
            cl = method.getParameterTypes()[count];
            parameters[count] = XJson.parse(param, cl);
            count++;
        }
        return method.invoke(obj, parameters);

    }

    public Method getMethod() {
        return method;
    }

    public Object getInstance() {
        return obj;
    }
}
