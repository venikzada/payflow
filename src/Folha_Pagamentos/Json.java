package Folha_Pagamentos;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

// Transforma Map, List, texto, número e booleano em JSON (sem biblioteca externa)
public final class Json {

	private Json() {
	}

	public static String escrever(Object v) {
		StringBuilder sb = new StringBuilder();
		escrever(sb, v);
		return sb.toString();
	}

	private static void escrever(StringBuilder sb, Object v) {
		if (v == null) {
			sb.append("null");
		} else if (v instanceof String s) {
			texto(sb, s);
		} else if (v instanceof Boolean b) {
			sb.append(b);
		} else if (v instanceof Double || v instanceof Float) {
			double d = ((Number) v).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				sb.append("null");
			} else {
				sb.append(BigDecimal.valueOf(d).stripTrailingZeros().toPlainString());
			}
		} else if (v instanceof Number n) {
			sb.append(n);
		} else if (v instanceof Map<?, ?> m) {
			sb.append('{');
			boolean primeiro = true;
			for (Map.Entry<?, ?> e : m.entrySet()) {
				if (!primeiro) {
					sb.append(',');
				}
				primeiro = false;
				texto(sb, String.valueOf(e.getKey()));
				sb.append(':');
				escrever(sb, e.getValue());
			}
			sb.append('}');
		} else if (v instanceof Collection<?> c) {
			sb.append('[');
			boolean primeiro = true;
			for (Object o : c) {
				if (!primeiro) {
					sb.append(',');
				}
				primeiro = false;
				escrever(sb, o);
			}
			sb.append(']');
		} else {
			texto(sb, v.toString());
		}
	}

	private static void texto(StringBuilder sb, String s) {
		sb.append('"');
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			switch (ch) {
			case '"' -> sb.append("\\\"");
			case '\\' -> sb.append("\\\\");
			case '\n' -> sb.append("\\n");
			case '\r' -> sb.append("\\r");
			case '\t' -> sb.append("\\t");
			default -> {
				if (ch < 0x20) {
					sb.append(String.format("\\u%04x", (int) ch));
				} else {
					sb.append(ch);
				}
			}
			}
		}
		sb.append('"');
	}
}
