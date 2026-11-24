/**
 * Json
 * ----
 * Tiny JSON string helpers used by the HTTP server to build responses.
 * No external libraries; just enough to safely encode strings and numbers.
 */
public class Json {

    /** Escape a string for inclusion inside a JSON string literal. */
    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** Format a double as a JSON number without a trailing ".0" for whole values. */
    public static String num(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d) && !Double.isNaN(d)) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }
}
