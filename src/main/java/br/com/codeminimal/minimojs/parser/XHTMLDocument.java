package br.com.codeminimal.minimojs.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XHTMLDocument extends XElement implements Cloneable {

    protected List<XElement> requiredResourcesList = new ArrayList<XElement>();
    private XElement htmlElement;

    public XHTMLDocument() {
        super("DOCUMENT", null);
        this.root = this;
    }

    @Override
    public String toString() {
        if (!requiredResourcesList.isEmpty()) {
            XElement bodyEl = null;
            XElement headEl = null;
            List<XElement> children = this.getElements();
            for (XElement e : children) {
                if (e.getName().equalsIgnoreCase("html")) {
                    List<XElement> childrenHtml = e.getElements();
                    boolean foundHead = false;
                    for (XElement ce : childrenHtml) {
                        if (ce.getName().equalsIgnoreCase("body")) {
                            bodyEl = ce;
                        } else if (ce.getName().equalsIgnoreCase("head")) {
                            headEl = ce;
                            foundHead = true;
                        }

                    }
                    if (!foundHead) {
                        headEl = new XElement("head", this);
                        e.insertChild(headEl, 0);
                    }
                }
            }
            if (bodyEl != null || headEl != null) {
                for (XElement e : requiredResourcesList) {
                    String source = e.getAttribute("src").trim();
                    if (source.toLowerCase().endsWith(".js")) {
                        XElement scriptEl;
                        if (bodyEl != null) {
                            scriptEl = bodyEl.addElement("script");
                        } else {
                            scriptEl = headEl.addElement("script");
                        }
                        scriptEl.setAttribute("src", "{webctx}/res/" + source);
                        scriptEl.setAttribute("type", "text/javascript");
                    } else if (source.toLowerCase().endsWith("css") && headEl != null) {
                        XElement linkEl = headEl.addElement("link");
                        linkEl.setAttribute("href", "{webctx}/res/" + source);
                        if (e.getAttribute("rel") != null) {
                            linkEl.setAttribute("rel", e.getAttribute("rel"));
                        }
                        if (e.getAttribute("media") != null) {
                            linkEl.setAttribute("media", e.getAttribute("media"));
                        }
                    }
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        for (XNode n : this.getChildren()) {
            sb.append(n.toString());
        }
        return sb.toString().trim();
    }

    @Override
    public void addChild(XNode node) {
        if (node instanceof XElement && ((XElement) node).getName().equalsIgnoreCase("html")) {
            this.htmlElement = (XElement) node;
        }
        super.addChild(node);
    }

    public List<XElement> getRequiredResourcesList() {
        return requiredResourcesList;
    }

    public String getHtmlStructure() {
        StringBuffer sb = new StringBuffer();
        for (XNode n : this.getChildren()) {
            sb.append(n.toString());
        }
        return sb.toString().trim();
    }

    public void replaceAllTexts(TextReplacer tr) {
        for (XText e : this.getAllTextNodes()) {
            e.setText(tr.replace(e.getText()));
        }
    }

    public void renameAllAttributesWithName(String name, String newName) {
        for (XElement e : this.getAllElements()) {
            e.renameAttribute(name, newName);
        }
    }

    public static interface TextReplacer {
        String replace(String text);
    }

    public XElement getHtmlElement() {
        return htmlElement;
    }

    public String getHTML(Map<String, Object> jsonDynAtt, Map<String, Map<String, Object>> jsonHiddenAtt, Map<String, String> jsonComp) {
        StringBuffer sb = new StringBuffer();
        for (XNode n : this.getChildren()) {
            sb.append(n.getHTML(jsonDynAtt, jsonHiddenAtt, jsonComp));
        }
        return sb.toString().trim();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("XhtmlDocument should be able to clone", e);
        }
    }
}
