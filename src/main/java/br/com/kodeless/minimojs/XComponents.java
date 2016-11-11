
package br.com.kodeless.minimojs;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;
import javax.servlet.ServletContext;

import br.com.kodeless.minimojs.parser.XAttribute;
import br.com.kodeless.minimojs.parser.XElement;
import br.com.kodeless.minimojs.parser.XHTMLDocument;
import br.com.kodeless.minimojs.parser.XHTMLParser;
import br.com.kodeless.minimojs.parser.XHTMLParsingException;
import br.com.kodeless.minimojs.parser.XModalBind;
import br.com.kodeless.minimojs.parser.XNode;
import org.apache.log4j.Logger;

public class XComponents {

    private static final Logger logger = Logger.getLogger(XComponents.class);

    protected static List<String[]> components = new ArrayList<String[]>();

    protected static String serverJSComponents;

    protected static Map<String, String> htmxSources = new HashMap<String, String>();

    private static String jsComponents;

    protected static String getJsComponents() {
        return jsComponents;
    }

    protected static void loadComponents(ServletContext ctx) throws IOException, ScriptException {
        logger.info("Loading components");
        Map<String, String> map = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        Set<String> resources = ctx.getResourcePaths("/components");
        for (String resource : resources) {
            map.put(resource, XStreamUtil.inputStreamToString(ctx.getResourceAsStream(resource)));
        }
        jsComponents = loadComponents(map, ctx.getContextPath());
    }

    protected static String loadComponents(Map<String, String> map, String ctxPath) throws IOException, ScriptException {
        serverJSComponents = "function generateId() {return java.lang.System.currentTimeMillis() + parseInt(Math.random() * 999999);}";
        StringBuilder sb = new StringBuilder("var components = {};");
        components.clear();
        htmxSources.clear();

        List<String> keys = new ArrayList<String>(map.keySet());
        for (int index = 0; index < keys.size(); index++) {
            String path = keys.get(index);
            String[] parts = path.split("/");
            String lastPart = parts[parts.length - 1];
            if (lastPart.startsWith(".")) {
                continue;
            }
            String varPath = "components";
            for (int i = 2; i < parts.length - 1; i++) {
                String newInst = varPath + "['" + parts[i] + "']";
                sb.append(newInst).append("={}");
                varPath = newInst;
            }
            String[] split = lastPart.split("\\.");
            String resName = split[0];
            String jsName = null;
            String htmxName = null;
            if (lastPart.endsWith(".js") && keys.contains((htmxName = path.replaceAll("\\.js$", ".htmx")))) {
                jsName = path;
                keys.remove(keys.indexOf(htmxName));
            } else if (lastPart.endsWith(".htmx") && keys.contains((jsName = path.replaceAll("\\.htmx$", ".js")))) {
                htmxName = path;
                keys.remove(keys.indexOf(jsName));
            }
            varPath = varPath + "['" + resName + "']";
            String create = "new" + resName.substring(0, 1).toUpperCase() + resName.substring(1);
            if (jsName != null && htmxName != null) {
                sb.append(createHtmxComponent(map, jsName, htmxName, varPath, resName));
            } else {
                sb.append(createOldTypeComponent(map.get(path), varPath, split[1], resName));
            }
            if (!resName.startsWith("_")) {
                components.add(new String[]{resName, varPath, create});
            }
        }
        String array = "var _comps = [";
        for (String[] cname : components) {
            array += "[\"" + cname[0] + "\",\"" + cname[1] + "\"],";
        }
        array += "];";
        sb.insert(0, array);
        String result = sb.toString().replaceAll("\\{webctx\\}", ctxPath);
        XJS.prepareComponents("var components = {};" + serverJSComponents);
        return result;
    }

    private static String createOldTypeComponent(String s, String varPath, String extension, String resName) {
        String compJS = s;
        if (extension.equals("html")) {
            compJS = prepareTemplateComponent(compJS);
        }
        String js = varPath
                + "= new function(){ var toBind = {};function get(id){ var h={id:id};X.merge(toBind, h);return h;}; this.get = get;function bindToHandle(obj) {X.merge(obj, toBind);}; function expose(name, fn){"
                + varPath + "[name] = fn;};var load;" + compJS
                + ";try{this.context = context;}catch(e){};this.getHtml = getHtml;try{this.getBindingMethods = getBindingMethods;}catch(e){};var generateId = X.generateId;try{this.childElementsInfo = childElementsInfo;}catch(e){this.childElementsInfo = function(){return {}}}"
                + ";try{X._addExecuteWhenReady(load);}catch(e){};try{this.onReady = onReady;}catch(e){};try{this.onVisible = onVisible;}catch(e){};};";

        serverJSComponents += varPath + "= new function(){ " + compJS
                + ";this.getHtml = getHtml;try{this.childElementsInfo = childElementsInfo;}catch(e){this.childElementsInfo = function(){return {}}}};";

        return js;
    }

    private static String createHtmxComponent(Map<String, String> map, String jsName, String htmxName, String varPath, String componentName) throws IOException, ScriptException {
        String js = map.get(jsName);
        String htmx = map.get(htmxName);
        htmxSources.put(varPath, htmx);
        String result = varPath
                + "= new function(){ this.htmxContext = function(attrs){ var selfcomp = this; this._attrs = attrs; this._compName = '" + componentName + "';" +
                "this._xcompEval = function(f){try{return eval(f);}catch(e){throw " +
                "   new Error('Error executing script component ' + this._compName + '. Script: ' + f + '. Cause: ' + e.message);}};" +
                "try{this.childElementsInfo = childElementsInfo;}catch(e){this.childElementsInfo = function(){return {}}};" +
                "var defMandatoryString = X._createValProp(true, 's', selfcomp);" +
                "var defString = X._createValProp(false, 's', selfcomp);" +
                "var defMandatoryNumber = X._createValProp(true, 'n', selfcomp);" +
                "var defNumber = X._createValProp(false, 'n', selfcomp);" +
                "var defMandatoryBoolean = X._createValProp(true, 'b', selfcomp);" +
                "var defBoolean = X._createValProp(false, 'b', selfcomp);" +
                "var defMandatoryJsValue = X._createValProp(true, 'scr', selfcomp);" +
                "var defJsValue = X._createValProp(false, 'scr', selfcomp);" +
                "var defMandatoryChildString = X._createValProp(true, 's', selfcomp, null, null, true);" +
                "var defChildString = X._createValProp(false, 's', selfcomp, null, null, true);" +
                "var defMandatoryChildNumber = X._createValProp(true, 'n', selfcomp, null, null, true);" +
                "var defChildNumber = X._createValProp(false, 'n', selfcomp, null, null, true);" +
                "var defMandatoryChildBoolean = X._createValProp(true, 'b', selfcomp, null, null, true);" +
                "var defChildBoolean = X._createValProp(false, 'b', selfcomp, null, null, true);" +
                "var defMandatoryChildJsValue = X._createValProp(true, 'src', selfcomp, null, null, true);" +
                "var bind = X._bindValProp(selfcomp);" +
                "var defChildJsValue = X._createValProp(false, 'src', selfcomp, null, null, true);\n\n" +
                js + "\n" + XJS.exposeFunctions(js, "", "this") +
                "\n\n;var generateId = X.generateId;}};" + varPath + ".childElementsInfo = new " + varPath + ".htmxContext({}).childElementsInfo;";

        serverJSComponents += varPath + "= new function(){ " + js
                + ";try{this.childElementsInfo = childElementsInfo;}catch(e){this.childElementsInfo = function(){return {}}}};";
        return result;
    }


    static Pattern patternTemplate = Pattern.compile("#\\{(.*?)}");

    private static String prepareTemplateComponent(String originalJS) {
        String compJS = originalJS.replace("\n", "").replace("'", "\\'");
        Matcher matcher = patternTemplate.matcher(compJS);
        while (matcher.find()) {
            String val = matcher.group(1);
            if (!val.equals("xbody")) {
                compJS = compJS.replace("#{" + val + "}", "' + (" + val.replace("\\'", "'") + ") + '");
                matcher = patternTemplate.matcher(compJS);
            }
        }
        return "function getHtml(comp){ return '" + compJS + "';}";
    }

    public static void main(String[] args) {
        try {
            String html = XFileUtil.instance.readFile("/Users/eduardo/work/xloja/XLojaWEB/WebContent/components/pagina.html");
            System.out.println(html);
            String result = prepareTemplateComponent(html);
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void prepareHTML(XHTMLDocument doc, final String context, Properties properties, String pathInfo,
                                   Set<String> boundVars, Map<String, XModalBind> boundModals,
                                   Map<String, List<Map<String, Object>>> componentMap, List<List<Object>> iteratorsList, boolean isModal) {
        try {
            List<XElement> requiredSourceList = doc.getRequiredResourcesList();

            // prepared components
            for (String[] comp : components) {
                String tagName = comp[0];
                String componentName = comp[1];
                buildComponent(tagName, componentName, doc, componentMap, requiredSourceList, boundVars, boundModals);
            }
            prepareIterators(doc, iteratorsList, isModal);
            prepareLabels(doc);
            XElement recValues = new XElement("xrs", doc);
            recValues.addChildList(requiredSourceList);
            doc.addChild(recValues);
        } catch (Exception e) {
            throw new RuntimeException("Error preparing html file", e);
        }
    }

    static Pattern pattern = Pattern.compile("\\(%(.*?%)\\)");

    private static void prepareLabels(XHTMLDocument html) throws XLabelException {
        html.replaceAllTexts(new XHTMLDocument.TextReplacer() {
            @Override
            public String replace(String text) {
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    String val = matcher.group();
                    val = val.substring(2, val.length() - 2);
                    String newVal = XLabels.getLabel(val);
                    text = matcher.replaceAll(newVal);
                    matcher = pattern.matcher(text);
                }
                return text;
            }

        });
    }

    private static synchronized void prepareIterators(XElement mainElement, List<List<Object>> iterators,
                                                      boolean isModal) throws XHTMLParsingException {
        XElement iterEl;
        while ((iterEl = findIterators(mainElement)) != null) {
            String xiterId = generateId();
            boolean isHidden = iterEl.getName().equalsIgnoreCase("xiterator");
            iterEl.setHiddenAttribute("xiterId", xiterId);
            iterEl.setHiddenAttribute("xiteratorStatus", "none");
            iterEl.setHiddenAttribute("xiteratorElement", "true");
            String listOrTimes = iterEl.getAttribute(isHidden ? "list" : "data-xiterator-list");
            boolean isTimes = false;
            if (listOrTimes == null) {
                isTimes = true;
                listOrTimes = iterEl.getAttribute(isHidden ? "count" : "data-xiterator-count");
            }
            if (listOrTimes == null) {
                throw new RuntimeException("Iterator must have a list or a count var");
            }
            List<Object> params = new ArrayList<Object>();
            params.add(xiterId);
            params.add(listOrTimes);
            String var = iterEl.getAttribute(isHidden ? "var" : "data-xiterator-var");
            params.add(var);
            var = iterEl.getAttribute(isHidden ? "indexvar" : "data-xiterator-indexvar");
            params.add(var);
            if (!isModal) {
                removeIteratorAttributes(iterEl);
            }
            iterEl.setTempAttribute("prepared-iterator", true);
            params.add(iterEl.toJson());
            params.add(isTimes);
            iterators.add(params);
            if (!isModal) {
                iterEl.removeAllChildren();
            }
        }
    }

    private static void removeIteratorAttributes(XElement iterEl) {
        if (iterEl.getName().equalsIgnoreCase("xiterator")) {
            iterEl.removeAttributes("indexvar", "var", "list", "count");
        } else {
            for (XAttribute a : iterEl.getAttributes()) {
                if (a.getName().startsWith("data-xiterator-")) {
                    iterEl.removeAttributes(a.getName());
                }
            }
        }
    }

    private static synchronized void buildComponent(String tagName, String componentName, XHTMLDocument doc,
                                                    Map<String, List<Map<String, Object>>> components, List<XElement> requiredList, Set<String> boundVars,
                                                    Map<String, XModalBind> boundModals) throws XHTMLParsingException {
        XElement element;
        while ((element = findDeepestChild(doc, tagName.toLowerCase())) != null) {

            // get declared properties in doc tag - start
            Map<String, Object> infoProperties = new HashMap<String, Object>();
            Map<String, Map<String, String>> childInfo = XJS.getChildElementsInfo(componentName);
            for (Map.Entry<String, Map<String, String>> entry : childInfo.entrySet()) {
                List<Map<String, Object>> childInfoProperties = new ArrayList<Map<String, Object>>();
                List<XElement> childElements = findAllChildren(element, (String) entry.getValue().get("from"));
                for (XElement child : childElements) {
                    Map<String, Object> childInfoMap = new HashMap<String, Object>();
                    childInfoMap.put("innerHTML", child.innerHTML());
                    for (XAttribute a : child.getAttributes()) {
                        childInfoMap.put(a.getName(), a.getValue());
                    }
                    child.remove();
                    childInfoProperties.add(childInfoMap);
                }
                infoProperties.put(entry.getKey(), childInfoProperties);
            }
            for (XAttribute a : element.getAttributes()) {
                infoProperties.put(a.getName(), a.getValue());
            }
            // get declared properties in doc tag - finish

            // generate html
            String newHTML = getHtml(componentName, infoProperties);
            if (infoProperties.containsKey("xid")) {
                newHTML = "<div _s_xid_='" + infoProperties.get("xid") + "'></div>" + newHTML + "<div _e_xid_='"
                        + infoProperties.get("xid") + "'></div>";
            }

            // change xbody
            newHTML = XStringUtil.replaceFirst(newHTML, "{xbody}", "<_temp_x_body/>");

            // parse new html
            XHTMLParser parser = new XHTMLParser();
            XHTMLDocument newDoc = parser.parse(newHTML);
            String id = generateId();
            newDoc.setHiddenAttributeOnChildren("xcompId", id);
            newDoc.setHiddenAttributeOnChildren("xcompName", tagName);
            infoProperties.put("xcompId", id);
            infoProperties = removeHTML(infoProperties);

            List<XElement> findBody = newDoc.getElementsByName("_temp_x_body");
            if (!findBody.isEmpty()) {
                if (element.getChildren().isEmpty()) {
                    findBody.get(0).remove();
                } else {
                    XNode node = element.getChildren().get(0);
                    findBody.get(0).replaceWith(node);
                    for (int i = 1; i < element.getChildren().size(); i++) {
                        XNode child = element.getChildren().get(i);
                        node.addAfter(child);
                        node = child;
                    }
                }
            }
            if (boundVars != null) {
                boundVars.addAll(parser.getBoundObjects());
            }
            if (boundModals != null) {
                boundModals.putAll(parser.getBoundModals());
            }
            requiredList.addAll(newDoc.getRequiredResourcesList());
            List<XNode> list = newDoc.getChildren();
            XNode newNode = list.get(0);
            element.replaceWith(newNode);
            for (int i = 1; i < list.size(); i++) {
                XNode auxNode = list.get(i);
                newNode.addAfter(auxNode);
                newNode = auxNode;
            }
            List<Map<String, Object>> listByComponent = components.get(tagName);
            if (listByComponent == null) {
                listByComponent = new ArrayList<Map<String, Object>>();
                components.put(tagName, listByComponent);
            }

            listByComponent.add(infoProperties);
        }
    }

    private static String getHtml(String componentName, Map<String, Object> infoProperties) {
        if (htmxSources.containsKey(componentName)) {
            return htmxSources.get(componentName);
        }
        return XJS.getHtml(componentName, infoProperties);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> removeHTML(Map<String, Object> infoProperties) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Map.Entry<String, Object> e : infoProperties.entrySet()) {
            if (!e.getKey().equals("innerHTML")) {
                if (e.getValue() instanceof Map) {
                    map.put(e.getKey(), removeHTML((Map<String, Object>) e.getValue()));
                } else {
                    map.put(e.getKey(), e.getValue());
                }
            }
        }
        return map;
    }

    public static final String generateId() {
        return "i" + (java.lang.System.currentTimeMillis() + (int) (Math.random() * 99999));
    }

    private static List<XElement> findAllChildren(XElement element, String tagName) {
        List<XElement> list = new ArrayList<XElement>();
        for (XNode child : element.getChildren()) {
            if (child instanceof XElement) {
                XElement e = (XElement) child;
                List<XElement> children = findAllChildren(e, tagName);
                list.addAll(children);
                if (e.getName().equals(tagName.toLowerCase())) {
                    list.add(e);
                }
            }
        }
        return list;
    }

    private static XElement findIterators(XElement mainElement) {
        return findDeepestElementIterator(mainElement);
    }

    private static XElement findDeepestElementIterator(XElement mainElement) {
        List<XElement> list = mainElement.getElements();
        for (XElement e : list) {
            XElement deep = findDeepestElementIterator(e);
            if (deep != null) {
                return deep;
            } else if ((e.getName().equals("xiterator") && e.getHiddenAttribute("xiteratorStatus") == null)
                    || ((e.getAttribute("data-xiterator-list") != null || e.getAttribute("data-xiterator-count") != null)
                    && e.getTempAttribute("prepared-iterator") == null)) {
                return e;
            }
        }
        return null;
    }

    private static XElement findDeepestXIterator(XElement mainElement) {
        List<XElement> list = mainElement.getElementsByName("xiterator");
        if (list != null && !list.isEmpty()) {
            XElement e = null;
            for (int i = 0; i < list.size(); i++) {
                e = list.get(i);
                if (e.getHiddenAttribute("xiteratorStatus") == null) {
                    break;
                }
            }
            if (e != null) {
                XElement deep = findDeepestXIterator(e);
                if (deep == null && e.getHiddenAttribute("xiteratorStatus") == null) {
                    return e;
                } else {
                    return deep;
                }
            }
        }
        return null;
    }

    private static XElement findDeepestChild(XElement mainElement, String tagName) {
        List<XElement> list = mainElement.getElementsByName(tagName);
        if (list != null && !list.isEmpty()) {
            XElement e = list.get(0);
            XElement deep = findDeepestChild(e, tagName);
            if (deep == null) {
                return e;
            } else {
                return deep;
            }
        }
        return null;
    }

    protected static XElement findDeepestChildWithAttribute(XElement mainElement, String attributeName) {
        List<XElement> list = mainElement.getElementsWithAttribute(attributeName);
        if (list != null && !list.isEmpty()) {
            XElement e = list.get(0);
            XElement deep = findDeepestChildWithAttribute(e, attributeName);
            if (deep == null) {
                return e;
            } else {
                return deep;
            }
        }
        return null;
    }

    public static void mainx(String[] args) {
        try {
            XJS.prepareComponents(XStreamUtil.inputStreamToString(
                    new FileInputStream("/Users/eduardo/work/eclipseworkspaces/xloja/Testes/teste.js")));
            String htmlIn = XStreamUtil.inputStreamToString(
                    new FileInputStream("/Users/eduardo/work/eclipseworkspaces/xloja/Testes/teste.html"));
            XHTMLParser parser = new XHTMLParser();
            XHTMLDocument doc = parser.parse(htmlIn);
            // buildComponent("texto", "components['texto']", doc, new
            // StringBuffer());
            System.out.println(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
