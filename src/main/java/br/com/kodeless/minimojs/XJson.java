package br.com.kodeless.minimojs;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class XJson {

    private static Map<String, Class<?>> typeAliasMap = new HashMap<String, Class<?>>();

    private static Map<String, DateFormat> dateFormats = new HashMap<String, DateFormat>();

    private static DateFormat defaultDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static Gson gson = new Gson();

    public static Object parse(String json, Class<?> cl) {
        JsonParser parser = new JsonParser();
        try {
            return parseElement(parser.parse(json), cl, null);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing json " + json + ".", e);
        }
    }

    @SuppressWarnings("all")
    private static Object parseArray(JsonArray array, Type type)
            throws Exception {
        Object result = null;
        if (type instanceof ParameterizedType) {
            Class c = (Class) ((ParameterizedType) type).getRawType();
            if (c.isAssignableFrom(Set.class)) {
                result = new HashSet();
            } else if (c.isAssignableFrom(List.class)
                    || c.equals(Collection.class)) {
                result = new ArrayList();
            }
            for (JsonElement element : array) {
                Object item = parseElement(element,
                        ((ParameterizedType) type).getActualTypeArguments()[0],
                        null);
                ((Collection<Object>) result).add(item);
            }
        } else {
            Class c = ((Class) type);
            if (c.isArray()) {
                result = Array.newInstance(c.getComponentType(), array.size());
                int i = 0;
                for (JsonElement element : array) {
                    Object item = parseElement(element, c.getComponentType(),
                            null);
                    Array.set(result, i++, item);
                }
            } else {
                if (c.getName().equals("java.util.List")
                        || c.getName().equals("java.util.Collection")) {
                    result = new ArrayList();
                } else if (c.getName().equals("java.util.Map")) {
                    result = new HashMap();
                } else {
                    // TODO if is interface throw error
                    result = c.newInstance();
                }
                Type typeParameter = Object.class;
                if (type instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) type;
                    typeParameter = pt.getActualTypeArguments()[0];
                }
                int i = 0;
                for (JsonElement element : array) {
                    Object item = parseElement(element, typeParameter, null);
                    ((Collection<Object>) result).add(item);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("all")
    private static Object parseArray(JsonArray array) throws Exception {
        List<Object> result = new ArrayList<Object>();
        for (JsonElement element : array) {
            Object item = parseSimpleValue(element);
            result.add(result);
        }
        return result;
    }

    @SuppressWarnings("all")
    private static Object parseElement(JsonElement element, Type type,
                                       Field field) throws Exception {
        Object result = null;
        if (element.isJsonArray()) {
            result = parseArray(element.getAsJsonArray(), type);
        } else if (element.isJsonPrimitive()) {
            result = parsePrimitive(element.getAsJsonPrimitive(), type, field);
        } else if (element.isJsonObject()) {
            result = parseObject(element.getAsJsonObject(), type);
        }
        return result;
    }

    @SuppressWarnings("all")
    private static Object parseObject(JsonObject o, Type type) throws Exception {
        if (o.isJsonNull()) {
            return null;
        }
        Object result = null;
        JsonElement xtype = o.get("xtype");
        boolean isMap = false;
        Class c;
        if (xtype != null) {
            c = typeAliasMap.get(xtype.getAsString());
        } else if (type == null || type.equals(Object.class)) {
            c = HashMap.class;
            isMap = true;
        } else if (type instanceof ParameterizedType) {
            c = (Class) ((ParameterizedType) type).getRawType();
            if (c.equals(Map.class)) {
                c = HashMap.class;
                isMap = true;
            }
        } else {
            c = ((Class) type);
        }
        result = c.newInstance();
        for (Map.Entry<String, JsonElement> entry : o.entrySet()) {

            if (!entry.getKey().equals("xtype")) {
                if (isMap) {
                    ((Map) result).put(entry.getKey(),
                            parseSimpleValue(entry.getValue()));
                } else {
                    Field f = getDeclaredField(c, entry.getKey());
                    if (f == null) {
                        f = getField(c, entry.getKey());
                        if (f == null) {
                            // throw new
                            // RuntimeException("Error parsing json. Field " +
                            // entry.getKey()
                            // + " not found in class " + c.getName());
                        }
                    } else {
                        f.setAccessible(true);
                    }
                    if (f != null && !Modifier.isFinal(f.getModifiers())
                            && !Modifier.isStatic(f.getModifiers())) {
                        f.set(result,
                                parseElement(entry.getValue(),
                                        f.getGenericType(), f));
                    }
                }
            }
        }
        return result;
    }

    private static Object parseSimpleValue(JsonElement element)
            throws Exception {
        Object result = null;
        if (element.isJsonArray()) {
            result = parseArray(element.getAsJsonArray());
        } else if (element.isJsonPrimitive()) {
            result = parsePrimitive(element.getAsJsonPrimitive());
        } else if (element.isJsonObject()) {
            result = parseObject(element.getAsJsonObject(), null);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static Field getDeclaredField(Class c, String name) {
        if (c == null) {
            return null;
        }
        try {
            return c.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return getDeclaredField(c.getSuperclass(), name);
        }
    }

    @SuppressWarnings("rawtypes")
    private static Field getField(Class c, String name) {
        if (c == null) {
            return null;
        }
        try {
            return c.getField(name);
        } catch (NoSuchFieldException e) {
            return getField(c.getSuperclass(), name);
        }
    }

    private static Object parsePrimitive(JsonPrimitive e) {
        if (e.isNumber()) {
            return e.getAsBigDecimal();
        } else if (e.isString()) {
            return e.getAsString();
        } else if (e.isBoolean()) {
            return e.getAsBoolean();
        } else {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parsePrimitive(JsonPrimitive e, Type type, Field field) {
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return e.getAsInt();
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return e.getAsByte();
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return e.getAsCharacter();
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return e.getAsShort();
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return e.getAsLong();
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return e.getAsFloat();
        } else if (type.equals(Double.class) || type.equals(Double.class)) {
            return e.getAsDouble();
        } else if (((Class<?>) type).isEnum()) {
            return Enum.valueOf((Class) type, e.getAsString());
        } else if (type.equals(Date.class)) {
            if (e.isNumber()) {
                return new Date(e.getAsLong());
            } else {
                DateFormat df = null;
                if (field != null) {
                    XJsonDateFormat annot = field
                            .getAnnotation(XJsonDateFormat.class);
                    if (annot != null) {
                        df = dateFormats.get(annot.value());
                        if (df == null) {
                            df = new SimpleDateFormat(annot.value());
                            dateFormats.put(annot.value(), df);
                        }

                    }
                }
                if (df == null) {
                    df = defaultDateFormat;
                }
                try {
                    String strDate = e.getAsString();
                    if (strDate == null || strDate.trim().equals("")) {
                        return null;
                    }
                    return df.parse(strDate);
                } catch (ParseException ex) {
                    throw new RuntimeException("Invalid date format: "
                            + e.getAsString(), ex);
                }
            }
        } else if (e.isString() || type.equals(String.class)) {
            return e.getAsString();
        } else if (e.isBoolean() || type.equals(boolean.class)
                || type.equals(Boolean.class)) {
            return e.getAsBoolean();
        } else if (type.equals(BigInteger.class)
                || type.equals(BigInteger.class)) {
            return e.getAsBigInteger();
        } else if (e.isNumber() || type.equals(BigDecimal.class)) {
            return e.getAsBigDecimal();
        }
        return null;
    }

    public static String toJson(Object obj) {
        return toJson(obj, null);
    }

    public static String toJson(Object obj, String[] ignoreFields) {
        StringBuilder sb = new StringBuilder();
        try {
            field(obj, sb, null, asList(ignoreFields), "");
        } catch (Exception e) {
            throw new RuntimeException("Error parsing to json.", e);
        }
        return sb.toString();
    }

    private static List<String> asList(String[] ignoreFields) {
        if (ignoreFields != null) {
            List<String> list = new ArrayList<String>();
            for (String ignoreField : ignoreFields) {
                list.add("." + ignoreField);
            }
            return list;
        }
        return null;
    }

    @SuppressWarnings("all")
    private static void startObject(Object obj, StringBuilder sb,
                                    List<String> ignoreFields, String currentPath) throws Exception {
        sb.append("{");
        if (obj instanceof Map<?, ?>) {
            for (Iterator entryIterator = ((Map<?, ?>) obj).entrySet()
                    .iterator(); entryIterator.hasNext(); ) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryIterator.next();
                String newField = currentPath + "." + entry.getKey();
                if (!ignore(ignoreFields, newField)) {
                    sb.append('"').append(entry.getKey()).append('"')
                            .append(':');
                    field(entry.getValue(), sb, null, ignoreFields, newField);
                    if (entryIterator.hasNext()) {
                        sb.append(',');
                    }
                }
            }
        } else {
            Class<? extends Object> c = obj.getClass();
            Class<? extends Object> originalClass = c;
            XJsonTypeAlias typeAlias = c.getAnnotation(XJsonTypeAlias.class);
            XJsonDiscard discard = c.getAnnotation(XJsonDiscard.class);
            if (discard != null) {
                for (String path : discard.value()) {
                    if (ignoreFields == null) {
                        ignoreFields = new ArrayList<String>();
                    }
                    ignoreFields.add(currentPath + "." + path);
                }
            }
            boolean wroteField = false;
            Map<String, String> fieldList = new HashMap<String, String>();
            List<String> skippedFieldList = new ArrayList<String>();
            do {
                for (int i = 0; i < c.getFields().length; i++) {
                    Field field = c.getFields()[i];
                    if (field.getAnnotation(XJsonIgnore.class) != null) {
                        skippedFieldList.add(field.getName());
                    } else {
                        String newField = currentPath + "." + field.getName();
                        if (!fieldList.containsKey(field.getName())
                                && (!ignore(ignoreFields, newField))) {
                            StringBuilder fieldSb = new StringBuilder();
                            if (printField(obj, fieldSb, field, ignoreFields,
                                    newField)) {
                                fieldList.put(field.getName(),
                                        fieldSb.toString());
                            }
                        }
                    }
                }

                for (int i = 0; i < c.getDeclaredFields().length; i++) {
                    Field field = c.getDeclaredFields()[i];
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    if (field.getAnnotation(XJsonIgnore.class) != null) {
                        skippedFieldList.add(field.getName());
                    } else {
                        String newField = currentPath + "." + field.getName();
                        if (!fieldList.containsKey(field.getName())
                                && (!ignore(ignoreFields, newField))) {
                            StringBuilder fieldSb = new StringBuilder();
                            if (printField(obj, fieldSb, field, ignoreFields,
                                    newField)) {
                                fieldList.put(field.getName(),
                                        fieldSb.toString());
                            }
                        }
                    }
                }
            } while ((c = c.getSuperclass()) != null);
            c = originalClass;
            do {
                for (PropertyDescriptor propertyDescriptor : Introspector
                        .getBeanInfo(c).getPropertyDescriptors()) {
                    String propertyName = propertyDescriptor.getDisplayName();
                    if (!skippedFieldList.contains(propertyName)
                            && !propertyName.equals("class")
                            && !fieldList.containsKey(propertyName)) {
                        StringBuilder fieldSb = new StringBuilder();
                        String newField = currentPath + "." + propertyName;
                        if (!ignore(ignoreFields, newField)
                                && printField(obj, fieldSb, propertyDescriptor,
                                ignoreFields, newField)) {
                            fieldList.put(propertyName, fieldSb.toString());
                        }
                    }
                }
            } while ((c = c.getSuperclass()) != null);
            for (Iterator<String> iterator = fieldList.values().iterator(); iterator
                    .hasNext(); ) {
                wroteField = true;
                String field = iterator.next();
                sb.append(field);
                if (iterator.hasNext()) {
                    sb.append(',');
                }
            }
            if (typeAlias != null) {
                addTypeAlias(originalClass, typeAlias.value());
                if (wroteField) {
                    sb.append(',');
                }
                sb.append("xtype:'").append(typeAlias.value()).append('\'');
            }
        }
        sb.append("}");
    }

    private static boolean ignore(List<String> ignoreFields, String newPath) {
        if (ignoreFields == null) {
            return false;
        }
        for (Iterator<String> iterator = ignoreFields.iterator(); iterator
                .hasNext(); ) {
            String s = (String) iterator.next();
            if (newPath.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean printField(Object obj, StringBuilder sb,
                                      Field field, List<String> ignoreFields, String currentField)
            throws Exception, IllegalAccessException {
        if (!Modifier.isStatic(field.getModifiers())) {
            sb.append('"').append(field.getName()).append('"').append(':');
            field(field.get(obj), sb, field, ignoreFields, currentField);
            return true;
        }
        return false;
    }

    private static boolean printField(Object obj, StringBuilder sb,
                                      PropertyDescriptor property, List<String> ignoreFields,
                                      String currentField) throws Exception, IllegalAccessException {
        sb.append('"').append(property.getName()).append('"').append(':');
        field(property.getReadMethod().invoke(obj, new Object[0]), sb,
                property, ignoreFields, currentField);
        return true;
    }

    private static void addTypeAlias(Class<? extends Object> cl, String alias) {
        if (!typeAliasMap.containsKey(alias)) {
            typeAliasMap.put(alias, cl);
        }
    }

    private static void field(Object obj, StringBuilder sb, Object field,
                              List<String> ignoreFields, String currentPath) throws Exception {
        if (obj == null) {
            sb.append("null");
        } else {
            Class<?> c = obj.getClass();
            if (XJsonPrinter.class.isAssignableFrom((Class<?>) c)) {
                sb.append(((XJsonPrinter) obj).toJson());
            } else if (boolean.class.equals(c) || Boolean.class.equals(c)
                    || byte.class.equals(c) || Byte.class.equals(c)
                    || short.class.equals(c) || Short.class.equals(c)
                    || int.class.equals(c) || Integer.class.equals(c)
                    || long.class.equals(c) || Long.class.equals(c)
                    || BigInteger.class.equals(c) || float.class.equals(c)
                    || Float.class.equals(c) || double.class.equals(c)
                    || Double.class.equals(c) || BigDecimal.class.equals(c)) {
                sb.append(obj);
            } else if (Date.class.isAssignableFrom((Class<?>) c)) {
                boolean formatted = false;
                if (field != null) {
                    DateFormat df = null;
                    XJsonDateFormat annot = field instanceof Field ? ((Field) field)
                            .getAnnotation(XJsonDateFormat.class) : null;
                    if (annot != null) {
                        df = dateFormats.get(annot.value());
                        if (df == null) {
                            df = new SimpleDateFormat(annot.value());
                            dateFormats.put(annot.value(), df);
                        }
                        formatted = true;
                        sb.append("\"" + df.format((Date) obj) + "\"");
                    }
                }
                if (!formatted) {
                    sb.append("new Date(" + ((Date) obj).getTime() + ")");
                }
            } else if (c.isEnum()) {
                sb.append("\"").append(((Enum<?>) obj).name()).append("\"");
            } else if (char.class.equals(c) || Character.class.equals(c)
                    || String.class.equals(c)) {
                if (obj instanceof String) {
                    obj = gson.toJson(obj);
                }
                sb.append(obj);
            } else if (((Class<?>) c).isArray()) {
                startArray((Object[]) obj, sb, ignoreFields, currentPath);
            } else if (Collection.class.isAssignableFrom((Class<?>) c)) {
                startArray((Iterable<?>) obj, sb, ignoreFields, currentPath);
            } else {
                startObject(obj, sb, ignoreFields, currentPath);
            }
        }
    }

    private static void startArray(Iterable<?> iterable, StringBuilder sb,
                                   List<String> ignoreFields, String currentPath) throws Exception {
        sb.append('[');
        for (Iterator<?> iterator = iterable.iterator(); iterator.hasNext(); ) {
            Object item = iterator.next();
            field(item, sb, null, ignoreFields, currentPath);
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(']');
    }

    private static void startArray(Object[] array, StringBuilder sb,
                                   List<String> ignoreFields, String currentPath) throws Exception {
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            Object item = array[i];
            field(item, sb, null, ignoreFields, currentPath);
            if (i < array.length - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
    }

    @SuppressWarnings("unused")
    static class X {
        private int i = 1;
        private String s = "X";
    }

    @SuppressWarnings("unused")
    @XJsonTypeAlias("TesteClassM")
    static class M {
        @XJsonIgnore
        private String ignore;
        private int i;
        private String s;
        private char c;
        private BigDecimal bd;
        private Map<String, Object> map;
        private String[] sa;
        public boolean bl;

        public M() {
        }

        public M fill() {
            i = 1234;
            s = "TesteTeste";
            c = 'c';
            bd = new BigDecimal(1234);
            map = new HashMap<String, Object>();
            sa = new String[]{"asdf", "qwer"};
            bl = true;
            Map<String, Object> map2 = new HashMap<String, Object>();
            map2.put("booleanKey", false);
            map2.put("xxxx", null);
            map.put("StringKey", "String");
            map.put("BigDecimalKey", new BigDecimal("1234"));
            map.put("MapKey", map2);
            return this;
        }

    }

    @SuppressWarnings("unused")
    @XJsonTypeAlias("TesteClassM2")
    static class M2 extends M {
        private BigInteger bi;
        private List<Object> list;

        public M2() {
        }

        public M2 fill2() {
            bi = new BigInteger("1234");
            list = new ArrayList<Object>();
            list.add(new M());
            list.add(new BigDecimal("1"));
            list.add("ItemList");
            return this;
        }
    }

    @SuppressWarnings("all")
    public static void main(String[] args) {
        String s = "{\"id\":'tabela',\"innerHTML\":'\n			<coluna tamanho=\"30\" titulo=\"Nome\">item.nome</coluna>\n\n			<coluna tamanho=\"60\" titulo=\"Descri??o\">item.descricao</coluna>\n\n			<coluna tamanho=\"10\" titulo=\"A??o\">\n				\'<span onclick=\"gotoEditar(\' + item.id + \');\"/>\n<span onclick=\"excluir(\\'\' + item.nome + \'\\',\' + item.id + \');\"/>\'\n			</coluna>\n\n		',\"tamanhopagina\":'10',\"indexVar\":'i',\"var\":'item',\"colunas\":'coluna'}";
        Map<String, String> m = new HashMap<String, String>();
        String s1 = "			<coluna tamanho=\"30\" titulo=\"Nome\">item.nome</coluna>\n\n			<coluna tamanho=\"60\" titulo=\"Descri??o\">item.descricao</coluna>\n\n			<coluna tamanho=\"10\" titulo=\"A??o\">\n				\'<span onclick=\"gotoEditar(\' + item.id + \');\"/>\n<span onclick=\"excluir(\\'\' + item.nome + \'\\',\' + item.id + \');\"/>\'\n			</coluna>\n\n		";
        String s2 = s1.replaceAll("\\\\", "#:@:#");
        s2 = s2.replaceAll("#:@:#", "\\\\\\\\");
        s2 = s2.replaceAll("\n", "\\\\n");
        s2 = s2.replaceAll("'", "\\\\\'");

        System.out.println(new Gson().toJson(s1));
        m.put("innerHTML", s1);
        s = (String) XJson.toJson(m, null);
        System.out.println(s);

        m = (Map<String, String>) XJson.parse(s, HashMap.class);
        System.out.println(m);
        Object o1 = new M().fill();
        ((M) o1).ignore = "Ignore";
        String js = toJson(o1);
        System.out.println(js);
        Object o2 = parse(js, M.class);
        String js2 = toJson(o2);
        System.out.println(js2);
        ((M) o1).ignore = null;
        if (!XBeanUtil.equals(o1, o2)) {
            throw new RuntimeException("Error 1");
        }

        o1 = new M2().fill2();
        js = toJson(o1);
        System.out.println(js);
        o2 = parse(js, M2.class);
        System.out.println(toJson(o2));
        if (!XBeanUtil.equals(o1, o2)) {
            throw new RuntimeException("Error 2");
        }
        System.out.println(toJson(null));

        System.out.println(toJson(new Object[]{new M(), 1, new M2()}));

        System.out.println(toJson(parse("[1,2]", Object[].class)));
        System.out.println(parse(toJson(new M2()), M2.class));
        System.out.println(parse(toJson(null), Object.class));
        System.out.println(parse(toJson(new Object[]{new M(), 1, new M2()}),
                Object[].class));
    }
}
