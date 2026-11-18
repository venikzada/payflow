package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiFuncionariosTest {

	ServidorTeste srv;
	ServidorTeste.Cliente cli;

	@BeforeEach
	void subir() throws Exception {
		srv = new ServidorTeste();
		cli = srv.cliente();
		cli.entrarExemplo();
	}

	@AfterEach
	void descer() {
		srv.close();
	}

	static final String[] BRUNO = { "nome", "Bruno Lima", "cpf", "111.444.777-35", "email", "bruno@facilities.com.br",
			"data_nascimento", "1999-03-10", "data_admissao", "2026-11-03", "cargo", "Auxiliar Administrativo",
			"salario_base", "2.300,00" };

	static String[] joao(String nome, String cpf) {
		return new String[] { "nome", nome, "cpf", cpf, "email", "joao.silva@facilities.com.br", "data_nascimento",
				"1995-05-20", "data_admissao", "2026-01-10", "cargo", "Auxiliar Administrativo", "salario_base", "2500" };
	}

	@Test
	void listaSeisEmOrdem() {
		var r = cli.get("/api/funcionarios");
		assertEquals(6, r.conta("\"cpf\":"));
		assertTrue(r.corpo().indexOf("Ana Ribeiro") < r.corpo().indexOf("Carlos Souza"));
		assertTrue(r.tem("\"tempo_empresa\":\"10 meses e 4 dias\""));
	}

	@Test
	void filtroExperiencia() {
		var r = cli.get("/api/funcionarios?filtro=experiencia");
		assertEquals(1, r.conta("\"cpf\":"));
		assertTrue(r.tem("Ana Ribeiro"));
		assertTrue(r.tem("\"dias_restantes\":17"));
		assertEquals(5, cli.get("/api/funcionarios?filtro=efetivados").conta("\"cpf\":"));
	}

	@Test
	void buscaSemDiferenciarMaiusculas() {
		var r = cli.get("/api/funcionarios?busca=JO%C3%83O");
		assertTrue(r.tem("João da Silva"));
		assertEquals(1, r.conta("\"cpf\":"));
	}

	@Test
	void cpfInvalido() {
		var d = BRUNO.clone();
		d[3] = "123.456.789-00";
		var r = cli.post("/api/funcionarios", d);
		assertEquals(400, r.status());
		assertTrue(r.tem("\"campo\":\"cpf\""));
	}

	@Test
	void admissaoAntesDoNascimento() {
		var d = BRUNO.clone();
		d[9] = "1990-01-01";
		var r = cli.post("/api/funcionarios", d);
		assertEquals(400, r.status());
		assertTrue(r.tem("A admissão não pode ser antes do nascimento."));
	}

	@Test
	void salarioNegativo() {
		var d = BRUNO.clone();
		d[13] = "-1";
		var r = cli.post("/api/funcionarios", d);
		assertEquals(400, r.status());
		assertTrue(r.tem("\"campo\":\"salario_base\""));
	}

	@Test
	void criarBuscarExcluir() {
		var r = cli.post("/api/funcionarios", BRUNO);
		assertEquals(201, r.status());
		String id = r.corpo().replaceAll("\\D", "");
		var f = cli.get("/api/funcionarios/" + id);
		assertTrue(f.tem("\"em_experiencia\":true"));
		assertTrue(f.tem("\"tem_folhas\":false"));
		assertTrue(f.tem("\"salario_base\":2300"));
		assertTrue(f.tem("\"cpf\":\"11144477735\""));
		assertTrue(cli.delete("/api/funcionarios/" + id).tem("EXCLUIDO"));
		assertEquals(404, cli.get("/api/funcionarios/" + id).status());
	}

	@Test
	void cpfDuplicado() {
		var d = BRUNO.clone();
		d[3] = "529.982.247-25";
		assertEquals(409, cli.post("/api/funcionarios", d).status());
	}

	@Test
	void editarMantendoProprioCpf() {
		var r = cli.put("/api/funcionarios/1", joao("João da Silva Santos", "52998224725"));
		assertEquals(200, r.status(), r.corpo());
		assertTrue(cli.get("/api/funcionarios/1").tem("João da Silva Santos"));
		assertEquals(409, cli.put("/api/funcionarios/1", joao("João", "16899535009")).status());
	}

	@Test
	void excluirComFolhaDesliga() {
		var d = cli.delete("/api/funcionarios/2");
		assertTrue(d.tem("DESLIGADO"), d.status() + " " + d.corpo());
		assertEquals(5, cli.get("/api/funcionarios").conta("\"cpf\":"));
		assertTrue(cli.get("/api/funcionarios/2").tem("\"ativo\":false"));
	}

	@Test
	void outraEmpresaNaoVe() {
		var b = srv.cliente();
		b.entrarComoBeta();
		assertEquals(404, b.get("/api/funcionarios/1").status());
		assertEquals(404, b.put("/api/funcionarios/1", joao("X", "52998224725")).status());
		assertEquals(404, b.delete("/api/funcionarios/1").status());
		assertEquals(0, b.get("/api/funcionarios").conta("\"cpf\":"));
		assertTrue(cli.get("/api/funcionarios/1").tem("João da Silva"));
	}

	@Test
	void cargos() {
		assertTrue(cli.get("/api/cargos").tem("Analista Financeiro"));
	}

	// Revisão final: "3.500" é três mil e quinhentos, não R$ 3,50
	@Test
	void salarioComPontoDeMilhar() {
		var r = cli.put("/api/funcionarios/1", "nome", "João da Silva", "cpf", "52998224725", "email", "joao.silva@facilities.com.br",
				"data_nascimento", "1995-05-20", "data_admissao", "2026-01-10", "cargo", "Auxiliar Administrativo", "salario_base", "3.500");
		assertEquals(200, r.status(), r.corpo());
		assertTrue(cli.get("/api/funcionarios/1").tem("\"salario_base\":3500"));
	}
}
