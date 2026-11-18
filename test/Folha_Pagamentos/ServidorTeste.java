package Folha_Pagamentos;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.sun.net.httpserver.HttpServer;

// Sobe o servidor web numa porta livre, com banco novo e "hoje" fixo em 14/11/2026
final class ServidorTeste implements AutoCloseable {

	static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-14T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

	private final HttpServer servidor;
	private final String url;
	private final String urlBanco;

	ServidorTeste() throws Exception {
		urlBanco = BancoTeste.url(BancoTeste.novoArquivo());
		servidor = Servidor.iniciar(0, urlBanco, Path.of("web"), RELOGIO);
		url = "http://localhost:" + servidor.getAddress().getPort();
	}

	Cliente cliente() {
		return new Cliente(url);
	}

	Connection conexao() throws Exception {
		return DriverManager.getConnection(urlBanco);
	}

	@Override
	public void close() {
		servidor.stop(0);
	}

	record Resp(int status, String corpo, HttpHeaders headers) {
		boolean tem(String trecho) {
			return corpo.contains(trecho);
		}

		int conta(String trecho) {
			int n = 0;
			for (int i = corpo.indexOf(trecho); i >= 0; i = corpo.indexOf(trecho, i + trecho.length())) {
				n++;
			}
			return n;
		}
	}

	static final class Cliente {
		private final String url;
		private final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

		Cliente(String url) {
			this.url = url;
		}

		private static String formulario(String... chaveValor) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i + 1 < chaveValor.length; i += 2) {
				if (sb.length() > 0) {
					sb.append('&');
				}
				sb.append(URLEncoder.encode(chaveValor[i], StandardCharsets.UTF_8)).append('=')
						.append(URLEncoder.encode(chaveValor[i + 1], StandardCharsets.UTF_8));
			}
			return sb.toString();
		}

		private Resp enviar(String metodo, String caminho, String... chaveValor) {
			try {
				HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url + caminho));
				if (metodo.equals("POST") || metodo.equals("PUT")) {
					b.header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
							.method(metodo, HttpRequest.BodyPublishers.ofString(formulario(chaveValor)));
				} else {
					b.method(metodo, HttpRequest.BodyPublishers.noBody());
				}
				HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
				return new Resp(r.statusCode(), r.body(), r.headers());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		Resp get(String caminho) {
			return enviar("GET", caminho);
		}

		Resp post(String caminho, String... chaveValor) {
			return enviar("POST", caminho, chaveValor);
		}

		Resp put(String caminho, String... chaveValor) {
			return enviar("PUT", caminho, chaveValor);
		}

		Resp delete(String caminho) {
			return enviar("DELETE", caminho);
		}

		Resp entrarExemplo() {
			return post("/api/acesso/entrar", "cnpj", "12345678000199", "senha", "123456");
		}

		// Cadastra a empresa Beta e entra com ela (para testar que uma empresa não vê dados da outra)
		Resp entrarComoBeta() {
			post("/api/acesso/cadastrar", "cnpj", "11222333000181", "razao_social", "Beta LTDA", "nome_fantasia", "Beta",
					"email", "contato@beta.com.br", "telefone", "(31) 4444-5555", "senha", "segredo1", "confirmacao",
					"segredo1");
			return post("/api/acesso/entrar", "cnpj", "11222333000181", "senha", "segredo1");
		}
	}
}
