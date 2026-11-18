package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServidorTest {

	ServidorTeste srv;
	ServidorTeste.Cliente cli;

	@BeforeEach
	void subir() throws Exception {
		srv = new ServidorTeste();
		cli = srv.cliente();
	}

	@AfterEach
	void descer() {
		srv.close();
	}

	@Test
	void estaticos() {
		var r = cli.get("/");
		assertEquals(200, r.status());
		assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
		assertEquals(404, cli.get("/../database/folha2.db.sql").status());
		assertEquals(404, cli.get("/%2e%2e/database/folha2.db.sql").status());
		assertEquals(404, cli.get("/naoexiste.html").status());
	}

	@Test
	void semLogin401() {
		var r = cli.get("/api/acesso/eu");
		assertEquals(401, r.status());
		assertTrue(r.tem("Faça login para continuar."));
	}

	@Test
	void entrarComCnpjMascarado() {
		var r = cli.post("/api/acesso/entrar", "cnpj", "12.345.678/0001-99", "senha", "123456");
		assertEquals(200, r.status());
		assertTrue(r.tem("\"nome_fantasia\":\"Administracao de Facilities\""));
		assertEquals(200, cli.get("/api/acesso/eu").status());
		assertEquals(404, cli.get("/api/nada").status());
	}

	@Test
	void cnpjCurto() {
		var r = cli.post("/api/acesso/entrar", "cnpj", "123", "senha", "123456");
		assertEquals(400, r.status());
		assertTrue(r.tem("\"campo\":\"cnpj\""));
	}

	@Test
	void tresErrosBloqueiam() {
		for (int i = 1; i <= 3; i++) {
			var r = cli.post("/api/acesso/entrar", "cnpj", "12345678000199", "senha", "errada");
			assertEquals(401, r.status());
			assertTrue(r.tem("Tentativa " + i + " de 3"), r.corpo());
		}
		var r = cli.post("/api/acesso/entrar", "cnpj", "12345678000199", "senha", "123456");
		assertEquals(429, r.status());
		assertTrue(r.tem("Muitas tentativas"));
	}

	@Test
	void cadastrarEEntrar() {
		var dados = new String[] { "cnpj", "11222333000181", "razao_social", "Beta LTDA", "nome_fantasia", "Beta", "email",
				"contato@beta.com.br", "telefone", "(31) 4444-5555", "senha", "segredo1", "confirmacao", "segredo1" };
		assertEquals(201, cli.post("/api/acesso/cadastrar", dados).status());
		assertEquals(409, cli.post("/api/acesso/cadastrar", dados).status());
		assertEquals(200, cli.post("/api/acesso/entrar", "cnpj", "11222333000181", "senha", "segredo1").status());
	}

	@Test
	void cadastrarSenhasDiferentes() {
		var r = cli.post("/api/acesso/cadastrar", "cnpj", "11222333000181", "razao_social", "B", "nome_fantasia", "B",
				"email", "b@b.com.br", "telefone", "1", "senha", "segredo1", "confirmacao", "outra123");
		assertEquals(400, r.status());
		assertTrue(r.tem("\"campo\":\"confirmacao\""));
	}

	@Test
	void cadastrarEmailInvalido() {
		var r = cli.post("/api/acesso/cadastrar", "cnpj", "11222333000181", "razao_social", "B", "nome_fantasia", "B",
				"email", "b@b", "telefone", "1", "senha", "segredo1", "confirmacao", "segredo1");
		assertEquals(400, r.status());
		assertTrue(r.tem("\"campo\":\"email\""));
	}

	@Test
	void redefinirSenha() {
		assertEquals(400, cli.post("/api/acesso/redefinir", "cnpj", "12345678000199", "email", "contato@facilities.com.br",
				"telefone", "0000", "senha", "novasenha", "confirmacao", "novasenha").status());
		assertEquals(200, cli.post("/api/acesso/redefinir", "cnpj", "12345678000199", "email", "contato@facilities.com.br",
				"telefone", "(31) 3333-4444", "senha", "novasenha", "confirmacao", "novasenha").status());
		assertEquals(200, cli.post("/api/acesso/entrar", "cnpj", "12345678000199", "senha", "novasenha").status());
	}

	@Test
	void sairEncerra() {
		cli.entrarExemplo();
		cli.post("/api/acesso/sair");
		assertEquals(401, cli.get("/api/acesso/eu").status());
	}
}
