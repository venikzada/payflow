package Folha_Pagamentos;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Versão web do sistema: entrega as páginas da pasta web/ e a API em /api
public final class Servidor {

	private static final Map<String, String> TIPOS = Map.of(
			"html", "text/html; charset=utf-8",
			"css", "text/css; charset=utf-8",
			"js", "text/javascript; charset=utf-8",
			"woff2", "font/woff2",
			"svg", "image/svg+xml",
			"png", "image/png",
			"ico", "image/x-icon");

	private Servidor() {
	}

	public static HttpServer iniciar(int porta, String urlBanco, Path pastaWeb, Clock relogio) throws Exception {
		try (Connection c = Banco.abrir(urlBanco)) {
			Banco.preparar(c);
		}

		Sessoes sessoes = new Sessoes(relogio);
		Roteador rotas = new Roteador(urlBanco, sessoes, relogio);
		ApiAcesso.registrar(rotas, sessoes);
		ApiFuncionarios.registrar(rotas);
		ApiFolhas.registrar(rotas);

		HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
		servidor.createContext("/api/", rotas);
		Path raiz = pastaWeb.toAbsolutePath().normalize();
		servidor.createContext("/", troca -> arquivo(troca, raiz));
		servidor.setExecutor(null); // uma requisição por vez (SQLite)
		servidor.start();
		return servidor;
	}

	// Entrega um arquivo da pasta web/, sem deixar sair dela
	private static void arquivo(HttpExchange troca, Path raiz) throws IOException {
		try (troca) {
			String caminho = troca.getRequestURI().getPath();
			if (caminho == null || caminho.equals("/")) {
				caminho = "/index.html";
			}
			Path alvo = raiz.resolve(caminho.substring(1)).normalize();
			boolean metodoOk = troca.getRequestMethod().equals("GET") || troca.getRequestMethod().equals("HEAD");
			if (!metodoOk || !alvo.startsWith(raiz) || !Files.isRegularFile(alvo)) {
				byte[] corpo = "Não encontrado".getBytes(StandardCharsets.UTF_8);
				troca.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
				troca.sendResponseHeaders(404, corpo.length);
				try (OutputStream os = troca.getResponseBody()) {
					os.write(corpo);
				}
				return;
			}
			String nome = alvo.getFileName().toString();
			String ext = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase() : "";
			byte[] corpo = Files.readAllBytes(alvo);
			troca.getResponseHeaders().set("Content-Type", TIPOS.getOrDefault(ext, "application/octet-stream"));
			troca.getResponseHeaders().set("Cache-Control", "no-cache");
			troca.sendResponseHeaders(200, corpo.length);
			try (OutputStream os = troca.getResponseBody()) {
				os.write(corpo);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
		String banco = args.length > 1 ? args[1] : "database/folha.db";
		iniciar(porta, "jdbc:sqlite:" + banco, Path.of("web"), Clock.systemDefaultZone());
		System.out.println("Sistema no ar: http://localhost:" + porta);
	}
}
