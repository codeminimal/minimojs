package br.com.kodeless.minimojs.parser;

import br.com.kodeless.minimojs.XFileUtil;
import br.com.kodeless.minimojs.XJS;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XHTMLParser {

    private char[] ca;

    private int i;

    XNode current;

    XElement currentParent = null;

    int lastAdvance;

    XElement currentRequires = null;

    Set<String> boundObjects = new HashSet<String>();

    Map<String, XModalBind> boundModals = new HashMap<String, XModalBind>();

    private List<XText> textNodes = new ArrayList<XText>();

    private boolean isCheckingHtmlElement;

    boolean foundHtml = false;

    StringBuilder currentLine = new StringBuilder();

    private List<XElement> templateScriptLlist = new ArrayList<XElement>();

    public XHTMLDocument parse(String html) throws XHTMLParsingException {
        ca = (html + "\n").toCharArray();
        XHTMLDocument doc = new XHTMLDocument();
        currentParent = doc;
        boolean inScript = false;
        StringBuffer currentScript = null;
        while (hasMore()) {
            String templateIfScript;
            String[] templateForScript;
            if (!inScript && nextIs("$if") && (templateIfScript = isIfTemplateScript()) != null) {
                //if template script eg: $if(exp){
                XElement hiddenIteratorElement = new XElement("xiterator", doc);
                hiddenIteratorElement.setAttribute("count", "(" + templateIfScript + ")?1:0");
                currentParent.addChild(hiddenIteratorElement);
                currentParent = hiddenIteratorElement;
                current = null;
                templateScriptLlist.add(hiddenIteratorElement);
                advanceLine();
            } else if (!inScript && nextIs("$for") && (templateForScript = isForTemplateScript()) != null) {
                //if template script eg: $if(exp){
                XElement hiddenIteratorElement = new XElement("xiterator", doc);
                hiddenIteratorElement.setAttribute("list", templateForScript[1]);
                hiddenIteratorElement.setAttribute("var", templateForScript[0]);
                if (templateForScript[2] != null) {
                    hiddenIteratorElement.setAttribute("indexvar", templateForScript[2]);
                }
                currentParent.addChild(hiddenIteratorElement);
                currentParent = hiddenIteratorElement;
                current = null;
                templateScriptLlist.add(hiddenIteratorElement);
                advanceLine();
            } else if (!templateScriptLlist.isEmpty() && isEndOfTemplateScript()) {
                //end of template script eg: }
                XElement hiddenIteratorElement = templateScriptLlist.remove(templateScriptLlist.size() - 1);
                advanceLine();
                closeTag("xiterator");
            } else if (!inScript && isCurrentTextOnlyTag()) {
                readTilCloseTag();
            } else if (!inScript && nextIs("<!--")) {
                advance();
                inComment();
            } else if (!inScript && nextIs("<![")) {
                advance();
                inComment();
            } else if (!inScript && nextIs("</")) {
                advance();
                close();
            } else if (!inScript && !nextIs("< ") && nextIs("<")) {
                advance();
                inTag(doc);
            } else {
                if (!inScript && nextIs("${")) {
                    inScript = true;
                    currentScript = new StringBuffer();
                } else if (inScript && nextIs("}") && XJS.validate(currentScript.toString().substring(2))) {
                    inScript = false;
                    currentScript = null;
                }
                char currentChar = read();
                if (inScript) {
                    currentScript.append(currentChar);
                }
            }
            if (foundHtml && isCheckingHtmlElement) {
                return null;
            }
        }
        while (!currentParent.isClosed() && currentParent != doc) {
            currentParent.setNotClosed();
            currentParent = currentParent.getParent();
        }
        for (XText text : textNodes) {
            text.normalize(doc);
        }
        return doc;
    }

    private void advanceLine() {
        readTill("\n").toLowerCase();
    }

    static Pattern patternIf1 = Pattern.compile("^\\$if\\s{0,}\\((.*?)\\)\\s{0,}\\{$");
    static Pattern patternIf2 = Pattern.compile("^\\$if\\s{1,}(.*?)[^\\)]\\s{0,}\\{$");

    private String isIfTemplateScript() {
        String line = getFullCurrentLine().trim();
        Matcher matcher = patternIf1.matcher(line);
        if (matcher.find()) {
            String val = matcher.group(1);
            return val;
        } else {
            matcher = patternIf2.matcher(line);
            if (matcher.find()) {
                String val = matcher.group(1);
                return val;
            }
        }
        return null;
    }

    static Pattern patternFor1 = Pattern.compile("^\\$for\\s{0,}\\((.*?)\\)\\s{0,}\\{$");
    static Pattern patternFor2 = Pattern.compile("^\\$for\\s{1,}(.*?)[^\\)]\\s{0,}\\{$");
    static Pattern patternForVariables = Pattern.compile("(\\S*?)\\s{1,}in\\s{1,}(\\S*)(\\s{1,}with\\s{1,}(\\S*))?");

    private String[] isForTemplateScript() {
        String line = getFullCurrentLine().trim();
        Matcher matcher = patternFor1.matcher(line);
        String variables = null;
        if (matcher.find()) {
            variables = matcher.group(1);
        } else {
            matcher = patternFor2.matcher(line);
            if (matcher.find()) {
                variables = matcher.group(1);
            }
        }
        if (variables != null) {
            matcher = patternForVariables.matcher(variables);
            if (matcher.find()) {
                String[] val = {matcher.group(1), matcher.group(2), matcher.group(4)};
                return val;
            }
        }
        return null;
    }

    private boolean isEndOfTemplateScript() {
        String line = getFullCurrentLine().trim();
        return line.equals("}");
    }

    private void readTilCloseTag() throws XHTMLParsingException {
        String tagName = currentParent.getName();
        StringBuffer sb = new StringBuffer();
        int j = i;
        while (true) {
            if (ca[j] == '<' && ca[j + 1] == '/') {
                int h = j + 2;
                char c;
                StringBuilder sbName = new StringBuilder();
                while (ca.length > h && (c = ca[h++]) != '>') {
                    sbName.append(c);
                }
                if (sbName.toString().trim().equals(tagName)) {
                    i = j + 2;
                    XText text = new XText();
                    textNodes.add(text);
                    text.setText(sb.toString());
                    currentParent.addChild(text);
                    close();
                    return;
                }
            }
            sb.append(ca[j++]);
        }
    }

    private void inTag(XHTMLDocument doc) {
        String name = readTill(" ", ">", "/>", "\n", "\t").toLowerCase();
        if (isCheckingHtmlElement && name.equalsIgnoreCase("html")) {
            foundHtml = true;
            return;
        }
        XElement element = new XElement(name, doc);
        Map<String, XModalBind> modalBindMap = new HashMap<String, XModalBind>();
        boolean isRequiresTag = false;
        if (name.equalsIgnoreCase("requires")) {
            doc.requiredResourcesList.add(element);
            currentRequires = element;
            isRequiresTag = true;
        } else {
            currentParent.addChild(element);
            currentParent = element;
            current = element;
        }
        StringBuffer attVal = new StringBuffer();
        int dynAttr = 0;
        while (true) {
            if (discard(' ')) {
                if (attVal.toString().trim().length() > 0) {
                    element.setAttribute(attVal.toString().trim(), null);
                }
                attVal = new StringBuffer();
            }
            if (nextIs("/>")) {
                advance();
                if (attVal.length() > 0) {
                    element.setAttribute(attVal.toString(), null);
                }
                if (isRequiresTag) {
                    currentRequires.close();
                    currentRequires = null;
                } else {
                    closeElement();
                }
                break;
            } else if (nextIs(">")) {
                advance();
                if (attVal.length() > 0) {
                    element.setAttribute(attVal.toString(), null);
                }
                current = null;
                break;
            }
            if (nextIs("${")) {
                advance();
                StringBuffer script = new StringBuffer();
                while (!nextIs("}") && XJS.validate(script.toString())) {
                    read(script);
                }
                discard('}');
                element.setAttribute("_outxdynattr_" + dynAttr++, script.toString());
            } else {

                char s = read(attVal);
                if (s == '=') {
                    String attName = attVal.substring(0, attVal.length() - 1).trim();
                    attVal = new StringBuffer();
                    char c = ca[i];
                    if (c == '\'' || c == '"' || c != ' ') {
                        s = c;
                        read(attVal);
                        boolean aspas = c == '\'' || c == '"';
                        while (true) {
                            c = read(attVal);
                            boolean endNoAspas = (!aspas && c == ' ')
                                    || (!aspas && ((c == '/' && ca[i + 1] == '>') || c == '>'));
                            if (endNoAspas || (aspas && c == s && previous(2) != '\\')) {
                                String val;
                                if (endNoAspas) {
                                    i--;
                                    val = attVal.substring(0, attVal.length() - 1);
                                } else {
                                    val = attVal.toString();
                                }
                                element.setAttribute(attName, val);
                                if (attName.equals("data-xbind")) {
                                    String bind = element.getAttribute(attName).trim();
                                    String varName = bind.split("\\.")[0];
                                    if (!varName.equals("window") && !varName.equals("xuser")) {
                                        boundObjects.add(varName.split("\\[")[0]);
                                    }
                                } else if (attName.startsWith("data-xmodal") && !attName.equals("data-xmodal-toggle")) {
                                    XModalBind modalBind = new XModalBind();
                                    if (attName.startsWith("data-xmodal-")) {// has
                                        // a
                                        // bound
                                        // var
                                        modalBind.setVarName(attName.substring("data-xmodal-".length()));
                                    } else {
                                        modalBind.setVarName("xvmd_" + ((int) (Math.random() * 99999)));
                                    }
                                    modalBind.setPath(element.getAttribute(attName).trim());
                                    modalBindMap.put(modalBind.getVarName(), modalBind);
                                }
                                attVal = new StringBuffer();
                                break;
                            }
                        }
                    } else {
                        element.setAttribute(attName, null);
                    }
                }
            }
        }
        if (!modalBindMap.isEmpty()) {
            String elementId = element.getAttribute("id");
            if (elementId == null) {
                elementId = "xmd_" + ((int) (Math.random() * 99999));
                element.setAttribute("id", elementId);
            }
            String toggle = element.getAttribute("data-xmodal-toggle");
            if (toggle != null) {
                XModalBind bind = modalBindMap.get(toggle);
                if (bind != null) {
                    bind.setToggle(true);
                }
            }
            for (Map.Entry<String, XModalBind> e : modalBindMap.entrySet()) {
                if (modalBindMap.size() == 1) {
                    e.getValue().setToggle(true);
                }
                e.getValue().setElementId(elementId);
            }
            boundModals.putAll(modalBindMap);
        }
        prepareElementsWithSource(element);
    }

    private void prepareElementsWithSource(XElement element) {
        if (element.getName().toUpperCase().equals("SCRIPT")) {
            String src = element.getAttribute("src");
            if (src != null && src.startsWith("/")) {
                element.setAttribute("src", "{webctx}" + src);
            }
        }
        if (element.getName().toUpperCase().equals("A")) {
            String href = element.getAttribute("href");
            if (href != null && href.startsWith("/")) {
                element.setAttribute("href", "{webctx}" + href);
            }
        }
    }

    private boolean isCurrentTextOnlyTag() {
        // put text only tag here
        String textOnlyTags = " script ";
        return currentParent != null && textOnlyTags.contains(currentParent.getName());
    }

    private char previous(int t) {
        return ca[i - t];
    }

    private boolean discard(char c) {
        int j = i;
        boolean discarded = false;
        while (ca[j] == c) {
            j++;
            discarded = true;
        }
        i = j;
        return discarded;
    }

    private void close() throws XHTMLParsingException {
        String tagName = null;
        try {
            tagName = readTill(">").toLowerCase().trim();
            closeTag(tagName);
        } catch (Exception e) {
            throw new XHTMLParsingException("Error closing tag " + (tagName != null ? tagName : ""));
        }
    }

    private void closeTag(String tagName) {
        if (tagName.equalsIgnoreCase("requires")) {
            currentRequires.close();
            currentRequires = null;
        } else {
            XElement toClose = current instanceof XElement ? (XElement) current : currentParent;
            while (!tagName.equals(toClose.getName())) {
                if (!toClose.isClosed()) {
                    toClose.setNotClosed();
                }
                XNode prev = toClose;
                while (true) {
                    prev = prev.getPrevious();
                    if (prev == null) {
                        toClose = toClose.getParent();
                        break;
                    } else if (prev instanceof XElement && !((XElement) prev).isClosed()) {
                        toClose = (XElement) prev;
                        break;
                    }
                }
            }
            toClose.close();
            currentParent = toClose.getParent();

            current = null;
        }
        i++;
    }

    private void closeElement() {
        currentParent = current.getParent();
        current.close();
        current = null;
    }

    private String readTill(String... s) {
        StringBuffer sb = new StringBuffer();
        int j = i;
        main:
        while (true) {
            for (int z = 0; z < s.length; z++) {
                if (nextIs(s[z], j)) {
                    break main;
                }
            }
            sb.append(ca[j++]);
        }
        lastAdvance = j - i;
        advance();
        return sb.toString();
    }

    private void inComment() {
        current = new XComment();
        currentParent.addChild(current);
        while (true) {
            if (nextIs("-->")) {
                advance();
                current.close();
                current = null;
                break;
            }
            read();
        }
    }

    private void advance() {
        i += lastAdvance;
    }

    private boolean nextIs(String s) {
        return nextIs(s, null);
    }

    private boolean nextIs(String s, Integer index) {
        StringBuffer sb = new StringBuffer();
        lastAdvance = s.length();
        int usedIndex = index == null ? i : index;
        int j = usedIndex;
        for (; j < s.length() + usedIndex && j < ca.length; j++) {
            sb.append(ca[j]);
        }
        return sb.toString().equals(s);
    }

    // private void rewind(int t) {
    // i -= t;
    // }

    private boolean hasMore() {
        return i < ca.length;
    }

    private char read() {
        return read(null);
    }

    private char read(StringBuffer sb) {
        char c = ca[i++];
        if (c == '\n') {
            //starting new line
            currentLine = new StringBuilder();
        }
        if (sb == null) {
            if (current == null) {
                current = new XText();
                textNodes.add((XText) current);
                currentParent.addChild(current);
            }
            current.addChar(c);
        } else {
            sb.append(c);
        }
        currentLine.append(c);
        return c;
    }

    private String getFullCurrentLine() {
        int localIndex = i;
        StringBuilder line = new StringBuilder(currentLine);
        char c;
        while (localIndex < ca.length - 1 && (c = ca[localIndex++]) != '\n') {
            line.append(c);
        }
        return line.toString();
    }

    public static void main(String[] args) {
        try {
            XHTMLParser parser = new XHTMLParser();
            XHTMLDocument doc = parser
                    .parse(XFileUtil.instance.readFile("/Users/eduardo/work/eclipseworkspaces/xloja/Testes/teste.html"));

            // System.out.println("html " + doc.getHtmlElement());
            System.out.println(doc.toJson());

            // parser = new XHTMLParser();
            // doc = parser.parse("<!DOCTYPE html><html></html>");
            // System.out.println("html " + doc.getHtmlElement());
            // System.out.println(doc.toString());
            //
            // parser = new XHTMLParser();
            // doc = parser.parse("<body>test</body>");
            // System.out.println("html " + doc.getHtmlElement());
            // System.out.println(doc.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<String> getBoundObjects() {
        return boundObjects;
    }

    public Map<String, XModalBind> getBoundModals() {
        return boundModals;
    }

    public boolean hasHtmlElement(String content) throws XHTMLParsingException {
        isCheckingHtmlElement = true;
        parse(content);
        return foundHtml;
    }
}
