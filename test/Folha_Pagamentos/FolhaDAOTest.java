package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class FolhaDAOTest {

	private static int inteiro(Object o) {
		return ((Number) o).intValue();
	}

	@Test
	void lancarGravaPendenteComLinhas() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			int id = FolhaDAO.lancar(c, 1, 1, FolhaDAO.montarEntrada(c, 1, 1, 0, 0, 0, 0), 11, 2026);
			assertEquals("PENDENTE", BancoTeste.valor(c, "SELECT status FROM folha_funcionarios WHERE id=?", id));
			assertEquals(1999.31, BancoTeste.valor(c, "SELECT salario_liquido FROM folha_funcionarios WHERE id=?", id));
			assertEquals(6, inteiro(BancoTeste.valor(c, "SELECT COUNT(*) FROM lancamentos_folha WHERE folha_funcionario_id=?", id)));
			assertEquals(100.0, BancoTeste.valor(c,
					"SELECT valor FROM lancamentos_folha WHERE folha_funcionario_id=? AND tipo_lancamento_id=16 AND descricao='Plano de Saúde'", id));
		}
	}

	@Test
	void lancarDuasVezesDaConflito() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			var e = FolhaDAO.montarEntrada(c, 1, 1, 0, 0, 0, 0);
			FolhaDAO.lancar(c, 1, 1, e, 11, 2026);
			var erro = assertThrows(ErroNegocio.class, () -> FolhaDAO.lancar(c, 1, 1, e, 11, 2026));
			assertEquals(409, erro.status());
			assertEquals("Esse funcionário já foi lançado neste mês.", erro.getMessage());
		}
	}

	@Test
	void montarEntradaValida() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			assertEquals(400, assertThrows(ErroNegocio.class, () -> FolhaDAO.montarEntrada(c, 1, 1, -1, 0, 0, 0)).status());
			c.createStatement().executeUpdate("INSERT INTO empresas (razao_social,nome_fantasia,cnpj,email,data_cadastro) VALUES ('B','B','11222333000181','b@b.com','2026-11-14')");
			assertEquals(404, assertThrows(ErroNegocio.class, () -> FolhaDAO.montarEntrada(c, 2, 1, 0, 0, 0, 0)).status());
			c.createStatement().executeUpdate("UPDATE funcionarios SET ativo=0 WHERE id=1");
			assertEquals(404, assertThrows(ErroNegocio.class, () -> FolhaDAO.montarEntrada(c, 1, 1, 0, 0, 0, 0)).status());
		}
	}

	@Test
	void confirmarEApagar() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			int id = FolhaDAO.lancar(c, 1, 1, FolhaDAO.montarEntrada(c, 1, 1, 0, 0, 0, 0), 11, 2026);
			assertTrue(FolhaDAO.pendentes(c, 1).stream().anyMatch(m -> inteiro(m.get("id")) == id));
			FolhaDAO.confirmar(c, 1, id);
			assertTrue(FolhaDAO.pendentes(c, 1).stream().noneMatch(m -> inteiro(m.get("id")) == id));
			assertEquals(409, assertThrows(ErroNegocio.class, () -> FolhaDAO.apagar(c, 1, id)).status());
			assertEquals(404, assertThrows(ErroNegocio.class, () -> FolhaDAO.confirmar(c, 1, id)).status());
		}
	}

	@Test
	void apagarRemoveLinhas() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			int id = FolhaDAO.lancar(c, 1, 1, FolhaDAO.montarEntrada(c, 1, 1, 0, 0, 0, 0), 11, 2026);
			FolhaDAO.apagar(c, 1, id);
			assertEquals(0, inteiro(BancoTeste.valor(c, "SELECT COUNT(*) FROM lancamentos_folha WHERE folha_funcionario_id=?", id)));
			assertEquals(0, inteiro(BancoTeste.valor(c, "SELECT COUNT(*) FROM folha_funcionarios WHERE id=?", id)));
		}
	}

	@Test
	void holeriteDaFolhaNova() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			int id = FolhaDAO.lancar(c, 1, 1, FolhaDAO.montarEntrada(c, 1, 1, 10, 0, 0, 0), 11, 2026);
			var h = FolhaDAO.holerite(c, 1, id);
			var linhas = (List<Map<String, Object>>) h.get("linhas");
			assertEquals(2154.42, h.get("salario_liquido"));
			assertTrue(linhas.stream().anyMatch(l -> l.get("descricao").equals("Hora Extra 50%")
					&& l.get("valor").equals(170.45) && l.get("tipo").equals("PROVENTO")));
			assertTrue(linhas.stream().noneMatch(l -> l.get("descricao").equals("Benefícios")));
			assertEquals("João da Silva", ((Map<String, Object>) h.get("funcionario")).get("nome"));
		}
	}

	@Test
	void holeriteAntigoDeSetembro() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			var linhas = (List<Map<String, Object>>) FolhaDAO.holerite(c, 1, 1).get("linhas");
			assertTrue(linhas.stream().anyMatch(l -> l.get("descricao").equals("Falta") && l.get("valor").equals(100.0)));
			assertTrue(linhas.stream().noneMatch(l -> l.get("descricao").equals("Benefícios")));
		}
	}

	@Test
	void holeriteSemLancamentosUsaResiduo() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			c.createStatement().executeUpdate("INSERT INTO folhas_pagamento VALUES (900,1,2020,'2020-01-31','ABERTA')");
			c.createStatement().executeUpdate("INSERT INTO folha_funcionarios VALUES (900,900,1,2500,2500,2500,2500,200.69,2500,200,2299.31,0,500.69,1999.31,'CONFIRMADA')");
			var linhas = (List<Map<String, Object>>) FolhaDAO.holerite(c, 1, 900).get("linhas");
			assertEquals(List.of("Salário Base", "INSS", "IRPF", "Benefícios"), linhas.stream().map(l -> l.get("descricao")).toList());
			assertEquals(300.0, linhas.get(3).get("valor"));
		}
	}

	@Test
	void holeriteDeOutraEmpresa404() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			assertEquals(404, assertThrows(ErroNegocio.class, () -> FolhaDAO.holerite(c, 2, 1)).status());
		}
	}

	@Test
	void historicoMesTotais() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			FolhaDAO.lancar(c, 1, 1, FolhaDAO.montarEntrada(c, 1, 1, 0, 0, 0, 0), 11, 2026);
			var totais = (Map<String, Object>) FolhaDAO.historicoMes(c, 1, 11, 2026).get("totais");
			assertEquals(1, inteiro(totais.get("quantidade")));
			assertEquals(1999.31, totais.get("liquido"));
		}
	}

	@Test
	void historicoFuncionarioMaisNovoPrimeiro() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			FolhaDAO.lancar(c, 1, 1, FolhaDAO.montarEntrada(c, 1, 1, 0, 0, 0, 0), 11, 2026);
			var lista = FolhaDAO.historicoFuncionario(c, 1, 1);
			assertEquals(11, inteiro(lista.get(0).get("mes")));
			assertEquals(9, inteiro(lista.get(lista.size() - 1).get("mes")));
		}
	}
}
