package br.com.codeminimal.minimojs;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;

import br.com.codeminimal.minimojs.model.XUser;
import br.com.codeminimal.minimojs.parser.XHTMLParsingException;

public class XServlet extends HttpServlet {

    private static final long serialVersionUID = 6340280941961523359L;

    private static final Logger logger = Logger.getLogger(XServlet.class);

    private int maxUploadSize = 10000000;

    private String defaultTemplateName;

    private String welcomePage;

    private byte[] customLoader;

    private String esprima;

    private Properties properties;

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {

            logger.info("Initializing XServlet..");
            properties = new Properties();
            logger.debug("Loading properties");
            properties.load(config.getServletContext().getResourceAsStream("/WEB-INF/x.properties"));
            XAuthManager.instance.init(config.getServletContext());
            logger.debug("Starting label service");
            XLabels.start(config.getServletContext());

            load(config.getServletContext());

            logger.debug("Initializing Hibernate Session, data source: " + properties.getProperty("data.source"));
            XDBManager.instance.init(properties, XObjectsManager.instance.getScheduledObjects());

            super.init(config);
            logger.info("XServlet Initialized");
        } catch (Exception e) {
            String msg = "Error x classes.";
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private void load(ServletContext ctx)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, XHTMLParsingException {

        String devmode = System.getenv("XDEVMODE");
        if (devmode == null) {
            devmode = properties.getProperty("devmode");
        }
        XContext.setDevMode(devmode != null && devmode.equalsIgnoreCase("true"));

        String maxUploadSizeStr = properties.getProperty("max.upload.size");
        if (maxUploadSizeStr != null) {
            maxUploadSize = Integer.parseInt(maxUploadSizeStr);
        }
        logger.debug("max.upload.size=" + maxUploadSizeStr);

        XComponents.loadComponents(ctx);

        XResourceManager.instance.init(ctx, properties);

        String jsonResourceInfo = new Gson().toJson(XResourceManager.instance.getImportableResourceInfo());

        XObjectsManager.instance.init(properties);
        //create x script
        XScriptManager.instance.reload(ctx, properties, XObjectsManager.instance.getScriptMetaClasses(), jsonResourceInfo);

        esprima = XFileUtil.instance.getResource("/esprima.js");

        logger.debug("Initializing Loader..");
        customLoader = XTemplates.loaderImg(properties.getProperty("loader.img.path"), ctx);

        defaultTemplateName = properties.getProperty("default.page.template");

        welcomePage = properties.getProperty("welcome.page");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        XRequest req = new XRequest(request);
        XResponse resp = new XResponse(response);
        updateContext(req, resp);
        String serv_path = req.getServletPath() + req.getPathInfo();
        String[] path = (serv_path).split("/");
        if (path.length > 1 && path[1].equals("xurl")) {
            // webmethods
            try {
                executeWebMethod(serv_path.substring("/xurl".length()), req, resp);
            } catch (Exception e) {
                String msg = "Error invoking web method.";
                logger.error(msg, e);
                throw new ServletException(msg, e);
            }
        } else if (path.length > 1 && path[1].equals("x")) {
            // aux methods
            if (path[2].equals("_reload")) {
                // reload app in dev mode
                if (XContext.isDevMode()) {
                    try {
                        load(req.getSession().getServletContext());
                    } catch (Exception e) {
                        throw new ServletException("Error reloading context", e);
                    }
                }
            } else if (path[2].equals("unauthorized")) {
                error(resp, response.getOutputStream(), HttpServletResponse.SC_FORBIDDEN);
            } else if (path[2].equals("_appcache")) {
                // status
                response.setContentType("text/cache-manifest");
                response.getWriter().write(XResourceManager.instance.getAppCache());
            } else if (path[2].equals("_status")) {
                // status
                printStatus(resp);
            } else if (path[2].equals("_x_ping")) {
                // pint
                logger.debug("ping");
                resp.getOutputStream().write("''".getBytes());
            } else if (path[2].equals("no_authentication")) {
                // forbidden
                error(resp, resp.getOutputStream(), HttpServletResponse.SC_FORBIDDEN);
            } else if (path[2].equals("loader.gif")) {
                // get loader img
                XHttp.setCachedResponseHeader(604800);
                resp.setContentType("image/gif");
                resp.getOutputStream().write(customLoader);
            } else if (path[2].equals("__meta")) {
                // get meta methods
                String alias = path[3];
                printResponse(resp, XObjectsManager.instance.getStringMetaClass(alias));
            } else if (path[2].equals("scripts") && path[3].equals("x.js")) {
                // get x.js
                XHttp.setCachedResponseHeader(1296000);
                resp.setContentType("text/javascript");
                resp.setCharacterEncoding("utf-8");
                printResponse(resp, XScriptManager.instance.getScript());
            } else if (path[2].equals("scripts") && path[3].equals("x.remote.js")) {
                // cross?
                XHttp.setCachedResponseHeader(1296000);
                resp.setContentType("text/javascript");
                resp.setCharacterEncoding("utf-8");
                printResponse(resp, XScriptManager.instance.getRemoteScript());
            } else if (path[2].equals("scripts") && path[3].equals("esprima.js")) {
                // esprima
                XHttp.setCachedResponseHeader(31296000);
                resp.setContentType("text/javascript");
                resp.setCharacterEncoding("utf-8");
                printResponse(resp, esprima);
            } else {
                // remote method call
                try {
                    Invoker invoker = XObjectsManager.instance.getGetMethod(path[2], path[3]);
                    if (invoker == null) {
                        throw new RuntimeException("Invalid method " + path[3] + " or alias " + path[2]);
                    }
                    invoke(req, resp, invoker, false, false);
                } catch (Exception e) {

                    if (e.getCause() instanceof XNotAuthException) {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        throw new ServletException(e);
                    }
                }
            }
        } else {
            // get resource
            String pathInfo = req.getPathInfo();
            if ((pathInfo.equals("/") || pathInfo.equals("")) && !welcomePage.equals("") && !welcomePage.equals("/")) {
                // welcome
                resp.sendRedirect(req.getContextPath() + welcomePage);
                return;
            }
            OutputStream os = resp.getOutputStream();
            if (XResourceManager.instance.isStaticResource(pathInfo)) {
                // simple resource
                setContentType(resp, pathInfo);
                XHttp.setCachedResponseHeader(1296000);
                byte[] page = XFileUtil.instance.readFromDisk(pathInfo.startsWith("/res") ? pathInfo : "/res" + pathInfo, null, this.getServletContext());
                if (page != null) {
                    os.write(page);
                } else {
                    error(resp, os, HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                // x resource
                try {
                    sendToPage(req, resp, pathInfo, os);
                } catch (XHTMLParsingException e) {
                    throw new ServletException("Error parsing html page", e);
                }
            }
            os.flush();
        }
        clearContext();
    }

    private void sendToPage(HttpServletRequest req, HttpServletResponse resp, String path, OutputStream os)
            throws IOException, XHTMLParsingException {
        XUser user = (XUser) req.getSession().getAttribute("__x_user");
        XResourceManager.Resource resInfo = XResourceManager.instance.getResourceInfo(path);
        if (resInfo == null) {
            error(resp, os, HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (resInfo.isImplicit()) {
            // is dir. Checking possibility to redirect
            resp.sendRedirect(user != null && resInfo.getLoggedRedirect() != null ? resInfo.getLoggedRedirect() : resInfo.getUnloggedRedirect());
            return;
        }

        if (resInfo instanceof XResourceManager.HtmxResource) {
            resp.setHeader("Content-Type", "text/html; charset=UTF-8");
        } else {
            resp.setHeader("Content-Type", "text/javascript; charset=UTF-8");
        }
        resp.setCharacterEncoding("utf-8");

        byte[] page = XResourceManager.instance.getPageContents(resInfo);
        os.write(page);

    }

    private void xparameters(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/json");
        resp.setCharacterEncoding("utf-8");

        StringBuilder scripts = new StringBuilder();

        if (req.getSession().getAttribute("__x_user") != null) {
            scripts.append(XJson.toJson(req.getSession().getAttribute("__x_user")));
        } else {
            scripts.append("null");
        }

        printResponse(resp, scripts.toString());
    }

    private Map<String, String> getParametersFromReferer(String referer) throws UnsupportedEncodingException {
        int index = referer.indexOf('?');
        Map<String, String> result = new HashMap<String, String>();
        if (index >= 0) {
            String[] parameters = referer.substring(index + 1).split("&");
            for (String param : parameters) {
                String[] paramSplitted = param.split("=");
                if (!paramSplitted[0].trim().equals("")) {
                    result.put(paramSplitted[0],
                            paramSplitted.length > 1 ? URLDecoder.decode(paramSplitted[1], "utf8") : "");
                }
            }
        }
        return result;
    }

    private void executeWebMethod(String url, XRequest req, XResponse resp) throws Exception {
        Invoker invoker = XObjectsManager.instance.getUrlMethod(url);
        String tempUrl = url;
        while (invoker == null && tempUrl.length() > 0 && tempUrl.indexOf('/') >= 0) {
            tempUrl = tempUrl.substring(tempUrl.lastIndexOf('/')) + "/*";
            invoker = XObjectsManager.instance.getUrlMethod(tempUrl);
            if (invoker != null) {
                XObjectsManager.instance.addUrlMethod(url, invoker);
            }
        }
        if (invoker == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            invoke(req, resp, invoker, false, true);
        }
    }

    private void printStatus(HttpServletResponse resp) throws IOException {
        PrintWriter w = resp.getWriter();
        w.print("<html><head><title>status</title></head><body><h1>Status</h1><br>Qtd Instances: ");
        w.print(XObjectsManager.instance.getManagedObjectsCount());
        w.print("<br>DevMode: ");
        w.print(XContext.isDevMode());
        w.print("<br></body></html>");
    }

    private void error(HttpServletResponse resp, OutputStream os, int errorCode) throws IOException {
        resp.setStatus(errorCode);
        os.write(XFileUtil.instance.readFromDisk("error-pages/" + errorCode + ".html", "/errorpages/" + errorCode + ".html",
                this.getServletContext()));
    }

    private void setContentType(HttpServletResponse resp, String pathInfo) {
        if (pathInfo.endsWith(".js")) {
            resp.setContentType("text/javascript");
            resp.setCharacterEncoding("utf-8");
        } else if (pathInfo.endsWith(".html")) {
            resp.setContentType("text/html");
            resp.setCharacterEncoding("utf-8");
        } else if (pathInfo.endsWith(".css")) {
            resp.setContentType("text/css");
            resp.setCharacterEncoding("utf-8");
        } else if (pathInfo.endsWith(".jpg")) {
            resp.setContentType("text/JPEG");
        } else if (pathInfo.endsWith(".jpeg")) {
            resp.setContentType("text/JPEG");
        } else if (pathInfo.endsWith(".gif")) {
            resp.setContentType("text/gif");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String serv_path = request.getServletPath() + request.getPathInfo();
        String[] path = (serv_path).split("/");
        if (path[2].equals("_xprms")) {
            // session objects in browser
            // in the future, when all comunications were through websocket, this will be the first information posted by the server
            xparameters(request, response);
            response.setContentType("text/json");
            response.setCharacterEncoding("utf-8");
            return;
        }
        XRequest req = new XRequest(request);
        XResponse resp = new XResponse(response);
        updateContext(req, resp);
        boolean isUpload = ServletFileUpload.isMultipartContent(req);
        if (isUpload) {
            ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
            upload.setSizeMax(maxUploadSize);

            List<FileItem> fileItems;
            try {
                fileItems = upload.parseRequest(req);
            } catch (FileUploadException e) {
                String msg = "Upload error";
                logger.error(msg, e);
                throw new ServletException(msg, e);
            }
            for (FileItem fi : fileItems) {
                if (!fi.isFormField()) {
                    XFile file = new XFile();
                    file.setFieldName(fi.getFieldName());
                    String fileName = fi.getName();
                    if (fileName.lastIndexOf("\\") >= 0) {
                        file.setFileName(fileName.substring(fileName.lastIndexOf("\\")));
                    } else {
                        file.setFileName(fileName.substring(fileName.lastIndexOf("\\") + 1));
                    }
                    file.setContentType(fi.getContentType());
                    file.setInMemory(fi.isInMemory());
                    file.setSizeInBytes(fi.getSize());

                    file.setData(XStreamUtil.inputStreamToByteArray(fi.getInputStream()));
                    XContext.setFileUpload(file);
                }
            }
        }
        if (path[1].equals("x")) {
            try {
                Invoker invoker = XObjectsManager.instance.getPostMethod(path[2], path[3]);
                if (invoker == null) {
                    throw new RuntimeException("Invalid method " + path[3] + " or alias " + path[2]);
                }
                invoke(req, resp, invoker, isUpload, false);
            } catch (Exception e) {
                throw new ServletException(e);
            }
        } else {
            req.getRequestDispatcher(req.getPathInfo());
        }
        clearContext();
    }

    private void updateContext(HttpServletRequest req, HttpServletResponse resp) {
        XContext.setXRequest(req);
        XContext.setXResponse(resp);
        XContext.setXSession(req.getSession());
    }

    private void clearContext() {
        XContext.setFileUpload(null);
        XContext.setUseWebObjects(false);
        XContext.setXRequest(null);
        XContext.setXResponse(null);
        XContext.setXSession(null);
    }

    private void invoke(XRequest req, XResponse resp, Invoker invoker, boolean isUpload,
                        boolean isWebMethod) throws Exception, IOException {
        Session session = null;
        XContext.setUseWebObjects(invoker.getMethod().getAnnotation(XMethod.class).responseInOutputStream()
                || invoker.getMethod().getAnnotation(XMethod.class).useWebObjects() || invoker.getMethod().getAnnotation(XMethod.class).upload()
                || !invoker.getMethod().getAnnotation(XMethod.class).url().trim().equals(""));
        Transaction tx = null;
        boolean commited = false;
        try {

            if (XDBManager.instance.isConfigured()) {
                try {
                    session = XDBManager.instance.openSession();
                    XContext.setPersistenceSession(session);
                    if (invoker.getMethod().getAnnotation(XMethod.class).transacted()) {
                        XContext.setInTransaction(true);
                        tx = session.beginTransaction();
                    }
                } catch (NullPointerException e) {
                    throw new RuntimeException("Data source not configured", e);
                }
            }

            int cacheExpiers = invoker.getMethod().getAnnotation(XMethod.class).cacheExpires();
            if (cacheExpiers > 0) {
                XHttp.setCachedResponseHeader(cacheExpiers);
            } else {
                XHttp.setAvoidCache();
            }
            List<String> stringParams = new ArrayList<String>();
            String param;
            int count = 0;
            while ((param = req.getParameter("_param" + count)) != null) {
                stringParams.add(param);
                count++;
            }
            try {
                Object result = invoker.invoke(stringParams);

                if (!isWebMethod && !resp.isOutputUsed()) {
                    // int timezoneOffset =
                    // Integer.parseInt(req.getParameter("_tz"));
                    XJsonDiscard jsonDiscardAnnot = invoker.getMethod().getAnnotation(XJsonDiscard.class);
                    String[] ignoreFieldsPath = jsonDiscardAnnot != null ? jsonDiscardAnnot.value() : null;
                    String resultStr = "{__response:true, result: " + XJson.toJson(result, ignoreFieldsPath) + "}";
                    if (isUpload) {
                        resultStr = "<html><script>parent.X._uploadResponse(\"" + resultStr.replaceAll("\"", "\\\\\"")
                                + "\");</script></html>";
                        resp.setContentType("text/html");
                    } else {
                        resp.setContentType("text/html");
                    }
                    resp.setCharacterEncoding("utf-8");
                    printResponse(resp, resultStr);
                }
                if (tx != null) {
                    tx.commit();
                    commited = true;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                Throwable exc = e.getTargetException();
                logger.error("Error invoking method.", exc);
                String exceptionName = exc.getClass().getName();
                String message = exc.getMessage();
                String resultStr = "{__error:true, exceptionName:'" + exceptionName + "', message:'" + message + "'}";
                if (isUpload) {
                    resultStr = "<html><script>parent.X._uploadResponse(\"" + resultStr.replaceAll("\"", "\\\\\"")
                            + "\");</script></html>";
                }
                resp.setContentType("text/html");
                resp.setCharacterEncoding("utf-8");
                printResponse(resp, resultStr);

            }
        } catch (XNotAuthenticatedException e) {
            String exceptionName = e.getClass().getName();
            String message = e.getMessage();
            String resultStr = "{__error:true, __not_authenticated: true, exceptionName:'" + exceptionName
                    + "', message:'" + message + "'}";
            if (isUpload) {
                resp.setContentType("application/json");
                resp.setCharacterEncoding("utf-8");
                resultStr = "<html><script>parent.X._uploadResponse(\"" + resultStr.replaceAll("\"", "\\\\\"")
                        + "\");</script></html>";
            } else {
                resp.setContentType("text/html");
                resp.setCharacterEncoding("utf-8");
            }
            printResponse(resp, resultStr);
        } catch (Throwable t) {
            throw new Exception("Unexpected error", t);
        } finally {
            if (session != null) {
                if (!commited && tx != null) {
                    tx.rollback();
                }
                session.close();
            }
        }

    }

    private void printResponse(HttpServletResponse response, String str) throws IOException {
        try {
            PrintWriter writer = response.getWriter();
            writer.print(str);
        } catch (EOFException e) {
        }
    }
}
