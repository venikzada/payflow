package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

class JsonTest {

	@Test
	void escreveMapa() {
		var m = new LinkedHashMap<String, Object>();
		m.put("nome", "João \"Zé\"\nSilva");
		m.put("valor", 1999.31);
		m.put("inteiro", 200.0);
		m.put("ok", true);
		m.put("nada", null);
		m.put("lista", List.of(1, 2));
		assertEquals("{\"nome\":\"João \\\"Zé\\\"\\nSilva\",\"valor\":1999.31,\"inteiro\":200,\"ok\":true,\"nada\":null,\"lista\":[1,2]}",
				Json.escrever(m));
	}

	@Test
	void escapaControle() {
		assertEquals("\"\\u0001\"", Json.escrever("\u0001"));
		assertEquals("\"a\\\\b\"", Json.escrever("a\\b"));
	}
}
