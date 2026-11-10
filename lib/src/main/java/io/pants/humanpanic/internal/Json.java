package io.pants.humanpanic.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Minimal pretty-printing JSON writer for the value types a crash report contains: {@link Map}
 * (with String keys), {@link Collection}, {@link CharSequence}, {@link Number}, {@link Boolean} and
 * {@code null}.
 *
 * <p>The library writes its own JSON instead of depending on Jackson, so it has no runtime
 * dependencies that could conflict with the host application's versions while it is handling a
 * crash. Not public API.
 */
public final class Json {

    private Json() {
    }

    /**
     * Serialises a value as indented JSON.
     *
     * @param value the value tree
     * @return the JSON text
     * @throws IllegalArgumentException if the tree contains an unsupported type or a non-finite number
     */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder(4096);
        write(sb, value, 0);
        return sb.append('\n').toString();
    }

    private static void write(StringBuilder sb, Object value, int indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof CharSequence) {
            string(sb, value.toString());
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Number) {
            number(sb, (Number) value);
        } else if (value instanceof Map) {
            object(sb, (Map<?, ?>) value, indent);
        } else if (value instanceof Collection) {
            array(sb, (Collection<?>) value, indent);
        } else {
            throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass().getName());
        }
    }

    private static void number(StringBuilder sb, Number n) {
        if ((n instanceof Double && !Double.isFinite(n.doubleValue()))
                || (n instanceof Float && !Float.isFinite(n.floatValue()))) {
            throw new IllegalArgumentException("Non-finite number cannot be written as JSON: " + n);
        }
        sb.append(n);
    }

    private static void object(StringBuilder sb, Map<?, ?> map, int indent) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> e = it.next();
            if (!(e.getKey() instanceof String)) {
                throw new IllegalArgumentException("JSON object keys must be strings: " + e.getKey());
            }
            pad(sb, indent + 1);
            string(sb, (String) e.getKey());
            sb.append(" : ");
            write(sb, e.getValue(), indent + 1);
            sb.append(it.hasNext() ? ",\n" : "\n");
        }
        pad(sb, indent);
        sb.append('}');
    }

    private static void array(StringBuilder sb, Collection<?> items, int indent) {
        if (items.isEmpty()) {
            sb.append("[ ]");
            return;
        }
        sb.append("[ ");
        Iterator<?> it = items.iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            write(sb, it.next(), indent);
        }
        sb.append(" ]");
    }

    private static void pad(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
    }

    private static void string(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20 || c == ' ' || c == ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
