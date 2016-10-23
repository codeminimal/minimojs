package br.com.kodeless.minimaljs.parser;

public class XAttribute {

    private String name;

    private String value;

    private char deliminitator;

    public XAttribute(String name, String value) {
        this.name = name.toLowerCase().trim().replaceAll("\n", "");
        this.setValue(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        deliminitator = '"';
        if (value != null) {
            value = value.trim();
            if (value.startsWith("\"") || value.startsWith("'")) {
                deliminitator = value.charAt(0);
                value = value.substring(1, value.length() - 1);
            } else {
                value = value.replace("\"", "\\\"");
            }
        }
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public char getDeliminitator() {
        return deliminitator;
    }

    @Override
    public String toString() {
        return name + (value != null ? "=" + deliminitator + value + deliminitator : "");
    }

    public String toJSON() {
        return "'" + name + "':" + (value != null ? deliminitator + value + deliminitator : "null");
    }

}
