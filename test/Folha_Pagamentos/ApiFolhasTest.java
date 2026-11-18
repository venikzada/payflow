package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiFolhasTest {

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

	@Test
	void simularJoao() {
		var r = cli.post("/api/folhas/simular", "funcionario_id", "1");
		assertEquals(200, r.status());
		assertTrue(r.tem("\"salario_liquido\":1999.31"), r.corpo());
		assertTrue(r.tem("\"valor_inss\":200.69"));
		assertTrue(r.tem("\"valor_fgts\":200"));
		assertTrue(r.tem("\"ja_lancada\":false"));
		assertTrue(r.tem("\"valor_hora\":11.36"));
	}

	@Test
	void simularAceitaVirgulaEVazio() {
		assertTrue(cli.post("/api/folhas/simular", "funcionario_id", "1", "he50", "1,5").tem("\"valor_he50\":25.57"));
		assertTrue(cli.post("/api/folhas/simular", "funcionario_id", "1", "he50", "").tem("\"salario_liquido\":1999.31"));
		var r = cli.post("/api/folhas/simular", "funcionario_id", "1", "faltas", "-1");
		assertEquals(400, r.status());
		assertTrue(r.tem("não podem ser negativos"));
		var lixo = cli.post("/api/folhas/simular", "funcionario_id", "1", "atrasos", "abc");
		assertEquals(400, lixo.status());
		assertTrue(lixo.tem("\"campo\":\"atrasos\""));
	}

	@Test
	void simularNaoGrava() throws Exception {
		try (Connection c = srv.conexao()) {
			var antes = BancoTeste.valor(c, "SELECT COUNT(*) FROM folha_funcionarios");
			cli.post("/api/folhas/simular", "funcionario_id", "1");
			assertEquals(antes, BancoTeste.valor(c, "SELECT COUNT(*) FROM folha_funcionarios"));
		}
	}

	@Test
	void lancarIgualAoSimuladoEDepoisConflita() {
		var r = cli.post("/api/folhas", "funcionario_id", "1", "he50", "10");
		assertEquals(201, r.status());
		assertTrue(r.tem("\"salario_liquido\":2154.42"));
		assertTrue(r.tem("\"status\":\"PENDENTE\""));
		var d = cli.post("/api/folhas", "funcionario_id", "1");
		assertEquals(409, d.status());
		assertTrue(d.tem("já foi lançado neste mês"));
		assertTrue(cli.post("/api/folhas/simular", "funcionario_id", "1").tem("\"ja_lancada\":true"));
	}

	@Test
	void pendentesConfirmarApagar() throws Exception {
		assertEquals(2, cli.get("/api/folhas?status=PENDENTE").conta("\"salario_liquido\""));
		var ids = new ArrayList<Integer>();
		try (Connection c = srv.conexao();
				var rs = c.createStatement().executeQuery("SELECT id FROM folha_funcionarios WHERE status='PENDENTE' ORDER BY id")) {
			while (rs.next()) {
				ids.add(rs.getInt(1));
			}
		}
		assertEquals(200, cli.post("/api/folhas/" + ids.get(0) + "/confirmar").status());
		var r = cli.delete("/api/folhas/" + ids.get(0));
		assertEquals(409, r.status());
		assertTrue(r.tem("Só folhas pendentes podem ser apagadas."));
		assertEquals(200, cli.delete("/api/folhas/" + ids.get(1)).status());
		assertEquals(0, cli.get("/api/folhas?status=PENDENTE").conta("\"salario_liquido\""));
	}

	@Test
	void historico() {
		assertTrue(cli.get("/api/folhas?mes=11&ano=2026").tem("\"quantidade\":5"));
		assertTrue(cli.get("/api/folhas?funcionario_id=1").tem("\"mes\":9"));
		assertEquals(400, cli.get("/api/folhas").status());
		assertTrue(cli.get("/api/folhas/1").tem("\"nome\":\"João da Silva\""));
	}

	@Test
	void outraEmpresa404() {
		var b = srv.cliente();
		b.entrarComoBeta();
		assertEquals(404, b.get("/api/folhas?funcionario_id=1").status());
		assertEquals(404, b.get("/api/folhas/1").status());
		assertEquals(404, b.post("/api/folhas/simular", "funcionario_id", "1").status());
		assertEquals(404, b.post("/api/folhas", "funcionario_id", "1").status());
		assertEquals(404, b.post("/api/folhas/1/confirmar").status());
		assertEquals(404, b.delete("/api/folhas/1").status());
		assertTrue(b.get("/api/resumo").tem("\"funcionarios\":0"));
	}

	@Test
	void resumoNovembro() throws Exception {
		var r = cli.get("/api/resumo");
		assertTrue(r.tem("\"funcionarios\":6"), r.corpo());
		assertTrue(r.tem("\"em_experiencia\":1"));
		assertTrue(r.tem("\"pendentes\":2"));
		assertTrue(r.tem("\"serie\":[{\"mes\":6,\"ano\":2026"));
		try (Connection c = srv.conexao()) {
			double esperado = ((Number) BancoTeste.valor(c,
					"SELECT SUM(ff.salario_liquido) FROM folha_funcionarios ff JOIN folhas_pagamento fp ON fp.id=ff.folha_id WHERE fp.mes=11 AND fp.ano=2026"))
					.doubleValue();
			assertTrue(r.tem("\"liquido_total\":" + Json.escrever(CalculoFolha.centavos(esperado))));
		}
	}

	@Test
	void resumoMesVazio() {
		var r = cli.get("/api/resumo?mes=1&ano=2026");
		assertTrue(r.tem("\"liquido_total\":0"));
		assertTrue(r.tem("\"variacao_liquido\":null"));
	}
}
