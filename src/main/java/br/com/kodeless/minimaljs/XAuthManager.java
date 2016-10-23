package br.com.kodeless.minimaljs;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Created by eduardo on 8/21/16.
 */
public enum XAuthManager {
    instance;

    private ServletContext ctx;

    public void init(ServletContext ctx) {
        this.ctx = ctx;
    }

    public AuthProperties getAuthProperties(String pathInfo) throws IOException {
        int dotIndex = pathInfo.lastIndexOf('.');
        int barIndex = pathInfo.lastIndexOf('/');
        String propPath;
        if (dotIndex > barIndex) {
            propPath = pathInfo.substring(0, dotIndex);
        } else {
            propPath = pathInfo;
        }
        byte[] bytes = XFileUtil.instance.readFromDisk(
                "/pages" + propPath + (propPath.lastIndexOf('/') == 0 ? "/index" : "") + ".auth", null,
                this.ctx);
        if (bytes == null) {
            bytes = XFileUtil.instance.readFromDisk("/pages" + propPath.substring(0, propPath.lastIndexOf('/')) + "/auth",
                    null, this.ctx);
        }
        AuthProperties result = null;
        if (bytes != null) {
            result = new AuthProperties();
            StringReader reader = new StringReader(new String(bytes));
            Properties p = new Properties();
            p.load(reader);
            String roles = (String) p.get("roles");
            if (roles != null) {
                String[] rolesArray = roles.split(",");
                for (int i = 0; i < rolesArray.length; i++) {
                    rolesArray[i] = rolesArray[i].trim();
                }
                result.setAllowedRoles(rolesArray);
            }
            String function = (String) p.get("function");
            if (function != null) {
                result.setAllowedFunction(function);
            }
            String authentication = (String) p.get("authentication");
            result.setNeedsAuthentication(authentication == null || authentication.equalsIgnoreCase("TRUE"));
        }
        return result;
    }

    public static class AuthProperties {
        private String[] allowedRoles;
        private String allowedFunction;
        private boolean needsAuthentication;

        public String[] getAllowedRoles() {
            return allowedRoles;
        }

        public void setAllowedRoles(String[] allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        public String getAllowedFunction() {
            return allowedFunction;
        }

        public void setAllowedFunction(String allowedFunction) {
            this.allowedFunction = allowedFunction;
        }

        public Boolean getNeedsAuthentication() {
            return needsAuthentication;
        }

        public void setNeedsAuthentication(Boolean needsAuthentication) {
            this.needsAuthentication = needsAuthentication;
        }
    }
}
