package br.com.kodeless.minimaljs.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class XNode implements Cloneable {

    protected StringBuffer buffer = new StringBuffer();

    protected XElement parent;

    protected Map<String, String> hiddenAttributes = new HashMap<String, String>();

    protected Map<String, Object> tempAttributes = new HashMap<String, Object>();

    public XNode getNext() {
        int index = this.getParent().getChildren().indexOf(this);
        if (index == this.getParent().getChildren().size() - 1) {
            return null;
        }
        return this.getParent().getChildren().get(index + 1);
    }

    public XNode getPrevious() {
        int index = this.getParent().getChildren().indexOf(this);
        if (index == 0) {
            return null;
        }
        return this.getParent().getChildren().get(index - 1);
    }

    public void addAfter(XNode node) {
        int index = this.getParent().getChildren().indexOf(this);
        if (index == this.getParent().getChildren().size() - 1) {
            this.getParent().addChild(node);
        } else {
            this.getParent().insertChildren(node, index + 1);
        }
    }

    public void addBefore(XNode node) {
        int index = this.getParent().getChildren().indexOf(this);
        if (index == 0) {
            this.getParent().insertChildren(node, 0);
        } else {
            this.getParent().insertChildren(node, index);
        }
    }

    public XElement getParent() {
        return parent;
    }

    public void addChar(char c) {
        buffer.append(c);
    }

    public void addString(String s) {
        buffer.append(s);
    }

    public abstract void close();

    @Override
    public String toString() {
        return buffer.toString();
    }

    public abstract String toJson();

    protected String prepareValueInXScript(String value, boolean escape) {
        if (value != null) {
            Pattern pattern = Pattern.compile("\\$\\{(?:(?!\\$\\{|}).)*}");
            Matcher matcher;
            while ((matcher = pattern.matcher(value)).find()) {
                String var = matcher.group().trim();
                var = var.substring(2, var.length() - 1).replaceAll("\"", "&quot;");

                String newValue = value.substring(0, matcher.start());

                newValue += "<xscript data-xscript=\"" + var + "\"></xscript>";
                newValue += value.substring(matcher.end());
                value = newValue;
            }
        }
        return value;
    }

    public void remove() {
        this.getParent().getChildren().remove(this);
        this.parent = null;
    }

    protected String printHiddenAttributesInJsonFormat() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : hiddenAttributes.entrySet()) {
            sb.append(e.getKey()).append(":'").append(e.getValue().replace("'", "\\\\'")).append("',");
        }
        return sb.toString();
    }

    public void setHiddenAttribute(String attr, String val) {
        if (!hiddenAttributes.containsKey(attr)) {
            if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
                val = val.substring(1, val.length() - 1);
            }
            hiddenAttributes.put(attr, val);
        }
    }

    public String getHiddenAttribute(String name) {
        return hiddenAttributes.get(name);
    }

    public boolean hasHiddenAttributes() {
        return !hiddenAttributes.isEmpty();
    }

    public Object getTempAttribute(String name) {
        return tempAttributes.get(name);
    }

    public void setTempAttribute(String name, Object value) {
        tempAttributes.put(name, value);
    }

    public abstract String getHTML(Map<String, Object> jsonDynAtt, Map<String, Map<String, Object>> jsonHiddenAtt,
                                   Map<String, String> jsonComp);

}
