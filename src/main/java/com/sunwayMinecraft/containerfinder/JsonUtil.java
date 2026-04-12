package com.sunwayMinecraft.containerfinder;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON serializer for report output.
 */
public final class JsonUtil {
    private JsonUtil() {}

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        appendJson(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void appendJson(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }

        if (value instanceof String s) {
            sb.append('"').append(escape(s)).append('"');
            return;
        }

        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
            return;
        }

        if (value instanceof Map<?, ?> map) {
            sb.append("{");
            Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> entry = it.next();
                sb.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(":");
                appendJson(sb, entry.getValue());
                if (it.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("}");
            return;
        }

        if (value instanceof List<?> list) {
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                appendJson(sb, list.get(i));
                if (i + 1 < list.size()) {
                    sb.append(",");
                }
            }
            sb.append("]");
            return;
        }

        sb.append('"').append(escape(String.valueOf(value))).append('"');
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}