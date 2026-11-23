package Folha_Pagamentos;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

// Uma rota da API: recebe a requisição e devolve o que vai virar JSON
@FunctionalInterface
interface Rota {
	Object tratar(Requisicao r) throws Exception;
}

// Resposta com status e cookie diferentes do padrão (200, sem cookie)
record Resposta(int status, Object corpo, String cookie) {
	static Resposta criada(Object corpo) {
		return new Resposta(201, corpo, null);
	}

	static Resposta ok(Object corpo, String cookie) {
		return new Resposta(200, corpo, cookie);
	}
}

// Dados de uma requisição já lidos: parâmetros, partes do caminho, empresa logada e conexão
final class Requisicao {
	final HttpExchange troca;
	final Connection conn;
	final Map<String, String> params;
	final List<String> grupos;
	final Integer empresaId;
	final Clock relogio;
	final String token;

	Requisicao(HttpExchange troca, Connection conn, Map<String, String> params, List<String> grupos, Integer empresaId,
			Clock relogio, String token) {
		this.troca = troca;
		this.conn = conn;
		this.params = params;
		this.grupos = grupos;
		this.empresaId = empresaId;
		this.relogio = relogio;
		this.token = token;
	}

	String texto(String nome) {
		String v = params.get(nome);
		return v == null ? "" : v.trim();
	}

	String obrigatorio(String nome, String rotulo) {
		String v = texto(nome);
		if (v.isEmpty()) {
			throw ErroNegocio.invalido(nome, "Campo obrigatório: " + rotulo);
		}
		return v;
	}

	double numero(String nome) {
		try {
			return Leitor.numeroBR(texto(nome));
		} catch (NumberFormatException e) {
			throw ErroNegocio.invalido(nome, "Número inválido.");
		}
	}

	int inteiro(String nome) {
		try {
			return Integer.parseInt(texto(nome));
		} catch (NumberFormatException e) {
			throw ErroNegocio.invalido(nome, "Número inválido.");
		}
	}

	int grupo(int i) {
		return Integer.parseInt(grupos.get(i));
	}

	LocalDate hoje() {
		return LocalDate.now(relogio);
	}
}

// Recebe tudo que começa com /api/, acha a rota certa e escreve a resposta em JSON
final class Roteador implements HttpHandler {

	private record Entrada(String metodo, Pattern padrao, boolean exigeLogin, Rota rota) {
	}

	private final String urlBanco;
	private final Sessoes sessoes;
	private final Clock relogio;
	private final List<Entrada> rotas = new ArrayList<>();

	Roteador(String urlBanco, Sessoes sessoes, Clock relogio) {
		this.urlBanco = urlBanco;
		this.sessoes = sessoes;
		this.relogio = relogio;
	}

	void get(String regex, Rota r) {
		rotas.add(new Entrada("GET", Pattern.compile(regex), true, r));
	}

	void post(String regex, Rota r) {
		rotas.add(new Entrada("POST", Pattern.compile(regex), true, r));
	}

	void put(String regex, Rota r) {
		rotas.add(new Entrada("PUT", Pattern.compile(regex), true, r));
	}

	void delete(String regex, Rota r) {
		rotas.add(new Entrada("DELETE", Pattern.compile(regex), true, r));
	}

	void publico(String metodo, String regex, Rota r) {
		rotas.add(new Entrada(metodo, Pattern.compile(regex), false, r));
	}

	@Override
	public void handle(HttpExchange troca) throws IOException {
		int status = 200;
		Object corpo;
		String cookie = null;
		try {
			String metodo = troca.getRequestMethod();
			String caminho = troca.getRequestURI().getPath();
			Map<String, String> params = lerParametros(troca);
			String token = lerCookie(troca, "sessao");
			Integer empresaId = sessoes.empresaDo(token);

			Entrada achada = null;
			List<String> grupos = new ArrayList<>();
			for (Entrada e : rotas) {
				Matcher m = e.padrao().matcher(caminho);
				if (e.metodo().equals(metodo) && m.matches()) {
					achada = e;
					for (int i = 1; i <= m.groupCount(); i++) {
						grupos.add(m.group(i));
					}
					break;
				}
			}

			if (achada == null) {
				status = 404;
				corpo = erro("Rota não encontrada.", null);
			} else if (achada.exigeLogin() && empresaId == null) {
				status = 401;
				corpo = erro("Faça login para continuar.", null);
			} else {
				try (Connection conn = Banco.abrir(urlBanco)) {
					Object resultado = achada.rota()
							.tratar(new Requisicao(troca, conn, params, grupos, empresaId, relogio, token));
					if (resultado instanceof Resposta r) {
						status = r.status();
						corpo = r.corpo();
						cookie = r.cookie();
					} else {
						corpo = resultado;
					}
				}
			}
		} catch (ErroNegocio e) {
			status = e.status();
			corpo = erro(e.getMessage(), e.campo());
		} catch (Exception e) {
			e.printStackTrace();
			status = 500;
			corpo = erro("Erro interno: " + e.getMessage(), null);
		}

		byte[] bytes = Json.escrever(corpo).getBytes(StandardCharsets.UTF_8);
		troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		troca.getResponseHeaders().set("Cache-Control", "no-store");
		if (cookie != null) {
			troca.getResponseHeaders().add("Set-Cookie", cookie);
		}
		troca.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = troca.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static Map<String, Object> erro(String mensagem, String campo) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("erro", mensagem);
		if (campo != null) {
			m.put("campo", campo);
		}
		return m;
	}

	private static Map<String, String> lerParametros(HttpExchange troca) throws IOException {
		Map<String, String> params = new HashMap<>();
		separar(troca.getRequestURI().getRawQuery(), params);
		byte[] corpo = troca.getRequestBody().readAllBytes();
		if (corpo.length > 0) {
			separar(new String(corpo, StandardCharsets.UTF_8), params);
		}
		return params;
	}

	private static void separar(String texto, Map<String, String> params) {
		if (texto == null || texto.isEmpty()) {
			return;
		}
		for (String par : texto.split("&")) {
			if (par.isEmpty()) {
				continue;
			}
			int i = par.indexOf('=');
			String chave = i < 0 ? par : par.substring(0, i);
			String valor = i < 0 ? "" : par.substring(i + 1);
			params.put(URLDecoder.decode(chave, StandardCharsets.UTF_8), URLDecoder.decode(valor, StandardCharsets.UTF_8));
		}
	}

	private static String lerCookie(HttpExchange troca, String nome) {
		for (String linha : troca.getRequestHeaders().getOrDefault("Cookie", List.of())) {
			for (String parte : linha.split(";")) {
				String p = parte.trim();
				if (p.startsWith(nome + "=")) {
					return p.substring(nome.length() + 1);
				}
			}
		}
		return null;
	}
}
