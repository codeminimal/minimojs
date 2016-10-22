package br.com.codeminimal.minimojs.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.codeminimal.minimojs.XComponents;
import br.com.codeminimal.minimojs.XJsonPrinter;

/**
 * @author eduardo
 */
public class XElement extends XNode {

    public static final String NO_END_TAG = "_base_link_meta_hr_br_wbr_area_img_track_embed_param_source_col_input_keygen_";

    private List<XNode> children = new ArrayList<XNode>();

    private Map<String, XAttribute> attributes = new HashMap<String, XAttribute>();

    private String innerText;

    private String tagText;

    private String name;

    private boolean notClosed;

    private boolean isClosed;

    private boolean isComponent;

    private String componentName;

    protected XHTMLDocument root;

    public XElement(String name, XHTMLDocument root) {
        this.root = root;
        this.name = name;
    }

    public List<XElement> getElementsByName(String name) {
        List<XElement> list = new ArrayList<XElement>();
        for (XElement e : getElements()) {
            if (e instanceof XElement) {
                if (((XElement) e).getName().equalsIgnoreCase(name)) {
                    list.add(e);
                }
                list.addAll(e.getElementsByName(name));
            }
        }
        return list;
    }

    public List<XElement> findChildrenByName(String name) {
        List<XElement> list = new ArrayList<XElement>();
        for (XElement e : getElements()) {
            if (e instanceof XElement) {
                if (((XElement) e).getName().equalsIgnoreCase(name)) {
                    list.add(e);
                }
            }
        }
        return list;
    }

    public List<XElement> getAllElements() {
        List<XElement> list = new ArrayList<XElement>();
        for (XElement e : getElements()) {
            list.add(e);
            list.addAll(e.getAllElements());
        }
        return list;
    }

    public List<XText> getAllTextNodes() {
        List<XText> list = new ArrayList<XText>();
        getAllTextNodes(this, list);
        return list;
    }

    private static void getAllTextNodes(XElement element, List<XText> list) {
        for (XNode e : element.getChildren()) {
            if (e instanceof XElement) {
                getAllTextNodes((XElement) e, list);
            } else if (e instanceof XText) {
                list.add((XText) e);
            }
        }
    }

    public List<XElement> getElementsWithAttribute(String name) {
        List<XElement> list = new ArrayList<XElement>();
        for (XElement e : getElements()) {
            if (e instanceof XElement) {
                if (((XElement) e).getAttribute(name) != null) {
                    list.add(e);
                }
                list.addAll(e.getElementsWithAttribute(name));
            }
        }
        return list;
    }

    public XElement addElement(String name) {
        XElement e = new XElement(name, this.root);
        addChild(e);
        return e;
    }

    public void addChildList(List<? extends XNode> list) {
        for (XNode xNode : list) {
            addChild(xNode);
        }
    }

    public void addChild(XNode node) {
        node.parent = this;
        this.children.add(node);
    }

    public void insertChildren(XNode node, int index) {
        node.parent = this;
        this.children.add(index, node);
    }

    public void insertChildAfter(XNode node, XNode after) {
        node.parent = this;
        this.children.add(this.children.indexOf(after) + 1, node);
    }

    public void insertChildBefore(XNode node, XNode before) {
        node.parent = this;
        this.children.add(this.children.indexOf(before), node);
    }

    public void insertChild(XNode node, int index) {
        node.parent = this;
        this.children.add(index, node);
    }

    public List<XNode> getChildren() {
        return children;
    }

    private boolean isEmptyText(XNode node) {
        if (node instanceof XText && !(node instanceof XComment) && ((XText) node).getText().trim().equals("")) {
            return true;
        }
        return false;
    }

    public List<XElement> getElements() {
        List<XElement> result = new ArrayList<XElement>();
        List<XNode> list = this.getChildren();
        for (XNode xNode : list) {
            if (xNode instanceof XElement) {
                result.add((XElement) xNode);
            }
        }
        return result;
    }

    public String getInnerText() {
        return innerText;
    }

    public String getTagText() {
        return tagText;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Collection<XAttribute> getAttributes() {
        return new ArrayList(attributes.values());
    }

    public void setAttribute(String n, String v) {
        if (n.startsWith("_hidden_")) {
            this.setHiddenAttribute(n.substring("_hidden_".length()), v);
        } else {
            XAttribute a = new XAttribute(n, v);
            this.attributes.put(a.getName(), a);
        }
    }

    public String getAttribute(String n) {
        return this.attributes.containsKey(n) ? this.attributes.get(n).getValue() : null;
    }

    public XAttribute getXAttribute(String n) {
        return this.attributes.get(n);
    }

    public String getName() {
        return name;
    }

    public void renameAttribute(String name, String newName) {
        XAttribute value = this.attributes.remove(name);
        this.attributes.put(newName, value);
    }

    public void close() {
        this.isClosed = true;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("<");
        sb.append(name);
        for (XAttribute a : getAttributes()) {
            sb.append(" ").append(a.toString());
        }
        if (!hiddenAttributes.isEmpty()) {
            for (Map.Entry<String, String> e : hiddenAttributes.entrySet()) {
                sb.append(" _hidden_").append(e.getKey()).append("=").append("'")
                        .append(e.getValue().replace("'", "\\'")).append("' ");
            }
        }
        sb.append(">");
        if (!this.notClosed && NO_END_TAG.indexOf("_" + name + "_") < 0) {
            printHTML(sb);
            sb.append("</" + name + ">");
        }
        return sb.toString();
    }

    Pattern patternScript = Pattern.compile("\\$\\{(.*?)}");

    @Override
    public String toJson() {
        if (name.equalsIgnoreCase("xscript")) {
            StringBuilder sb = new StringBuilder();
            XAttribute att = this.getXAttribute("data-xscript");
            sb.append("{x: ").append(att.getDeliminitator()).append(att.getValue()).append(att.getDeliminitator());

            if (hasHiddenAttributes()) {
                String hidden = printHiddenAttributesInJsonFormat();
                sb.append(",h:{");
                sb.append(hidden);
                sb.append("}");
            }
            sb.append("}");
            return sb.toString();
        } else {
            StringBuffer sb = new StringBuffer("{n:'");
            sb.append(name).append("',");
            StringBuilder sbAttr = new StringBuilder();
            for (XAttribute a : getAttributes()) {
                sbAttr.append("'").append(a.getName()).append("':[");
                if (a.getValue() != null) {
                    Matcher m = patternScript.matcher(a.getValue());
                    int index = 0;
                    while (m.find()) {
                        if (index != m.start()) {
                            sbAttr.append("{v:").append(a.getDeliminitator());
                            sbAttr.append(a.getValue().substring(index, m.start()).replace("\n", "\\n"));
                            sbAttr.append(a.getDeliminitator()).append("},");
                        }
                        sbAttr.append("{s:").append(a.getDeliminitator());
                        sbAttr.append(m.group(1).replace("\n", "\\n").replace("&quot;", "\\\""));
                        sbAttr.append(a.getDeliminitator()).append("},");
                        index = m.end();
                    }
                    if (index < a.getValue().length()) {
                        sbAttr.append("{v:").append(a.getDeliminitator());
                        sbAttr.append(a.getValue().substring(index, a.getValue().length()).replace("\n", "\\n"));
                        sbAttr.append(a.getDeliminitator()).append("},");
                    }
                }
                sbAttr.append("],");
            }
            if (sbAttr.length() > 0) {
                sb.append("a:{").append(sbAttr).append("},");
            }
            StringBuilder sbChildren = new StringBuilder();
            for (XNode n : this.getChildren()) {
                String htmlStruct;
                if (!isEmptyText(n) && !(htmlStruct = n.toJson().trim()).equals("")) {
                    sbChildren.append(htmlStruct).append(",");
                }
            }
            if (sbChildren.length() > 0) {
                sb.append("c:[");
                sb.append(sbChildren);
                sb.append("],");
            }
            String hidden = printHiddenAttributesInJsonFormat();
            if (!hidden.equals("")) {
                sb.append("h:{");
                sb.append(hidden);
                sb.append("}");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    public String innerHTML() {
        StringBuffer sb = new StringBuffer();
        printHTML(sb);
        return sb.toString();
    }

    private void printHTML(StringBuffer sb) {
        for (XNode n : this.getChildren()) {
            if (!isEmptyText(n)) {
                sb.append(n.toString());
            }
        }
    }

    public void setNotClosed() {
        if (this.getParent() != null) {
            this.notClosed = true;
            for (XNode n : this.getChildren()) {
                this.getParent().addChild(n);
            }
            this.getChildren().clear();
        }
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void remove() {
        super.remove();
    }

    public void replaceWith(XNode node) {
        this.getParent().insertChildAfter(node, this);
        this.remove();
    }

    public void removeAllChildren() {
        this.children.clear();
    }

    public void addClass(String c) {
        String classes = getAttribute("class");
        if (classes == null || classes.trim().equals("")) {
            this.setAttribute("class", c);
        } else {
            this.setAttribute("class", classes + " " + c);
        }
    }

    public void removeAttributes(String... attributeNames) {
        for (String name : attributeNames) {
            this.attributes.remove(name);
        }
    }

    public void setHiddenAttributeOnChildren(String attr, String val) {
        for (XNode node : getChildren()) {
            node.setHiddenAttribute(attr, val);
            if (node instanceof XElement && !((XElement) node).getName().equals("_x_text_with_attributes")) {
                ((XElement) node).setHiddenAttributeOnChildren(attr, val);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getHTML(Map<String, Object> jsonDynAtt, Map<String, Map<String, Object>> jsonHiddenAtt,
                          Map<String, String> jsonComp) {
        String xcompId = getHiddenAttribute("xcompId");
        if (xcompId != null) {
            // component
            String xcompName = getHiddenAttribute("xcompName");
            jsonComp.put(xcompId, xcompName);
        }
        StringBuffer sb = new StringBuffer("<");
        String dynId = this.getAttribute("data-xdynid");
        if (name.equalsIgnoreCase("xscript")) {
            XAttribute att = this.getXAttribute("data-xscript");
            sb.append("xscript data-xscript=")
                    .append(att.getDeliminitator()).append(att.getValue()).append(att.getDeliminitator());
            if (hasHiddenAttributes()) {
                if (dynId == null) {
                    dynId = XComponents.generateId();
                }
                sb.append(" data-xdynid='").append(dynId).append("' ");
                Map<String, Object> h = new HashMap<String, Object>();
                jsonHiddenAtt.put(dynId, h);
                for (Map.Entry<String, String> e : hiddenAttributes.entrySet()) {
                    h.put(e.getKey(), e.getValue());
                }
            }

            sb.append("></xscript>");
            return sb.toString();
        } else {
            sb.append(name);
            for (XAttribute a : getAttributes()) {
                boolean isDynAtt = false;
                if (a.getValue() != null) {
                    Matcher m = patternScript.matcher(a.getValue());
                    int index = 0;
                    List<Map<String, Object>> attValues = null;
                    while (m.find()) {
                        isDynAtt = true;
                        if (dynId == null) {
                            dynId = XComponents.generateId();
                            sb.append(" data-xdynid='").append(dynId).append("' ");
                        }
                        // get dynamic atts for element
                        Map<String, List<Map<String, Object>>> dynAtts = (Map<String, List<Map<String, Object>>>) jsonDynAtt
                                .get(dynId);
                        if (dynAtts == null) {
                            dynAtts = new HashMap<String, List<Map<String, Object>>>();
                            jsonDynAtt.put(dynId, dynAtts);
                        }
                        // get values of the att
                        attValues = dynAtts.get(a.getName());
                        if (attValues == null) {
                            attValues = new ArrayList<Map<String, Object>>();
                            dynAtts.put(a.getName(), attValues);
                        }
                        if (index != m.start()) {
                            Map<String, Object> valMap = new HashMap<String, Object>();
                            attValues.add(valMap);
                            valMap.put("v", a.getDeliminitator() + a.getValue().substring(index, m.start()).replace("\n", "\\n") + a.getDeliminitator());
                        }
                        Map<String, Object> valMap = new HashMap<String, Object>();
                        attValues.add(valMap);
                        final String attribute = a.getDeliminitator() + m.group(1).replace("\n", "\\n") + a.getDeliminitator();
                        valMap.put("s", new XJsonPrinter() {

                            @Override
                            public String toJson() {
                                return attribute;
                            }
                        });
                        index = m.end();
                    }
                    if (isDynAtt && index < a.getValue().length()) {
                        Map<String, Object> valMap = new HashMap<String, Object>();
                        attValues.add(valMap);
                        valMap.put("v", a.getDeliminitator() + a.getValue().substring(index, a.getValue().length()).replace("\n", "\\n") + a.getDeliminitator());
                    }
                }
                if (!isDynAtt) {
                    sb.append(" ").append(a.toString());
                }
            }
        }

        if (!hiddenAttributes.isEmpty()) {
            Map<String, Object> h = new HashMap<String, Object>();
            dynId = getAttribute("data-xdynid");
            if (dynId == null) {
                dynId = XComponents.generateId();
                sb.append(" data-xdynid='").append(dynId).append("' ");
            }
            jsonHiddenAtt.put(dynId, h);

            for (Map.Entry<String, String> e : hiddenAttributes.entrySet()) {
                h.put(e.getKey(), e.getValue());
            }
        }
        if (!this.notClosed && NO_END_TAG.indexOf("_" + name + "_") < 0) {
            StringBuilder sbChild = new StringBuilder();
            StringBuilder sbHiddenIterators = new StringBuilder();
            int i = 0;
            for (XNode n : this.getChildren()) {
                if (!isEmptyText(n)) {
                    boolean hiddenIterator = n instanceof XElement
                            && ((XElement) n).getName().equalsIgnoreCase("xiterator");
                    if (!hiddenIterator) {
                        sbChild.append(n.getHTML(jsonDynAtt, jsonHiddenAtt, jsonComp));
                    } else {
                        XElement e = (XElement) n;
                        // hidden iterator
                        sbHiddenIterators.append(e.getHiddenAttribute("xiterId")).append(",").append(i).append("|");
                    }
                    i++;
                }
            }
            if (sbHiddenIterators.length() > 0) {
                sb.append(" data-hxiter='").append(sbHiddenIterators).append("'");
            }
            sb.append(">");
            sb.append(sbChild);
            sb.append("</" + name + ">");
        } else {
            sb.append(">");
        }
        return sb.toString();
    }
}
