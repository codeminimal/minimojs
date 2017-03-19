package br.com.kodeless.minimojs;

import br.com.kodeless.minimojs.parser.*;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.script.ScriptException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Created by eduardo on 8/21/16.
 */
public enum XResourceManager {

    instance;

    private static final Logger logger = Logger.getLogger(XResourceManager.class);

    private Map<String, byte[]> pages = new HashMap<String, byte[]>();

    private Map<String, Resource> resourceInfoMap = new HashMap<String, Resource>();

    private Map<String, Boolean> dirs = new HashMap<String, Boolean>();

    private Map<String, ImportableResourceInfo> importableResourceInfo = new HashMap<String, ImportableResourceInfo>();

    private String defaultTemplateName;

    private boolean isConfiguredToBeSpa;

    private Map<String, List<String>> modalPathsDeclared;

    private Map<String, List<String>> validResources;

    private Set<String> importableScripts;

    private String basePagesPath;

    private String baseResPath;

    private String appCacheFile;

    private Map<String, XHTMLDocument> templateMap;

    private Set<String> allResources;

    private String baseDestPath;

    public void init(String baseDestPath) throws IOException, XHTMLParsingException {
        this.baseDestPath = baseDestPath;
        //default is true
        isConfiguredToBeSpa = !("false".equalsIgnoreCase(X.getProperty("spa")));
        if (logger.isDebugEnabled()) {
            logger.debug("SPA: " + isConfiguredToBeSpa);
        }
        defaultTemplateName = X.getProperty("default.page.template");
        basePagesPath = X.getRealPath("/pages");
        baseResPath = X.getRealPath("/res");
        reload();
    }

    private void loadPageAndCache(Resource resInfo) throws IOException, XHTMLParsingException {
        byte[] bytes = loadPage(resInfo);
        if (X.isRunningInServletContainer()) {
            pages.put(getResInfoKey(resInfo), bytes);
        } else {
            XFileUtil.instance.writeFile(baseDestPath + resInfo.getPath() + (resInfo instanceof HtmxResource ? ".html" : ""), bytes);
        }
    }

    //reload page, scripts and resource info
    public synchronized void reload() throws IOException {
        modalPathsDeclared = new HashMap<String, List<String>>();
        validResources = new HashMap<String, List<String>>();
        importableScripts = new HashSet<String>();
        templateMap = new HashMap<String, XHTMLDocument>();
        if (!X.isRunningInServletContainer()) {
            //copy all res to dest
            FileUtils.copyDirectory(new File(baseResPath), new File(baseDestPath));
            //make a copy to res as well. It is accessible through both paths
            FileUtils.copyDirectory(new File(baseResPath), new File(baseDestPath + "/res"));
        }
        reloadHtmxFiles();
        reloadJsFiles();
        reloadGlobalImported();
        generateAppCacheFile();
        startWatchService();
        allResources = new HashSet<String>();
        collectAllResources();
        reloadCommonResources();
    }

    private void collectAllResources() {
        File resDir = new File(baseResPath);
        List<File> list = getAllFiles(resDir);
        for (File file : list) {
            String path = file.getAbsolutePath().substring(resDir.getAbsolutePath().length());
            allResources.add(path);
            allResources.add("/res" + path);
        }
    }

    public boolean isStaticResource(String path) {
        return allResources.contains(path);
    }

    private List<File> getAllFiles(File dir) {
        return getFiles(dir, true);
    }

    private List<File> getFiles(File dir, boolean addOnlyFiles) {
        List<File> result = new ArrayList<File>();
        File[] fileArray = dir.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                if (file.isDirectory()) {
                    result.addAll(getFiles(file, addOnlyFiles));
                    if (!addOnlyFiles) {
                        result.add(file);
                    }
                }
                if (addOnlyFiles && !file.isDirectory()) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    //watch changes in pages folder
    private void startWatchService() throws IOException {
        final WatchService watcher = FileSystems.getDefault().newWatchService();
        Map<WatchKey, Path> keys = new HashMap<WatchKey, Path>();
        List<File> files = getFiles(new File(this.basePagesPath), false);

        for (File file : files) {
            Path dir = Paths.get(file.getAbsolutePath());
            WatchKey key;
            if (XContext.isDevMode()) {
                key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            } else {
                key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            }
            keys.put(key, dir);
        }

        Thread watcherThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    WatchKey key = null;
                    try {
                        // wait for a key to be available
                        key = watcher.take();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                        continue;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        // get event type
                        WatchEvent.Kind<?> kind = event.kind();
                        Path path = keys.get(key);
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileName = ev.context();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        } else if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            logger.info("The file " + fileName + " was changed. Reloading...");
                            String fileNameString = path.toString() + "/" + fileName.toString();
                            File file = new File(fileNameString);
                            if (!file.isDirectory()) {
                                try {
                                    File htmxFile = file;
                                    if (fileNameString.endsWith(".js")) {
                                        htmxFile = new File(fileNameString.replaceAll("\\.js$", ".htmx"));
                                    }
                                    if (htmxFile.exists()) {
                                        reloadHtmxFiles(htmxFile);
                                    } else if (fileNameString.endsWith(".js")) {
                                        reloadJs(file);
                                        reloadGlobalJs(file.getAbsolutePath().substring(basePagesPath.length()));
                                    }
                                    //regenerate app cache
                                    generateAppCacheFile();
                                    reloadCommonResources();
                                } catch (Exception e) {
                                    logger.error("ERROR loading resource " + fileNameString, e);
                                }
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {

                            // TODO this process of reloading must be improved
                        }
                    }
                    // IMPORTANT: The key must be reset after processed
                    key.reset();
                }
            }
        };
        watcherThread.setDaemon(true);
        watcherThread.start();


    }

    private void reloadCommonResources() {
        String jsonResourceInfo = new Gson().toJson(XResourceManager.instance.getImportableResourceInfo());
        try {
            XScriptManager.instance.reload(XObjectsManager.instance.getScriptMetaClasses(), jsonResourceInfo);
        } catch (IOException e) {
            throw new RuntimeException("Error reloading x.js", e);
        }
        try {
            XFileUtil.instance.writeFile(baseDestPath + "/x/scripts/x.js", XScriptManager.instance.getScript().getBytes());
            XFileUtil.instance.writeFile(baseDestPath + "/x/loader.gif", XTemplates.loaderImg(X.getProperty("loader.img.path")));
            XFileUtil.instance.writeFile(baseDestPath + "/x/_appcache", XResourceManager.instance.getAppCache().getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error writing scripts to dest folder", e);
        }
    }

    private void generateAppCacheFile() {
        StringBuilder content = new StringBuilder();
        content.append("CACHE MANIFEST\n");
        content.append("# ").append(System.currentTimeMillis()).append("\n");
        content.append("# This file is automatically generated\n");
        content.append("CACHE:\n");
        content.append(X.getContextPath() + "/x/loader.gif\n");
        content.append(X.getContextPath() + "/x/scripts/x.js\n");
        for (String resource : validResources.keySet()) {
            content.append(X.getContextPath() + resource).append("\n");
        }
        content.append("NETWORK:\n");
        content.append("*\n");
        appCacheFile = content.toString();
        logger.debug("Generated app cache:");
    }

    private void reloadGlobalImported() throws IOException {
        Map<String, List<String>> resourceMap = new HashMap<String, List<String>>(validResources);
        for (String resource : resourceMap.keySet()) {
            int indexOfQM = resource.indexOf("?");
            if (indexOfQM < 0) {
                if (importableScripts.contains(resource)) {
                    logger.info("Loading js for global import " + resource);
                    reloadGlobalJs(resource);
                }
            }
        }
    }

    //pre loads all that doesn't have a htmx pair
    private void reloadJsFiles() throws IOException {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".js");
            }
        };
        List<File> jsList = XFileUtil.instance.listFiles("/pages", filter);
        for (File jsFile : jsList) {
            reloadJs(jsFile);
        }
    }

    private void addPath(Map<String, List<String>> map, String path, String owner) {
        List<String> list = map.get(path);
        if (owner != null) {
            if (list == null) {
                list = new ArrayList<String>();
            }
            list.add(owner);
        }
        map.put(path, list);
    }

    private void reloadGlobalJs(String resource) throws IOException {
        File jsFile = new File(basePagesPath + "/pages" + resource);

        addPath(validResources, resource, null);

        JsResource resInfo = (JsResource) getResourceInfo(resource);
        try {
            //load it as global
            loadPageAndCache(resInfo);
        } catch (XHTMLParsingException e) {
            logger.warn("Path " + resource + " has an invalid htmx file. ", e);
        } catch (Exception e) {
            logger.warn("Error loading " + resource + ".", e);
        }
        ImportableResourceInfo importable = new ImportableResourceInfo();
        importable.setPath(resource);
        importableResourceInfo.put(resource, importable);
    }

    private void reloadJs(File jsFile) throws IOException {
        logger.info("Loading js " + jsFile.getPath());

        String path = jsFile.getAbsolutePath().substring(basePagesPath.length());

        if (validResources.containsKey(path.substring(0, path.lastIndexOf(".js"))) || path.equals("/lifecycle.js")) {
            //it has a htmx pair
            return;
        }

        importableScripts.add(path);

        //plain
        path = path.replaceAll("\\.js$", ".p.js");
        addPath(validResources, path, null);

        JsResource resInfo = (JsResource) getResourceInfo(path);
        try {
            //load it as local
            loadPageAndCache(resInfo);
        } catch (XHTMLParsingException e) {
            logger.warn("Path " + path + " has an invalid htmx file. ", e);
        } catch (Exception e) {
            logger.warn("Error loading path " + path + ". ", e);
        }
        ImportableResourceInfo importable = new ImportableResourceInfo();
        importable.setPath(path);
        importableResourceInfo.put(path, importable);
    }

    //preloads all htmx resources and its scripts. this returns the paths of htmx found
    private void reloadHtmxFiles() throws IOException {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".htmx");
            }
        };
        List<File> htmxList = XFileUtil.instance.listFiles("/pages", filter);
        for (File htmxFile : htmxList) {
            reloadHtmxFiles(htmxFile);
        }
    }

    private void reloadHtmxFiles(File htmxFile) throws IOException {
        ImportableResourceInfo info = new ImportableResourceInfo();
        int htmxStrLength = ".htmx".length();

        String path = htmxFile.getAbsolutePath().substring(basePagesPath.length(), htmxFile.getAbsolutePath().length() - htmxStrLength);
        if (path.endsWith(".modal")) {
            path = path.substring(0, path.lastIndexOf(".modal"));
        }
        info.setPath(path);

        if (!htmxFile.getName().startsWith("_")) {
            XAuthManager.AuthProperties authProperties = XAuthManager.instance.getAuthProperties(path);
            info.setAuth(authProperties);
        }

        Resource resInfo = getResourceInfo(path);
        try {
            //load html main window
            loadPageAndCache(resInfo);
            addPath(validResources, path, null);

            info.setTemplateName(((HtmxResource) resInfo).getTemplateName());
            importableResourceInfo.put(info.getPath(), info);
            //if it has template is modal
            String jsPath = path + (info.getTemplateName() != null ? ".m.js" : ".p.js");

            resInfo = getResourceInfo(jsPath);
            //load html spa window
            loadPageAndCache(resInfo);
            addPath(validResources, path + (info.getTemplateName() != null ? ".m" : ".p") + ".js", null);
        } catch (XHTMLParsingException e) {
            logger.warn("Path " + path + " has an invalid htmx file. ", e);
        } catch (Exception e) {
            logger.warn("Error loading " + path + ".", e);
        }
    }

    /**
     * load htmx and js resources, prepare and cache them.
     */
    private byte[] loadPage(Resource resInfo) throws IOException, XHTMLParsingException {
        logger.info("Loading htmx " + resInfo.getPath());
        boolean isModal = resInfo.modal;
        boolean isGlobal = (resInfo instanceof JsResource && ((JsResource) resInfo).global);
        boolean isJs = resInfo instanceof XResourceManager.JsResource;
        byte[] page = XFileUtil.instance.readFromDisk(resInfo.getRelativePath(), isJs ? "/empty_file" : null);
        boolean usesTemplate = false;
        if (page != null) {
            String strResponse = new String(page);
            String htmlStruct = null;
            //get all the bound variables in the page
            Set<String> boundVars = new HashSet<String>();
            //get all the bound modals in the page
            Map<String, XModalBind> boundModals = new HashMap<String, XModalBind>();
            Map<String, List<Map<String, Object>>> components = new HashMap<String, List<Map<String, Object>>>();
            if (!isJs || isModal) {
                String html = strResponse;
                if (isModal) {
                    //if is modal the page contains the script. So getting the html
                    html = new String(XFileUtil.instance.readFromDisk(((JsResource) resInfo).getHtmx().getRelativePath(),
                            null));
                }
                XHTMLDocument doc = null;
                //spa main window is the window tha loads just the template. It should not load any page previously
                boolean isSpaMainWindow = false;
                if (!isModal) {
                    boolean hasHtmlElemennt = new XHTMLParser().hasHtmlElement(html);
                    XHTMLDocument templateDoc = null;
                    //check the template
                    if (!hasHtmlElemennt) {
                        isSpaMainWindow = true;
                        usesTemplate = true;
                        String templateName = XTemplates.getTemplateName(html, defaultTemplateName, resInfo.isImplicit());
                        templateDoc = checkTemplate(resInfo, templateName, boundVars, boundModals);
                        ((HtmxResource) resInfo).templateName = templateName;
                        if (isConfiguredToBeSpa) {
                            //is main window
                            doc = templateDoc;
                        } else {
                            //must return the entire page
                            doc = parseHTMLPage(html, boundModals, boundVars);
                            List<XElement> xbody = templateDoc.getElementsByName("xbody");
                            for (XNode node : doc.getChildren()) {
                                xbody.get(0).addChild(node);
                            }
                            templateDoc.getRequiredResourcesList().addAll(doc.getRequiredResourcesList());
                            doc = templateDoc;
                        }
                    }

                }
                if (doc == null) {
                    //just return the page (it is spa or modal)
                    doc = parseHTMLPage(html, boundModals, boundVars);
                }
                if (resInfo instanceof HtmxResource) {
                    ((HtmxResource) resInfo).hasHtmlElement = !usesTemplate;
                }

                //will put all the iterators here
                List<List<Object>> iteratorList = new ArrayList<List<Object>>();

                //place real html of components, prepare iterators and labels
                XComponents.prepareHTML(doc, boundVars, boundModals, components, iteratorList, isModal);

                if (!isModal) {
                    //prepare not in a modal like html, loader
                    prepareTopElements(doc);
                    String userAppCache = doc.getHtmlElement().getAttribute("data-useappcache");
                    if (userAppCache != null && userAppCache.toUpperCase().equals("TRUE") && !XContext.isDevMode()) {
                        doc.getHtmlElement().removeAttributes("data-useappcache");
                        doc.getHtmlElement().setAttribute("manifest", X.getContextPath() + "/x/_appcache");
                    }
                }
                // remove xbody element
                if (!isSpaMainWindow) {
                    prepareXBody(doc.getElementsByName("xbody"));
                }

                List<XElement> requiredSourceList = doc.getRequiredResourcesList();
                for (XElement requiredElement : requiredSourceList) {
                    String src = requiredElement.getAttribute("src");
                    if (src.startsWith("/")) {
                        src = src.substring(1);
                    }
                    src = "/res/" + src;
                    addPath(validResources, src, resInfo.getRealPath());
                }
                if (!isModal) {
                    Map<String, Object> jsonDynAtt = new HashMap<String, Object>();
                    Map<String, Map<String, Object>> jsonHiddenAtt = new HashMap<String, Map<String, Object>>();
                    Map<String, String> jsonComp = new HashMap<String, String>();

                    html = XTemplates.replaceVars(doc.getHTML(jsonDynAtt, jsonHiddenAtt, jsonComp));

                    StringBuilder postString = new StringBuilder();

                    postString.append("\n(function(){\n");
                    postString.append("\n		var X = new _XClass();");

                    //the main window should always register as it might have iterators, xscripts and dyn attribs
                    postString.append("\n		X._registerObjects(").append(XJson.toJson(jsonDynAtt))
                            .append(",");
                    postString.append("\n			").append(XJson.toJson(jsonHiddenAtt)).append(",");
                    postString.append("\n			").append(XJson.toJson(iteratorList)).append(",");
                    postString.append("\n			").append(XJson.toJson(jsonComp)).append(",");
                    postString.append("\n			").append(XJson.toJson(components)).append(");");

                    if (!isSpaMainWindow) {
                        postString.append("\n		X._getJS('" + resInfo.getPath() + ".p.js', null, function(){");
                        postString.append("\n			console.log('X Loaded');");
                        postString.append("\n		})");
                    } else {
                        postString.append("\n         var xbody = document.getElementsByTagName('xbody')[0];");

                        postString.append("\n         X$._xbodyNode = xbody;");
                        postString.append("\n         X$._isSpa = true;");
                        postString.append("\n         X$._xbodyNode.xsetModal = function(child){");
                        postString.append("\n             X$._xbodyNode.appendChild(child);");
                        postString.append("\n         };");

                        postString.append("\n         var controller = new function(){var __xbinds__ = null; this._x_eval = function(f){return eval(f)};};");
                        postString.append("\n         X._setEvalFn(controller._x_eval);");
                        postString.append("\n         document.body.setAttribute('data-x_ctx', 'true');");
                        postString.append("\n         X.setController(controller, function(){console.log('X started (spa)');});");
                        postString.append("\n         X.setSpaModalNode(X$._xbodyNode);");
                    }
                    postString.append("\n})();");

                    html = html.replace("{xpostscript}", postString.toString());
                    tempBoundVars.put(resInfo.getPath() + ".js", boundVars);
                    tempBoundModals.put(resInfo.getPath() + ".js", boundModals);
                    strResponse = html;
                } else {
                    htmlStruct = XTemplates.replaceVars(doc.toJson());
                }
                addChildValidElements(resInfo, doc);
            }
            if (isJs) {
                if (!isModal) {
                    boundVars = tempBoundVars.remove(resInfo.getPath());
                    boundModals = tempBoundModals.remove(resInfo.getPath());
                }
                strResponse = XTemplates.replaceVars(strResponse);
                try {
                    strResponse = XJS.instrumentController(strResponse, resInfo.getPath(),
                            boundVars, boundModals, isModal, isGlobal, htmlStruct, XJson.toJson(components), (JsResource) resInfo);
                } catch (ScriptException e) {
                    String msg = "Error in script: " + resInfo.getRealPath();
                    logger.error(msg, e);
                    throw new RuntimeException(msg, e);
                }
            }
            if (boundModals != null) {
                for (XModalBind modal : boundModals.values()) {
                    addPath(modalPathsDeclared, modal.getPath(), resInfo.getRealPath());
                }
            }
            page = strResponse.replace("{webctx}", X.getContextPath()).getBytes("UTF-8");
            return page;
        }
        return null;
    }

    private XHTMLDocument parseHTMLPage(String html, Map<String, XModalBind> boundModals, Set<String> boundVars) throws XHTMLParsingException {
        XHTMLParser parser = new XHTMLParser();
        XHTMLDocument doc = parser.parse(html);
        //remove any xbody. It should be just in template
        List<XElement> listXBody = doc.getElementsByName("xbody");
        for (XElement e : listXBody) {
            e.remove();
        }
        //get all the bound variables in the page
        boundVars.addAll(parser.getBoundObjects());
        //get all the bound modals in the page
        boundModals.putAll(parser.getBoundModals());
        return doc;
    }

    //add child element of this doc that can be cached with appcache
    private void addChildValidElements(Resource resInfo, XHTMLDocument doc) {
        List<XElement> scripts = findElementsOnHtml(doc, "script");
        for (XElement script : scripts) {
            String src = script.getAttribute("src");
            if (src != null && !src.startsWith("/x/")) {
                addPath(validResources, src.replace("{webctx}", ""), resInfo.getRealPath());
            }
        }
        List<XElement> links = findElementsOnHtml(doc, "link");
        for (XElement link : links) {
            String href = link.getAttribute("href");
            if (href != null) {
                addPath(validResources, href.replace("{webctx}", ""), resInfo.getRealPath());
            }
        }
    }

    private List<XElement> findElementsOnHtml(XHTMLDocument doc, String elementName) {
        return doc.getElementsByName(elementName);
    }

    private Map<String, Set<String>> tempBoundVars = new HashMap<String, Set<String>>();
    private Map<String, Map<String, XModalBind>> tempBoundModals = new HashMap<String, Map<String, XModalBind>>();

    public byte[] getPageContents(Resource resInfo) throws IOException, XHTMLParsingException {
        if (XContext.isDevMode()) {
            return loadPage(resInfo);
        }
        return pages.get(getResInfoKey(resInfo));
    }

    private String getResInfoKey(Resource resInfo) {
        return resInfo.getRealPath() + "|" + resInfo.modal + "|" +
                (resInfo instanceof HtmxResource ? false : resInfo instanceof JsResource ? ((JsResource) resInfo).global : false);
    }

    /**
     * Just main windows here. Modal shouldnt have xbody
     */
    private void prepareXBody(List<XElement> listXBody) {
        if (listXBody.isEmpty()) {
            //modal or no xbody
            return;
        }
        XElement xbody = listXBody.get(0);

        List<XNode> nodeList = xbody.getChildren();
        //get previous if exists or parent
        XNode prev = xbody.getPrevious();
        while (prev != null && prev instanceof XText && ((XText) prev).getText().trim().equals("")) {
            prev = prev.getPrevious();
        }
        XNode firstNode = nodeList.get(0);
        if (prev == null) {
            prev = xbody.getParent();
            ((XElement) prev).addChild(firstNode);
        } else {
            prev.addAfter(firstNode);
        }
        prev = firstNode;
        xbody.remove();
        for (int i = 1; i < nodeList.size(); i++) {
            XNode n = nodeList.get(i);
            prev.addAfter(n);
            prev = n;
        }
    }

    private void prepareTopElements(XHTMLDocument doc) {
        List<XElement> elementList = doc.findChildrenByName("html");
        if (elementList.isEmpty() || elementList.size() > 1) {
            throw new RuntimeException(
                    "Invalid page. There must be one (and only one) html element in a html page");
        }
        XElement htmlEl = elementList.get(0);
        prepareXScripts(doc, htmlEl);

        elementList = htmlEl.findChildrenByName("body");
        if (elementList.isEmpty() || elementList.size() > 1) {
            throw new RuntimeException(
                    "Invalid page. There must be one (and only one) body element in a html page");
        }
        XText newLine = new XText();
        newLine.setText("\n\n");
        elementList.get(0).addChild(newLine);

        XElement tempLoadDiv = new XElement("div", doc);
        tempLoadDiv.setAttribute("id", "_xtemploaddiv_");
        tempLoadDiv.setAttribute("style",
                "position:absolute;top:0px;left:0px;height: 100%;width:100%;z-index: 99999;background-color: white;");
        XElement imgLoad = new XElement("img", doc);
        imgLoad.setAttribute("style",
                "position:absolute;top:0;left:0;right:0;bottom:0;margin:auto;");
        imgLoad.setAttribute("height", "42");
        imgLoad.setAttribute("width", "42");
        imgLoad.setAttribute("src", "{webctx}/x/loader.gif");
        tempLoadDiv.addChild(imgLoad);
        elementList.get(0).insertChild(tempLoadDiv, 0);

        // controller
        XElement script = new XElement("script", doc);
        script.setAttribute("type", "text/javascript");

        XText text = new XText();
        text.setText("{xpostscript}");
        script.addChild(text);
        elementList.get(0).addChild(script);
    }

    /**
     * Check if the page needs a template (if no html element is found). If needs gets the template and put into de doc
     */
    private XHTMLDocument checkTemplate(Resource resInfo, String templateName, Set<String> boundVars, Map<String, XModalBind> boundModals)
            throws IOException, XHTMLParsingException {
        XHTMLDocument docTemplate = templateMap.get(templateName);
        if (docTemplate == null) {
            // Page has no html element. Getting html
            // template...
            String htmlTemplatePage = XTemplates.getTemplate(templateName, resInfo.isImplicit());
            XHTMLParser templateParser = new XHTMLParser();
            docTemplate = templateParser.parse(htmlTemplatePage);
            List<XElement> xbody = docTemplate.getElementsByName("xbody");
            if (xbody.isEmpty()) {
                throw new RuntimeException(
                        "Invalid template " + templateName + ". There must be a {xbody}");
            } else if (xbody.size() > 1) {
                throw new RuntimeException(
                        "Invalid template " + templateName + ". There must be just one {xbody}");
            }
            boundVars.addAll(templateParser.getBoundObjects());
            boundModals.putAll(templateParser.getBoundModals());
        }
        return (XHTMLDocument) docTemplate.clone();
    }

    private void prepareXScripts(XHTMLDocument doc, XElement htmlEl) {
        List<XElement> elementList;
        elementList = htmlEl.findChildrenByName("head");
        XElement headEl;
        if (elementList.isEmpty()) {
            headEl = new XElement("head", doc);
            htmlEl.insertChild(headEl, 0);
        } else {
            headEl = elementList.get(0);
        }
        // params
        XElement script;

        if (X.isRunningInServletContainer()) {
            script = new XElement("script", doc);
            script.setAttribute("type", "text/javascript");
            XText scriptContent = new XText();
            StringBuilder strScript = new StringBuilder();
            //loads user info
            strScript.append("var xhttp = new XMLHttpRequest();\n" +
                    "  xhttp.onreadystatechange = function() {\n" +
                    "    if (this.readyState == 4 && this.status == 200) {\n" +
                    "     eval('window.xuser = ' + this.responseText);\n" +
                    "     window._x_parameters_loaded = true;\n" +
                    "    }\n" +
                    "  };\n" +
                    "  xhttp.open(\"POST\", \"" + X.getContextPath() + "/x/_xprms\", true);\n" +
                    "  xhttp.send();");
            scriptContent.setText(strScript.toString());
            script.addChild(scriptContent);
            headEl.addChild(script);
        }
        // cache timestamp
        script = new XElement("script", doc);
        headEl.addChild(script);

        // x.js
        script = new XElement("script", doc);
        script.setAttribute("type", "text/javascript");
        script.setAttribute("src", "{webctx}/x/scripts/x.js");
        headEl.addChild(script);

        for (XElement e : doc.getRequiredResourcesList()) {
            String source = e.getAttribute("src").trim();
            source = source.startsWith("/") ? source : "/" + source;
            if (source.toLowerCase().endsWith(".js")) {
                script = new XElement("script", doc);
                script.setAttribute("type", "text/javascript");
                script.setAttribute("src", "{webctx}/res" + source);
                headEl.addChild(script);
            } else if (source.toLowerCase().endsWith("css") && headEl != null) {
                XElement linkEl = headEl.addElement("link");
                linkEl.setAttribute("href", "{webctx}/res" + source);
                if (e.getAttribute("rel") != null) {
                    linkEl.setAttribute("rel", e.getAttribute("rel"));
                }
                linkEl.setAttribute("rel", "stylesheet");
                if (e.getAttribute("media") != null) {
                    linkEl.setAttribute("media", e.getAttribute("media"));
                }
            }
        }
    }

    public Resource getResourceInfo(String path) throws IOException {
        Resource resInfo = resourceInfoMap.get(path);
        if (resInfo == null && !resourceInfoMap.containsKey(path)) {
            synchronized (this) {
                Resource result = null;

                boolean isJS = path.endsWith(".js");

                String noExtensionPath = path;
                if (isJS) {
                    result = new JsResource();
                    result.modal = path.endsWith(".m.js");
                    ((JsResource) result).global = !result.modal && !path.endsWith(".p.js");

                    noExtensionPath = path.substring(0, path.lastIndexOf('.'));
                    if (!((JsResource) result).global) {
                        noExtensionPath = noExtensionPath.substring(0, noExtensionPath.lastIndexOf('.'));
                    }
                }
                boolean isDir = isDir(noExtensionPath);

                if (isJS) {
                    if (isDir) {
                        result.implicit = true;
                        File index = new File(X.getRealPath("/pages" + noExtensionPath + "/_index.js"));
                        if (index.exists()) {
                            result.redirect = noExtensionPath + "/_index.js";
                        } else {
                            result.redirect = noExtensionPath + "/index.js";
                        }
                    } else {
                        result.relativePath = "/pages" + noExtensionPath + ".js";
                        HtmxResource htmx = (HtmxResource) getResourceInfo(noExtensionPath);
                        ((JsResource) result).htmx = htmx;
                        result.realPath = X.getRealPath(result.relativePath);
                        boolean existsJs = exists(result);
                        if (!existsJs && htmx == null) {
                            resourceInfoMap.put(path, null);
                            return null;
                        }
                    }

                } else {
                    result = new HtmxResource();
                    if (isDir) {
                        result.implicit = true;
                        File index = new File(X.getRealPath("/pages" + noExtensionPath + "/_index.htmx"));
                        if (index.exists()) {
                            result.redirect = noExtensionPath + "/_index";
                        } else {
                            result.redirect = noExtensionPath + "/index";
                        }
                    } else {
                        result.relativePath = "/pages" + noExtensionPath + ".htmx";
                        result.realPath = X.getRealPath(result.relativePath);
                        if (!exists(result)) {
                            result.relativePath = "/pages" + noExtensionPath + ".modal.htmx";
                            result.realPath = X.getRealPath("/pages" + noExtensionPath + ".modal.htmx");
                            if (!exists(result)) {
                                resourceInfoMap.put(path, null);
                                return null;
                            }
                            ((HtmxResource) result).modal = true;
                        }
                    }
                }
                result.path = path;

                if (!isDir) {
                    int lastIndex = path.charAt(path.length() - 1) == '/' ? path.lastIndexOf("/", 1)
                            : path.lastIndexOf("/");

                    result.authProperties = XAuthManager.instance.getAuthProperties(path);
                    if (result.authProperties != null && result.authProperties.getNeedsAuthentication()) {
                        result.needsLogin = result.authProperties.getNeedsAuthentication();
                    } else {
                        result.needsLogin = path.length() <= 1 || path.charAt(lastIndex + 1) != '_';
                    }
                }
                resourceInfoMap.put(path, result);
            }
            resInfo = resourceInfoMap.get(path);
        }
        return resInfo;
    }

    private boolean exists(Resource res) {
        return new File(res.realPath).exists();
    }

    public abstract static class Resource {
        private boolean implicit;//the name is index
        private boolean needsLogin;
        private String path;
        private String redirect;
        private String realPath;
        private String relativePath;
        private XAuthManager.AuthProperties authProperties;
        private boolean modal;

        public boolean isNeedsLogin() {
            return needsLogin;
        }

        public String getPath() {
            return path;
        }

        public String getRedirect() {
            return redirect;
        }

        public String getRealPath() {
            return realPath;
        }

        public XAuthManager.AuthProperties getAuthProperties() {
            return authProperties;
        }

        public boolean isImplicit() {
            return implicit;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public boolean isModal() {
            return modal;
        }
    }

    public static class HtmxResource extends Resource {
        private boolean modal;
        private boolean hasHtmlElement;
        private String templateName;

        public boolean hasHtmlElement() {
            return hasHtmlElement;
        }

        public String getTemplateName() {
            return templateName;
        }

        public boolean isModal() {
            return modal;
        }
    }

    public static class JsResource extends Resource {
        private HtmxResource htmx;
        private boolean global;

        public boolean isGlobal() {
            return global;
        }

        public HtmxResource getHtmx() {
            return htmx;
        }
    }

    //valid resources that can be loaded by the application. The x script will contains a list of this so it now if it is loadable by the browser
    public static class ImportableResourceInfo {
        private String path;
        private String templateName;
        private XAuthManager.AuthProperties auth;

        public XAuthManager.AuthProperties getAuth() {
            return auth;
        }

        public void setAuth(XAuthManager.AuthProperties auth) {
            this.auth = auth;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getTemplateName() {
            return templateName;
        }

        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }

    }

    private boolean isDir(String pathInfo) throws IOException {
        Boolean isDir = dirs.get(pathInfo);
        if (isDir != null) {
            return isDir;
        } else {
            isDir = false;
            if (pathInfo.indexOf('.') < 0) {
                String diskPath = X.getRealPath("/pages" + pathInfo);
                if (diskPath != null) {
                    isDir = new File(diskPath).isDirectory();
                }
            }
            dirs.put(pathInfo, isDir);
            return isDir;
        }
    }

    public Map<String, ImportableResourceInfo> getImportableResourceInfo() {
        return importableResourceInfo;
    }

    public String getAppCache() {
        return appCacheFile;
    }
}
