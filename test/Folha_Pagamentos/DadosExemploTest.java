package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

class DadosExemploTest {

	private static int inteiro(Object o) {
		return ((Number) o).intValue();
	}

	@Test
	void seisFuncionariosAtivos() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			assertEquals(6, inteiro(BancoTeste.valor(c, "SELECT COUNT(*) FROM funcionarios WHERE empresa_id=1 AND ativo=1")));
		}
	}

	@Test
	void novembroSemJoaoComDuasPendentes() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			String base = "SELECT COUNT(*) FROM folha_funcionarios ff JOIN folhas_pagamento fp ON fp.id=ff.folha_id WHERE fp.mes=11 AND fp.ano=2026";
			assertEquals(0, inteiro(BancoTeste.valor(c, base + " AND ff.funcionario_id=1")));
			assertEquals(5, inteiro(BancoTeste.valor(c, base)));
			assertEquals(2, inteiro(BancoTeste.valor(c, base + " AND ff.status='PENDENTE'")));
		}
	}

	@Test
	void mesesDeJunhoANovembro() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			assertEquals("6,7,8,9,10,11", BancoTeste.valor(c,
					"SELECT group_concat(mes) FROM (SELECT DISTINCT mes FROM folhas_pagamento WHERE ano=2026 ORDER BY mes)"));
		}
	}

	@Test
	void folhasConsistentes() throws Exception {
		try (Connection c = BancoTeste.novo();
				ResultSet rs = c.createStatement().executeQuery("SELECT * FROM folha_funcionarios WHERE id > 1")) {
			int n = 0;
			while (rs.next()) {
				n++;
				assertEquals(CalculoFolha.centavos(rs.getDouble("total_proventos") - rs.getDouble("total_descontos")),
						rs.getDouble("salario_liquido"), 0.001);
				assertEquals(CalculoFolha.centavos(rs.getDouble("base_fgts") * 0.08), rs.getDouble("valor_fgts"), 0.001);
				assertEquals(new descontos().InssDesconto(rs.getDouble("base_inss")), rs.getDouble("valor_inss"), 0.001);
			}
			assertEquals(31, n);
		}
	}

	@Test
	void beneficiosDentroDoLimite() throws Exception {
		try (Connection c = BancoTeste.novo(); ResultSet rs = c.createStatement().executeQuery(
				"SELECT b.nome, fb.valor, fb.desconto_folha, f.salario_base FROM funcionarios_beneficios fb "
						+ "JOIN beneficios b ON b.id=fb.beneficio_id JOIN funcionarios f ON f.id=fb.funcionario_id WHERE fb.ativo=1")) {
			while (rs.next()) {
				assertTrue(rs.getDouble(3) <= Beneficio.limiteDesconto(rs.getString(1), rs.getDouble(2), rs.getDouble(4)) + 0.001,
						rs.getString(1));
			}
		}
	}
}
