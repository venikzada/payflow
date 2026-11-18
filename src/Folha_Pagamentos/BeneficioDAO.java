package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Acesso ao banco para os benefícios dos funcionários (versão web). Os limites vêm da classe Beneficio.
public final class BeneficioDAO {

	private BeneficioDAO() {
	}

	private static PreparedStatement preparar(Connection c, String sql, Object... params) throws SQLException {
		PreparedStatement st = c.prepareStatement(sql);
		for (int i = 0; i < params.length; i++) {
			st.setObject(i + 1, params[i]);
		}
		return st;
	}

	private static Map<String, Object> primeira(Connection c, String sql, Object... params) throws SQLException {
		try (PreparedStatement st = preparar(c, sql, params); ResultSet rs = st.executeQuery()) {
			return rs.next() ? Banco.linha(rs) : null;
		}
	}

	private static double numero(Object o) {
		return o == null ? 0 : ((Number) o).doubleValue();
	}

	public static List<Map<String, Object>> catalogo(Connection c) throws SQLException {
		try (ResultSet rs = c.createStatement()
				.executeQuery("SELECT id, nome, tipo, valor_padrao FROM beneficios WHERE ativo = 1 ORDER BY nome")) {
			return Banco.linhas(rs);
		}
	}

	private static double salario(Connection c, int empresaId, int funcionarioId) throws SQLException {
		Map<String, Object> f = primeira(c, "SELECT salario_base FROM funcionarios WHERE id = ? AND empresa_id = ? AND ativo = 1",
				funcionarioId, empresaId);
		if (f == null) {
			throw ErroNegocio.naoEncontrado("Funcionário não encontrado.");
		}
		return numero(f.get("salario_base"));
	}

	private static String nomeBeneficio(Connection c, int beneficioId) throws SQLException {
		Map<String, Object> b = primeira(c, "SELECT nome FROM beneficios WHERE id = ? AND ativo = 1", beneficioId);
		if (b == null) {
			throw ErroNegocio.naoEncontrado("Benefício não encontrado.");
		}
		return (String) b.get("nome");
	}

	// Quanto pode ser descontado na folha, e quanto dá o percentual informado
	private static Map<String, Object> calcularLimite(String nome, double valor, double salario, double percentual) {
		double limite = CalculoFolha.centavos(Beneficio.limiteDesconto(nome, valor, salario));
		boolean sobreValor = Beneficio.sobreValor(nome);
		double base = sobreValor ? valor : salario;
		boolean permite = limite > 0 && base > 0;
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("limite", limite);
		m.put("base", base);
		m.put("percentual_maximo", permite ? CalculoFolha.centavos(limite / base * 100) : 0.0);
		m.put("sobre", sobreValor ? "valor" : "salario");
		m.put("permite_desconto", permite);
		m.put("desconto", permite ? CalculoFolha.centavos(base * percentual / 100) : 0.0);
		return m;
	}

	public static Map<String, Object> limite(Connection c, int empresaId, int funcionarioId, int beneficioId, double valor,
			double percentual) throws SQLException {
		return calcularLimite(nomeBeneficio(c, beneficioId), valor, salario(c, empresaId, funcionarioId), percentual);
	}

	// Confere valor e percentual e devolve o desconto em reais
	private static double descontoValidado(String nome, double valor, double salario, double percentual) {
		if (valor < 0) {
			throw ErroNegocio.invalido("valor", "Valor não pode ser negativo.");
		}
		Map<String, Object> lim = calcularLimite(nome, valor, salario, percentual);
		if (!(Boolean) lim.get("permite_desconto")) {
			if (percentual > 0) {
				throw ErroNegocio.invalido("percentual", "Esse benefício não permite desconto na folha.");
			}
			return 0;
		}
		double max = numero(lim.get("percentual_maximo"));
		if (percentual < 0 || percentual > max + 0.0001) {
			throw ErroNegocio.invalido("percentual",
					"Percentual inválido. Digite entre 0 e " + String.format(Locale.of("pt", "BR"), "%.2f", max) + ".");
		}
		return numero(lim.get("desconto"));
	}

	public static List<Map<String, Object>> doFuncionario(Connection c, int empresaId, int funcionarioId) throws SQLException {
		if (primeira(c, "SELECT 1 FROM funcionarios WHERE id = ? AND empresa_id = ?", funcionarioId, empresaId) == null) {
			throw ErroNegocio.naoEncontrado("Funcionário não encontrado.");
		}
		List<Map<String, Object>> lista;
		try (PreparedStatement st = preparar(c,
				"SELECT fb.id, fb.beneficio_id, b.nome, fb.valor, fb.desconto_folha, fb.data_inicio, f.salario_base "
						+ "FROM funcionarios_beneficios fb JOIN beneficios b ON b.id = fb.beneficio_id "
						+ "JOIN funcionarios f ON f.id = fb.funcionario_id "
						+ "WHERE fb.funcionario_id = ? AND fb.ativo = 1 ORDER BY b.nome",
				funcionarioId); ResultSet rs = st.executeQuery()) {
			lista = Banco.linhas(rs);
		}
		for (Map<String, Object> b : lista) {
			boolean sobreValor = Beneficio.sobreValor((String) b.get("nome"));
			double base = sobreValor ? numero(b.get("valor")) : numero(b.get("salario_base"));
			b.put("percentual", base > 0 ? CalculoFolha.centavos(numero(b.get("desconto_folha")) / base * 100) : 0.0);
			b.put("sobre", sobreValor ? "valor" : "salario");
			b.remove("salario_base");
		}
		return lista;
	}

	public static int atribuir(Connection c, int empresaId, int funcionarioId, int beneficioId, double valor,
			double percentual, LocalDate hoje) throws SQLException {
		double salario = salario(c, empresaId, funcionarioId);
		String nome = nomeBeneficio(c, beneficioId);
		if (primeira(c, "SELECT 1 FROM funcionarios_beneficios WHERE funcionario_id = ? AND beneficio_id = ? AND ativo = 1",
				funcionarioId, beneficioId) != null) {
			throw ErroNegocio.conflito("beneficio_id", "Esse funcionário já tem o benefício " + nome + ".");
		}
		double desconto = descontoValidado(nome, valor, salario, percentual);
		try (PreparedStatement st = c.prepareStatement(
				"INSERT INTO funcionarios_beneficios (funcionario_id, beneficio_id, valor, desconto_folha, data_inicio) "
						+ "VALUES (?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS)) {
			st.setInt(1, funcionarioId);
			st.setInt(2, beneficioId);
			st.setDouble(3, CalculoFolha.centavos(valor));
			st.setDouble(4, desconto);
			st.setString(5, hoje.toString());
			st.executeUpdate();
			try (ResultSet chave = st.getGeneratedKeys()) {
				chave.next();
				return chave.getInt(1);
			}
		}
	}

	// Vínculo ativo (funcionário x benefício) que pertence à empresa
	private static Map<String, Object> vinculo(Connection c, int empresaId, int vinculoId) throws SQLException {
		Map<String, Object> v = primeira(c,
				"SELECT fb.id, fb.funcionario_id, fb.beneficio_id, b.nome, f.salario_base FROM funcionarios_beneficios fb "
						+ "JOIN beneficios b ON b.id = fb.beneficio_id JOIN funcionarios f ON f.id = fb.funcionario_id "
						+ "WHERE fb.id = ? AND f.empresa_id = ? AND fb.ativo = 1",
				vinculoId, empresaId);
		if (v == null) {
			throw ErroNegocio.naoEncontrado("Benefício não encontrado.");
		}
		return v;
	}

	public static void editar(Connection c, int empresaId, int vinculoId, double valor, double percentual)
			throws SQLException {
		Map<String, Object> v = vinculo(c, empresaId, vinculoId);
		double desconto = descontoValidado((String) v.get("nome"), valor, numero(v.get("salario_base")), percentual);
		try (PreparedStatement st = preparar(c, "UPDATE funcionarios_beneficios SET valor = ?, desconto_folha = ? WHERE id = ?",
				CalculoFolha.centavos(valor), desconto, vinculoId)) {
			st.executeUpdate();
		}
	}

	// Só desativa. Antes apaga um vínculo inativo antigo do mesmo benefício, por causa do índice único.
	public static void remover(Connection c, int empresaId, int vinculoId, LocalDate hoje) throws SQLException {
		Map<String, Object> v = vinculo(c, empresaId, vinculoId);
		try (PreparedStatement st = preparar(c,
				"DELETE FROM funcionarios_beneficios WHERE funcionario_id = ? AND beneficio_id = ? AND ativo = 0",
				v.get("funcionario_id"), v.get("beneficio_id"))) {
			st.executeUpdate();
		}
		try (PreparedStatement st = preparar(c,
				"UPDATE funcionarios_beneficios SET ativo = 0, data_fim = ? WHERE id = ?", hoje.toString(), vinculoId)) {
			st.executeUpdate();
		}
	}
}
