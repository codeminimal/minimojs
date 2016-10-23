package br.com.kodeless.minimaljs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.ServletContext;

import br.com.kodeless.minimaljs.parser.XModalBind;
import org.apache.log4j.Logger;

import br.com.kodeless.minimaljs.parser.XHTMLParsingException;

public class XJS {

    static final Logger logger = Logger.getLogger(XJS.class);

    private static enum Annotation {
        service, importJs, modal, getter, setter, inject
    }

    static ScriptEngineManager factory = new ScriptEngineManager();
    static ScriptEngine engine;
    static ScriptEngine compEngine;
    static String components;
    static Map<String, String> modalInfoMap = new HashMap<String, String>();


    static {
        StringBuilder sb = new StringBuilder("[");
        for (String[] comp : XComponents.components) {
            sb.append("['X', 'comp', '").append(comp[2]).append("'],");
        }
        sb.append("]");
        components = sb.toString();
    }

    static {
        factory = new ScriptEngineManager();
        engine = factory.getEngineByName("JavaScript");
        String esprimaJS;
        try {
            esprimaJS = XStreamUtil.loadResource("/esprima.js");
            engine.eval(esprimaJS);
        } catch (Exception e) {
            String msg = "Error starting JSParser.";
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static synchronized void prepareComponents(String js) {

        compEngine = factory.getEngineByName("JavaScript");
        try {
            compEngine.eval(js);
        } catch (Exception e) {
            String msg = "Error starting Components on server.";
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }

    }

    @SuppressWarnings("unchecked")
    public static synchronized Map<String, Map<String, String>> getChildElementsInfo(String componentName) {
        if (componentName == null) {
            return new HashMap<String, Map<String, String>>();
        }
        try {
            compEngine.eval("var __temp_info__ = " + componentName + ".childElementsInfo();");
            return (Map<String, Map<String, String>>) compEngine.get("__temp_info__");
        } catch (ScriptException e) {
            String msg = "Error getting child info on server.";
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static synchronized String getHtml(String componentName, Map<String, Object> compObj) {
        try {
            compEngine.eval("var __temp_comp__ = " + XJson.toJson(compObj) + ";");
            compEngine.eval("var __temp_html__ = " + componentName + ".getHtml(__temp_comp__);");
            Object html = compEngine.get("__temp_html__");
            @SuppressWarnings("unchecked")
            Map<String, Object> compObjChanged = (Map<String, Object>) compEngine.get("__temp_comp__");
            for (Map.Entry<String, Object> e : compObjChanged.entrySet()) {
                compObj.put(e.getKey(), e.getValue());
            }
            return html != null ? html.toString() : "";
        } catch (ScriptException e) {
            String msg = "Error getHtml on server for component: " + componentName;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static synchronized String getFirstLevelFunctions(String js) throws IOException, ScriptException {
        int from = 0;
        while (js.indexOf('\\', from) >= 0) {
            int index = js.indexOf('\\', from);
            js = js.substring(0, index) + "\\\\" + js.substring(index + 1);
            from = index + 2;
        }
        js = js.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n").replaceAll("\\'", "\\\\'");

        String s = XStreamUtil.loadResource("/esprimaIterator.js");
        String esprima = "var parsed = esprima.parse(\"" + js + "\");";
        synchronized (engine) {
            engine.eval("var append_xmeta = " + components + ";");

            engine.eval(esprima);

            engine.eval(s);
            engine.eval("var thisFunctions = getFirstLevelFunctions(parsed);");
            return thisFunctions("X", engine.eval("thisFunctions").toString());
        }
    }

    public static synchronized String instrumentController(String js, String jsName,
                                                           Set<String> boundVars, Map<String, XModalBind> boundModals, boolean isModal, boolean isGlobal,
                                                           String htmlStruct, String componentSruct, XResourceManager.JsResource resInfo, ServletContext ctx) throws IOException, ScriptException, XHTMLParsingException {
        js = prepareInjections(js, boundModals, ctx, null);
        int from = 0;
        while (js.indexOf('\\', from) >= 0) {
            int index = js.indexOf('\\', from);
            js = js.substring(0, index) + "\\\\" + js.substring(index + 1);
            from = index + 2;
        }
        js = js.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n").replaceAll("\\'", "\\\\'");

        String s = XStreamUtil.loadResource("/esprimaIterator.js");
        String esprima = "var parsed = esprima.parse(\"" + js + "\");";
        synchronized (engine) {
            engine.eval("var append_xmeta = " + components + ";");

            engine.eval(esprima);

            engine.eval(s);
            engine.eval("var newScript = \"" + js + "\";");
            engine.eval("var thisFunctions = getFirstLevelFunctions(parsed);");
            String[] existingVariables = engine.eval("getFirstLevelVariables(parsed);").toString().split(" ");
            StringBuilder boundVarDeclaration = new StringBuilder();
            if (boundVars != null) {
                if (existingVariables != null) {
                    for (String existingVar : existingVariables) {
                        boundVars.remove(existingVar.trim());
                    }
                }
                for (String boundVar : boundVars) {
                    if (!boundVar.trim().equals("")) {
                        boundVarDeclaration.append("var ").append(boundVar).append(";\n");
                    }
                }
                if (boundVarDeclaration.length() > 0) {
                    boundVarDeclaration.insert(0, "//undeclared vars\n");
                    boundVarDeclaration.append(" //\n\n\n");
                }
            }
            String controllerObject = "function(xInstance){\nvar X=xInstance;\n" +
                    "var setInterval=X._interval;\nvar setTimeout=X._timeout;\nvar clearInterval=X._clearInterval;\nvar clearTimeout=X._clearTimeout;\n\n"
                    + boundVarDeclaration + engine.eval("newScript").toString() + "\n\n\n\n\n"
                    + thisFunctions("this", engine.eval("thisFunctions").toString()) + ";\nthis.resourceName='" + jsName
                    + "';\nthis.isModal=" + isModal + ";\n";
            if (resInfo.getHtmx() != null && resInfo.getHtmx().getRelativePath().endsWith(".modal.htmx")) {
                controllerObject += "_xthis=this;function closeModal(){X.closeMsg(_xthis._id_modal);};this.closeModal = closeModal;\n";
            }
            if (isGlobal) {
                controllerObject += "window." + parseGlovalVarName(jsName) + "=this;\n";
            }
            controllerObject += "this._x_eval = function(f){return eval(f)};\nfunction _(id){return X._(id)};\nthis._=_;}";

            //TODO remove this after fixing js esprima bugs
            controllerObject = controllerObject.replace("X.defineProperty(, ", "X.defineProperty(this, ");
            //

            if (isGlobal) {
                return "(function (){\nvar _load = function(){var xInstance = new _XClass();" +
                        "X$._onScript(" + htmlStruct + ", " + controllerObject + ", " + componentSruct + ", xInstance, " +
                        "function(){console.log(\"Global resource " + jsName + " imported.\")}, null, '" + jsName + "');};\n" +
                        "if(window.addEventListener) {" +
                        "\nwindow.addEventListener('load',_load,false);" +
                        "\n} else if(window.attachEvent) {" +
                        "\nwindow.attachEvent('onload', _load);\n}})();";
            } else {
                return "X$.register(" + htmlStruct + ", " + componentSruct + ", '" + jsName + "', "
                        + controllerObject + ");";
            }
        }
    }

    //Takes the script name and turns it into a variable name
    private static String parseGlovalVarName(String jsName) {
        String[] split = jsName.split("/");
        String result = split[split.length - 1].split("\\.")[0];
        int index;
        while ((index = result.indexOf('-')) >= 0) {
            result = result.substring(0, index) + result.substring(index + 1, index + 2).toUpperCase() + result.substring(index + 2);
        }
        return result.startsWith("_") ? result.substring(1) : result;
    }

    private static boolean checkAnnotationToVar(String name, String[] lines, int currentIndex) {
        if (lines[currentIndex].trim().startsWith("//" + name + ":")) {
            return currentIndex < lines.length && lines[currentIndex + 1].trim().startsWith("var ");
        }
        return false;
    }

    private static boolean checkAnnotationToFunction(String name, String[] lines, int currentIndex) {
        if (lines[currentIndex].trim().startsWith("//" + name + ":")) {
            return currentIndex < lines.length && lines[currentIndex + 1].trim().startsWith("function ");
        }
        return false;
    }

    private static boolean checkAnnotationToInject(String name, String[] lines, int currentIndex) {
        return lines[currentIndex].trim().startsWith("//" + name + ":");
    }

    /**
     * Prepare injections of services, imports, modals and properties
     *
     * @param js          the js content
     * @param boundModals the modals bound to this file
     * @param ctx
     * @return Returns the js prepared
     * @throws IOException
     * @throws XHTMLParsingException
     */
    private static String prepareInjections(String js, Map<String, XModalBind> boundModals, ServletContext ctx, StringBuilder xbinds)
            throws IOException, XHTMLParsingException, ScriptException {
        StringBuilder result = new StringBuilder();
        boolean appendXBinds = false;
        if (xbinds == null) {
            xbinds = new StringBuilder();
            appendXBinds = true;
        }
        Map<String, String[]> propertiesMap = new HashMap<String, String[]>();
        String[] lines = js.split("\n");
        String line = null;
        int countVars = 0;
        int countImports = 0;
        for (int i = 0; i < lines.length; i++) {
            boolean isAnnot = false;
            line = lines[i];
            Annotation annotation = checkAnnotation(lines, i);
            if (annotation != null) {
                if (annotation.equals(Annotation.service)) {
                    //prepares service injection from js annotations (//service:pathtoservice)
                    String nextLine = lines[++i];
                    String[] values = parseVar("//service:", line, nextLine);
                    xbinds.append(values[0]).append(" = X.bindService('").append(values[1]).append("');\n");
                    result.append("var ").append(values[0]).append(";\n");
                    countVars++;
                    if (values[2] != null) {
                        result.append(values[2]).append("\n");
                    }
                    isAnnot = true;
                } else if (annotation.equals(Annotation.importJs)) {
                    //prepares import injection from js annotations (//import:pathtojs)
                    String nextLine = lines[++i];
                    String[] values = parseVar("//import:", line, nextLine);
                    xbinds.append("X.import('").append(values[1])
                            .append(".p.js', function(o){o.CTX=X.CTX;" + values[0] + "=o;__chord.notify();});\n");
                    result.append("var ").append(values[0]).append(";\n");
                    countVars++;
                    countImports++;
                    if (values[2] != null) {
                        result.append(values[2]).append("\n");
                    }
                    isAnnot = true;
                } else if (annotation.equals(Annotation.modal)) {
                    //prepares modal injection from js annotation (//modal:path,parameters)
                    String nextLine = lines[++i];
                    String[] values = parseVar("//modal:", line, nextLine);
                    String[] params = values[1].split(",");
                    boolean toggle = params.length > 2 && params[2].equalsIgnoreCase("toggle");
                    xbinds.append("X.modalS('").append(params[0].trim()).append("',").append(toggle).append(",'")
                            .append(params[1].trim()).append("', function(o){" + values[0] + "=o;__chord.notify();});\n");
                    result.append("var ").append(values[0]).append(";\n");
                    countVars++;
                    countImports++;
                    if (values[2] != null) {
                        result.append(values[2]).append("\n");
                    }
                    isAnnot = true;
                } else if (annotation.equals(Annotation.inject)) {
                    //prepares code injection from js annotation (//inject:path)
                    String scriptPath = "/pages" + line.substring("//inject:".length()) + ".js";
                    byte[] jsBytesToInject = XFileUtil.instance.readFromDisk(scriptPath, null,
                            ctx);
                    if (jsBytesToInject == null) {
                        throw new ScriptException("Invalid script path to inject: " + scriptPath);
                    }
                    String jsToInject = prepareInjections(new String(jsBytesToInject), boundModals, ctx, xbinds);
                    result.append("// start of ").append(scriptPath).append("\n");
                    result.append(jsToInject).append("\n");
                    result.append("// end of ").append(scriptPath).append("\n");
                    isAnnot = true;
                } else {
                    if (annotation.equals(Annotation.getter) || annotation.equals(Annotation.setter)) {
                        //prepares getter or setter function from js annotation (//getter:propertyName function fn(){...)
                        String nextLine = lines[++i];
                        result.append(nextLine).append("\n");
                        String[] values = parseFunction("//" + annotation + ":", line, nextLine);
                        if (values != null) {
                            String[] getterSetter = propertiesMap.get(values[0]);
                            if (getterSetter == null) {
                                getterSetter = new String[2];
                                propertiesMap.put(values[0], getterSetter);
                            }
                            String fnName = values[1];
                            if (annotation.equals(Annotation.getter)) {
                                //getter in pos 0
                                getterSetter[0] = fnName;
                            } else {
                                //setter in pos 1
                                getterSetter[1] = fnName;
                            }
                            isAnnot = true;
                        }
                    }
                }
            }
            if (!isAnnot) {
                result.append(line).append("\n");
            }

        }
        if (boundModals != null) {
            for (Map.Entry<String, XModalBind> e : boundModals.entrySet()) {
                boolean toggle = e.getValue().isToggle();
                xbinds.append("X.modalS('").append(e.getValue().getPath()).append("',").append(toggle).append(",'")
                        .append(e.getValue().getElementId()).append("', function(o){ "
                        + e.getValue().getVarName() + "=o;__chord.notify();});\n");
                result.append("var ").append(e.getValue().getVarName()).append(";\n");
                countVars++;
                countImports++;
            }
        }
        StringBuilder propertiesDeclaration = new StringBuilder();
        if (!propertiesMap.isEmpty()) {
            for (Map.Entry<String, String[]> e : propertiesMap.entrySet()) {
                propertiesDeclaration.append("\nX.defineProperty(this, '")
                        .append(e.getKey()).append("', ")
                        .append(e.getValue()[0]).append(",").append(e.getValue()[1]).append(");\n");
            }
        }
        result.append(propertiesDeclaration);
        // if (elementIdWithModal != null) {
        // int countModal = 0;
        // for (String[] modalElementId : elementIdWithModal) {
        // String varName = "_xmodal_var_" + countModal++;
        // xbinds.append("X.modalS('").append(modalElementId[1]).append("',").append(true).append(",'")
        // .append(modalElementId[0]).append("', function(o){" + varName +
        // "=o;__chord.notify();});\n");
        // result.append("var ").append(varName).append(";\n");
        // countVars++;
        // countImports++;
        // }
        // }
        if (appendXBinds) {
            if (countVars > 0) {
                return "var __xbinds__ = {length:" + countImports + ", __exec: function(__chord){\n" + xbinds
                        + "\n;if(__chord.getCount() == 0){__chord.getOnFinishFunction()()}}}\n" + result.toString();
            } else {
                return "var __xbinds__ = null;\n\n" + result.toString();
            }
        }
        return result.toString();
    }

    private static Annotation checkAnnotation(String[] lines, int i) {
        if (checkAnnotationToVar("service", lines, i)) {
            return Annotation.service;
        } else if (checkAnnotationToVar("import", lines, i)) {
            return Annotation.importJs;
        } else if (checkAnnotationToVar("modal", lines, i)) {
            return Annotation.modal;
        } else if (checkAnnotationToFunction("getter", lines, i)) {
            return Annotation.getter;
        } else if (checkAnnotationToFunction("setter", lines, i)) {
            return Annotation.setter;
        } else if (checkAnnotationToInject("inject", lines, i)) {
            return Annotation.inject;
        }
        return null;
    }

    /*
     * Returns [property name, function name]
     */
    private static String[] parseFunction(String fn, String line, String nextLine) {
        int indexEndFunctionName = nextLine.indexOf("(");
        if (indexEndFunctionName > 0 && nextLine.trim().startsWith("function ")) {

            return new String[]{line.substring(fn.length()).trim(), nextLine.substring(0, indexEndFunctionName).split(" ")[1]};
        }
        return null;
    }

    /*
     * Returns [var name, annotation parameters, nextline (var declaration)]
     */
    private static String[] parseVar(String fn, String line, String nextLine) {
        int index = nextLine.indexOf(";");
        String newNextLine = null;
        if (index > 0) {
            newNextLine = nextLine.substring(index + 1);
            nextLine = nextLine.substring(0, index);
        }
        String varName = nextLine.split(" ")[1];
        return new String[]{varName, line.substring(fn.length()).trim(), newNextLine};
    }

    private static String thisFunctions(String var, String eval) {
        String[] split = eval.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (String fn : split) {
            if (fn != null && !fn.trim().equals("")) {
                sb.append(var + "." + fn + " = " + fn + ";");
            }
        }
        return sb.toString();
    }

    public static synchronized boolean validate(String js) {
        String validation = "var valid=true;try{esprima.parse(\"" + js.replace("\"", "\\\"") + "\");}catch(e){valid=false}";
        synchronized (engine) {
            try {
                engine.eval(validation);
            } catch (ScriptException e) {
                return false;
            }
            Object result = engine.get("valid");
            return (Boolean) result;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println(validate("a||\"\""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
