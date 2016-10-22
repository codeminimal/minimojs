package br.com.codeminimal.minimojs.parser;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

public class XText extends XNode {

    private String text;

    public String getText() {
        return text == null || text.trim().equals("") ? this.buffer.toString() : text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public final void normalize(XHTMLDocument doc) {
        String text = getText();
        if (text == null || text.trim().equals("")) {
            text = "";
            return;
        }
        Pattern p = Pattern.compile("\\$\\{(.*?)}");
        Matcher m = p.matcher(text);
        int index = 0;
        XNode t = new XText();
        this.addAfter(t);
        this.remove();
        while (m.find()) {
            if (index != m.start()) {
                String newText = text.substring(index, m.start());
                if (newText.length() > 0) {
                    XText tNew = new XText();
                    tNew.setText(newText);
                    t.addAfter(tNew);
                    t = tNew;
                }
            }
            XElement x = new XElement("xscript", doc);
            x.setAttribute("data-xscript", m.group(1));
            for (Map.Entry<String, String> e : hiddenAttributes.entrySet()) {
                x.setHiddenAttribute(e.getKey(), e.getValue());
            }
            t.addAfter(x);
            t = x;
            index = m.end();
        }
        if (index < text.length()) {
            String newText = text.substring(index, text.length());
            if (newText.length() > 0) {
                XText tNew = new XText();
                tNew.setText(newText);
                t.addAfter(tNew);
                t = tNew;
            }
        }
    }

    @Override
    public String toString() {
        String textValue = prepareValueInXScript(getText(), this.getParent().getName().equalsIgnoreCase("script"));
        if (!hiddenAttributes.isEmpty()) {
            // hidden attributes
            StringBuilder sbHiddenAttributes = new StringBuilder();
            for (Map.Entry<String, String> e : hiddenAttributes.entrySet()) {
                sbHiddenAttributes.append(" _hidden_").append(e.getKey()).append("=").append("'")
                        .append(e.getValue().replace("'", "\\'")).append("' ");
            }

            // separete xscripts
            XHTMLDocument doc = null;
            try {
                doc = new XHTMLParser().parse("<root>" + textValue + "</root>");
            } catch (XHTMLParsingException e1) {
                throw new RuntimeException("UNKNOWN ERROR IN XTEXT TO STRING", e1);
            }
            StringBuilder sb = new StringBuilder();
            for (XNode child : ((XElement) doc.getChildren().get(0)).getChildren()) {
                if (child instanceof XText) {
                    sb.append(((XText) child).getText());
                } else {
                    XElement element = (XElement) child;
                    if (!element.getName().toLowerCase().equals("xscript")) {
                        throw new RuntimeException(
                                "THERE SHOUDN'T BE A TAG DIFFERENT THAN XSCRIPT INSIDE A XTEXT. TAG: "
                                        + element.getName());
                    }
                    for (Map.Entry<String, String> e : hiddenAttributes.entrySet()) {
                        element.setHiddenAttribute(e.getKey(), e.getValue());
                    }
                    sb.append(element.toString());
                }
            }

            textValue = sb.toString();
        }
        return textValue;
    }

    @Override
    public String toJson() {
        String text = getText();
        if (text == null || text.trim().equals("")) {
            return "";
        } else {
            return "{t:\"" + StringEscapeUtils.unescapeHtml4(text.replace("\"", "\\\"").replace("\n", "")) + "\"}";
        }
    }

    public void close() {
    }

    @Override
    public String getHTML(Map<String, Object> jsonDynAtt, Map<String, Map<String, Object>> jsonHiddenAtt,
                          Map<String, String> jsonComp) {
        String text = getText();
        if (text == null || text.trim().equals("")) {
            return "";
        } else {
            return text;
        }
    }
}
