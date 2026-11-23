package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

class BancoTest {

	private static int inteiro(Object o) {
		return ((Number) o).intValue();
	}

	@Test
	void prepararCriaColunasTipoEBeneficio() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			assertEquals(1, inteiro(BancoTeste.valor(c, "SELECT COUNT(*) FROM pragma_table_info('funcionarios') WHERE name='ativo'")));
			assertEquals(1, inteiro(BancoTeste.valor(c, "SELECT COUNT(*) FROM pragma_table_info('funcionarios') WHERE name='email'")));
			assertEquals("Benefício", BancoTeste.valor(c, "SELECT nome FROM tipos_lancamento WHERE id=16"));
			assertEquals(1, inteiro(BancoTeste.valor(c, "SELECT COUNT(*) FROM beneficios WHERE nome='Vale Combustível'")));
		}
	}

	@Test
	void senhaDeExemploEh123456() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			assertTrue(Senha.confereSenha("123456", (String) BancoTeste.valor(c, "SELECT senha FROM usuarios WHERE id=1")));
		}
	}

	@Test
	void prepararConverteSenhaEmTextoPuro() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			c.createStatement().executeUpdate("UPDATE usuarios SET senha='abc123' WHERE id=1");
			Banco.preparar(c);
			String s = (String) BancoTeste.valor(c, "SELECT senha FROM usuarios WHERE id=1");
			assertTrue(s.contains(":"));
			assertTrue(Senha.confereSenha("abc123", s));
			Banco.preparar(c);
			assertEquals(s, BancoTeste.valor(c, "SELECT senha FROM usuarios WHERE id=1"));
		}
	}

	@Test
	void prepararAcrescentaAtivoEmBancoAntigo() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			c.createStatement().executeUpdate("ALTER TABLE funcionarios DROP COLUMN ativo");
			Banco.preparar(c);
			assertEquals(1, inteiro(BancoTeste.valor(c, "SELECT ativo FROM funcionarios WHERE id=1")));
		}
	}

	@Test
	void cpfsDeExemploSaoValidos() throws Exception {
		try (Connection c = BancoTeste.novo(); ResultSet rs = c.createStatement().executeQuery("SELECT cpf FROM funcionarios")) {
			while (rs.next()) {
				assertTrue(Leitor.cpfValido(rs.getString(1)), rs.getString(1));
			}
		}
	}
}
