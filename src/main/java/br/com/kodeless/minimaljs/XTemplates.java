package br.com.kodeless.minimaljs;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;
import javax.servlet.ServletContext;

public class XTemplates {

	private static final String XDEFAULT_TEMPLATE_PROPERTY = "_xdefaultTemplate";
	private static final String XDEFAULT_TEMPLATE_PROPERTY_REF = "_xdefaultTemplateRef";

	protected static Map<String, String> webTemplateIndex = new HashMap<String, String>();

	protected static Map<String, String> webTemplate = new HashMap<String, String>();

	protected static String getTemplate(String templateName, boolean isIndex) throws IOException {
		String xbody = "{xbody}";
		String resultHTML = "";
		if (templateName != null) {
			String template = (isIndex ? webTemplateIndex : webTemplate).get(templateName);
			int index = template.indexOf(xbody);
			if(index < 0){
				throw new RuntimeException("Invalid template " + templateName + ". No {xbody} found");
			}
			resultHTML = template.substring(0, index) + "<xbody/>" + template.substring(index + xbody.length());
		}
		return resultHTML;
	}

	public static String getTemplateName(String html, String defaultTemplateName, ServletContext ctx,
			boolean isIndexPage) throws IOException {
		String templateName = defaultTemplateName;
		int indexTemplateInfo = html.indexOf("<template-info ");
		if (indexTemplateInfo >= 0) {
			String info = html.substring(indexTemplateInfo, html.indexOf(">", indexTemplateInfo));
			int indexPath = info.indexOf("path=") + 5;
			String template = info.substring(indexPath);
			if (template.startsWith("\"")) {
				template = template.substring(1, template.indexOf("\"", 1));
			} else if (template.startsWith("'")) {
				template = template.substring(1, template.indexOf("'", 1));
			} else {
				throw new RuntimeException("Invalid template tag");
			}
			templateName = template;
		}
		if (templateName != null && ((!webTemplate.containsKey(templateName) && !isIndexPage)
				|| (!webTemplateIndex.containsKey(templateName) && isIndexPage))) {
			InputStream is = ctx.getResourceAsStream("/templates/" + templateName);
			if (is == null) {
				throw new IOException("Invalid template name " + templateName);
			}
			String template = XStreamUtil.inputStreamToString(is);
			if (isIndexPage) {
				webTemplateIndex.put(templateName, template);
			} else {
				webTemplate.put(templateName, template);
			}

		}
		return templateName;
	}

	protected static String applyScriptForPopup(String html) throws IOException {
		return html + "<script type=\"text/javascript\">var scr = document.createElement('script');"
				+ "scr.src = window.location.pathname + '.js';"
				+ "scr.onload = function(){if(window.onInit){onInit();}};" + "document.body.appendChild(scr);"
				+ "</script></body>";

	}

	protected static String popupModalTemplate(String templateName, ServletContext ctx) throws IOException {
		InputStream is = ctx.getResourceAsStream("/templates/" + templateName);
		return preparePopupModalTemplate(is);
	}

	protected static String getFrameworkPopupModalTemplate(ServletContext ctx) throws IOException {
		InputStream is = XTemplates.class.getResourceAsStream("/modal_template.html");
		return preparePopupModalTemplate(is);
	}

	private static String preparePopupModalTemplate(InputStream is) throws IOException {
		return XStreamUtil.inputStreamToString(is).replaceAll("\n", "").replaceAll("'", "\\\\'");
	}

	protected static String templateScripts(String templateName, ServletContext ctx)
			throws IOException, ScriptException {
		InputStream is;
		if (templateName == null) {
			is = XTemplates.class.getResourceAsStream("/templates.js");
		} else {
			is = ctx.getResourceAsStream("/templates/" + templateName);
		}
		String scr = XStreamUtil.inputStreamToString(is);
		return "\n(function(){" + scr + ";" + XJS.getFirstLevelFunctions(scr) + "})();";
	}

	public static byte[] loaderImg(String property, ServletContext ctx) throws IOException {
		InputStream is;
		if (property == null) {
			is = XTemplates.class.getResourceAsStream("/loader.gif");
		} else {
			is = ctx.getResourceAsStream(property);
		}
		return XStreamUtil.inputStreamToByteArray(is);
	}

	public static String replaceVars(String strResponse, ServletContext ctx) {
		return strResponse.replaceAll("\\{webctx\\}", ctx.getContextPath());
	}

	protected static String preparePopupModalTemplates(String popupModalTemplates, ServletContext ctx)
			throws IOException {
		StringBuilder result = new StringBuilder();

		boolean usesFrameworkTemplate = false;
		String[] modalTemplatesArray;
		if (popupModalTemplates == null || popupModalTemplates.trim().equals("")) {
			modalTemplatesArray = new String[] { XDEFAULT_TEMPLATE_PROPERTY };
			usesFrameworkTemplate = true;
		} else {
			modalTemplatesArray = popupModalTemplates.split(",");
		}
		for (String tpl : modalTemplatesArray) {
			if (!tpl.trim().equals("")) {
				String propertyName = tpl;
				String templateBody;
				if (propertyName.equals(XDEFAULT_TEMPLATE_PROPERTY)) {
					templateBody = XTemplates.getFrameworkPopupModalTemplate(ctx);
				} else {
					templateBody = XTemplates.popupModalTemplate(tpl, ctx);
				}
				result.append("'").append(propertyName).append("':'").append(templateBody).append("',");
			}
		}
		if (!usesFrameworkTemplate) {
			String propertyName = modalTemplatesArray[0];
			result.append("'").append(XDEFAULT_TEMPLATE_PROPERTY_REF).append("':'").append(propertyName).append("',");
		}
		return result.toString();
	}
}
