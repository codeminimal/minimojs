package br.com.kodeless.minimaljs;

import org.apache.log4j.Logger;

import javax.script.ScriptException;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by eduardo on 8/21/16.
 */
public enum XScriptManager {
    instance;

    private Logger logger = Logger.getLogger(XScriptManager.class);

    protected static String globalScript;

    private String remoteScripts;

    private String scripts;

    private String[] modulesToAdd = {"xcomponents", "xdefaultservices", "xdom", "xevents", "xinputs", "xlog", "xmask", "xobj",
            "xremote", "xutil", "xvisual", "xautocomplete", "xresources"};

    public void reload(ServletContext ctx, Properties properties, String metaClasses, String resourceInfoMap) throws IOException {
        String mainScript = XFileUtil.instance.getResource("/x/x.js");

        StringBuilder modules = new StringBuilder();
        for (String md : modulesToAdd) {
            modules.append(getScriptModule(md));
        }

        mainScript = replaceString(mainScript, "%xmodulescripts%", modules.toString());

        mainScript = replaceString(mainScript, "%resourceInfoMap%", resourceInfoMap);

        String defaultDateFormat = properties.getProperty("defaultdateformat");
        defaultDateFormat = defaultDateFormat == null ? "" : defaultDateFormat;
        mainScript = replaceAllStrings(mainScript, "%defaultdateformat%", defaultDateFormat);

        String defaultDateTimeFormat = properties.getProperty("defaultdatetimeformat");
        defaultDateTimeFormat = defaultDateTimeFormat == null ? "" : defaultDateTimeFormat;
        mainScript = replaceAllStrings(mainScript, "%defaultdatetimeformat%", defaultDateTimeFormat);

        String defaultTimeFormat = properties.getProperty("defaulttimeformat");
        defaultTimeFormat = defaultTimeFormat == null ? "" : defaultTimeFormat;
        mainScript = replaceAllStrings(mainScript, "%defaulttimeformat%", defaultTimeFormat);

        logger.debug("Initializing js scripts...");
        mainScript = mainScript.replaceAll("%meta%", metaClasses).replaceAll("%ctx%", ctx.getContextPath());

        String currencyFormatter = "";
        if (properties.getProperty("currency.decimal.separator") != null) {
            currencyFormatter += "\n_default_decimal_separator = '"
                    + properties.getProperty("currency.decimal.separator") + "';\n";
        }
        if (properties.getProperty("currency.thousand.separator") != null) {
            currencyFormatter += "\n_default_thousand_separator = '"
                    + properties.getProperty("currency.thousand.separator") + "';\n";
        }
        mainScript = replaceString(mainScript, "%currency_formatter%", currencyFormatter);

        mainScript = replaceString(mainScript, "%xdevmode%", String.valueOf(XContext.isDevMode()));

        //mainScript = replaceString(mainScript, "%isspa%", String.valueOf("true".equalsIgnoreCase(properties.getProperty("spa"))));

        logger.debug("Applying templates...");

        mainScript = replaceString(mainScript, "%popupmodaltemplates%",
                XTemplates.preparePopupModalTemplates(properties.getProperty("popup.modal.templates"), ctx));
        String jsComponent = XComponents.getJsComponents();

        logger.debug("Initializing XComponents...");
        mainScript = replaceString(mainScript, "%xcomponents%", jsComponent);
        String debugFlags = properties.getProperty("js.debug.flags");
        if (debugFlags != null) {
            String[] splitDebugFlags = debugFlags.split(",");
            debugFlags = "";
            for (String debug : splitDebugFlags) {
                debugFlags += "'" + debug.trim() + "',";
            }
            mainScript = replaceString(mainScript, "%debug_flags%", debugFlags);
        } else {
            mainScript = replaceString(mainScript, "%debug_flags%", "");
        }

        logger.debug("Initializing js scripts template...");
        try {
            globalScript = XTemplates.templateScripts(properties.getProperty("golbal.script"), ctx);
        } catch (ScriptException e) {
            logger.fatal("Error reading global script", e);
            System.exit(1);
        }

        // TODO ver https
        this.remoteScripts = mainScript.replaceAll("%sitedomain%", "http://" + properties.getProperty("sitedomain"))
                .replaceAll("%is_remote%", "true").replaceAll("%global_script%", globalScript);

        mainScript = mainScript.replaceAll("%sitedomain%", "").replaceAll("%is_remote%", "false");

        if (properties.getProperty("load.jquery") != null
                && properties.getProperty("load.jquery").equalsIgnoreCase("true")) {
            scripts = XFileUtil.instance.getResource("/thirdparty/jquery-1.6.1.min.js")
                    + XFileUtil.instance.getResource("/thirdparty/jquery.maskedinput.min.js")
                    + XFileUtil.instance.getResource("/thirdparty/jquery.priceformat.1.7.min.js") + mainScript;
        } else {
            scripts = mainScript;
        }
    }

    private String getScriptModule(String name) throws IOException {
        return "var " + name
                + " = addModule(function(xInstance){\nvar thisX = xInstance;var X = thisX;\n\t\t\tvar thisModule = this; \n\t\t\tfunction _expose(fn, name){xexpose(thisModule, fn, false, name);};"
                + "\n\t\t\tfunction _external(fn, name){xexpose(thisModule, fn, true, name);};\n"
                + XFileUtil.instance.getResource("/x/" + name + ".js") + "\n\t\t});";
    }


    private String replaceString(String str, String patternReplace, String newStr) {
        int index = str.indexOf(patternReplace);
        return str.substring(0, index) + newStr + str.substring(index + patternReplace.length());
    }

    private String replaceAllStrings(String str, String patternReplace, String newStr) {
        while (str.indexOf(patternReplace) >= 0) {
            str = replaceString(str, patternReplace, newStr);
        }
        return str;
    }

    public String getRemoteScript() {
        return remoteScripts;
    }

    public String getScript() {
        return scripts;
    }
}
