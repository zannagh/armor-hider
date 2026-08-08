package de.zannagh.armorhider.smoke.paper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deliberately tiny JSON reader/writer.
 *
 * <p>The smoke project has no dependencies beyond JUnit, and adding Gson here purely to read three
 * fields out of the PaperMC API (and to assert on the plugin's config file) would be a poor trade.
 * Objects become {@link LinkedHashMap}, arrays {@link ArrayList}, numbers {@link Double}.</p>
 */
final class Json {

    private final String source;
    private int cursor;

    private Json(String source) {
        this.source = source;
    }

    /** Parses a complete JSON document. */
    static Object parse(String text) {
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.cursor != text.length()) {
            throw new IllegalArgumentException("Trailing content at offset " + parser.cursor);
        }
        return value;
    }

    /** Casts to a JSON object, or throws with the offending value in the message. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object but got: " + value);
        }
        return (Map<String, Object>) value;
    }

    /** Casts to a JSON array, or throws with the offending value in the message. */
    @SuppressWarnings("unchecked")
    static List<Object> array(Object value) {
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("Expected a JSON array but got: " + value);
        }
        return (List<Object>) value;
    }

    /** Follows a chain of object keys, returning {@code null} as soon as one is missing. */
    static Object path(Object root, String... keys) {
        Object current = root;
        for (String key : keys) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = object(current).get(key);
        }
        return current;
    }

    /** Quotes and escapes a string for embedding in a JSON document. */
    static String quote(String raw) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
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
        return out.append('"').toString();
    }

    private Object readValue() {
        char c = peek();
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            cursor++;
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            result.put(key, readValue());
            skipWhitespace();
            char c = source.charAt(cursor++);
            if (c == '}') {
                return result;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Expected , or } at offset " + (cursor - 1));
            }
        }
    }

    private List<Object> readArray() {
        List<Object> result = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            cursor++;
            return result;
        }
        while (true) {
            skipWhitespace();
            result.add(readValue());
            skipWhitespace();
            char c = source.charAt(cursor++);
            if (c == ']') {
                return result;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Expected , or ] at offset " + (cursor - 1));
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = source.charAt(cursor++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char escape = source.charAt(cursor++);
            switch (escape) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    out.append((char) Integer.parseInt(source.substring(cursor, cursor + 4), 16));
                    cursor += 4;
                }
                default -> out.append(escape);
            }
        }
    }

    private Object readLiteral(String literal, Object value) {
        if (!source.startsWith(literal, cursor)) {
            throw new IllegalArgumentException("Bad literal at offset " + cursor);
        }
        cursor += literal.length();
        return value;
    }

    private Double readNumber() {
        int start = cursor;
        while (cursor < source.length() && "+-.eE0123456789".indexOf(source.charAt(cursor)) >= 0) {
            cursor++;
        }
        return Double.valueOf(source.substring(start, cursor));
    }

    private void expect(char expected) {
        if (peek() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " at offset " + cursor);
        }
        cursor++;
    }

    private char peek() {
        if (cursor >= source.length()) {
            throw new IllegalArgumentException("Unexpected end of JSON input");
        }
        return source.charAt(cursor);
    }

    private void skipWhitespace() {
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }
    }
}
