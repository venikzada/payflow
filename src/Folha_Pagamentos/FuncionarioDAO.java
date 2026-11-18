package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Acesso ao banco para os funcionários (versão web)
public final class FuncionarioDAO {

	public static final int MESES_EXPERIENCIA = 3;

	private static final String CAMPOS = "id, nome, cpf, email, cargo, salario_base, data_nascimento, data_admissao, ativo";

	private FuncionarioDAO() {
	}

	// Dados do formulário de funcionário, já conferidos
	public record Dados(String nome, String cpf, String email, LocalDate nascimento, LocalDate admissao, String cargo,
			double salarioBase) {

		public static Dados validar(String nome, String cpf, String email, String nascimento, String admissao,
				String cargo, String salario) {
			nome = nome == null ? "" : nome.trim();
			if (nome.isEmpty()) {
				throw ErroNegocio.invalido("nome", "Campo obrigatório: Nome");
			}
			cpf = cpf == null ? "" : cpf.replaceAll("\\D", "");
			if (!Leitor.cpfValido(cpf)) {
				throw ErroNegocio.invalido("cpf", "CPF inválido.");
			}
			email = email == null ? "" : email.trim();
			if (!Leitor.emailValido(email)) {
				throw ErroNegocio.invalido("email", "Email inválido.");
			}
			LocalDate dNasc;
			try {
				dNasc = LocalDate.parse(nascimento == null ? "" : nascimento.trim());
			} catch (DateTimeParseException e) {
				throw ErroNegocio.invalido("data_nascimento", "Data de nascimento inválida.");
			}
			LocalDate dAdm;
			try {
				dAdm = LocalDate.parse(admissao == null ? "" : admissao.trim());
			} catch (DateTimeParseException e) {
				throw ErroNegocio.invalido("data_admissao", "Data de admissão inválida.");
			}
			if (dAdm.isBefore(dNasc)) {
				throw ErroNegocio.invalido("data_admissao", "A admissão não pode ser antes do nascimento.");
			}
			cargo = cargo == null ? "" : cargo.trim();
			if (cargo.isEmpty()) {
				throw ErroNegocio.invalido("cargo", "Campo obrigatório: Cargo");
			}
			double valor;
			try {
				valor = Leitor.numeroBR(salario);
			} catch (NumberFormatException e) {
				throw ErroNegocio.invalido("salario_base", "Salário inválido.");
			}
			if (valor < 0) {
				throw ErroNegocio.invalido("salario_base", "Salário não pode ser negativo.");
			}
			return new Dados(nome, cpf, email, dNasc, dAdm, cargo, CalculoFolha.centavos(valor));
		}
	}

	// Acrescenta tempo de empresa e situação da experiência
	private static Map<String, Object> completar(Map<String, Object> f, LocalDate hoje) {
		LocalDate admissao = LocalDate.parse((String) f.get("data_admissao"));
		LocalDate fim = admissao.plusMonths(MESES_EXPERIENCIA);
		boolean emExperiencia = hoje.isBefore(fim);
		f.put("ativo", f.get("ativo") == null || ((Number) f.get("ativo")).intValue() == 1);
		f.put("tempo_empresa", FuncionarioMenu.tempoDeEmpresa(admissao, hoje));
		f.put("em_experiencia", emExperiencia);
		f.put("fim_experiencia", fim.toString());
		f.put("dias_restantes", emExperiencia ? ChronoUnit.DAYS.between(hoje, fim) : 0);
		return f;
	}

	public static List<Map<String, Object>> listar(Connection c, int empresaId, String filtro, String busca,
			LocalDate hoje) throws SQLException {
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.of("pt", "BR"));
		List<Map<String, Object>> lista = new ArrayList<>();
		try (PreparedStatement st = c.prepareStatement(
				"SELECT " + CAMPOS + " FROM funcionarios WHERE empresa_id = ? AND ativo = 1 ORDER BY nome")) {
			st.setInt(1, empresaId);
			try (ResultSet rs = st.executeQuery()) {
				for (Map<String, Object> f : Banco.linhas(rs)) {
					completar(f, hoje);
					boolean exp = (Boolean) f.get("em_experiencia");
					if ("experiencia".equals(filtro) && !exp) {
						continue;
					}
					if ("efetivados".equals(filtro) && exp) {
						continue;
					}
					if (!termo.isEmpty()
							&& !((String) f.get("nome")).toLowerCase(Locale.of("pt", "BR")).contains(termo)) {
						continue;
					}
					f.remove("ativo");
					lista.add(f);
				}
			}
		}
		return lista;
	}

	public static Map<String, Object> buscar(Connection c, int empresaId, int id, LocalDate hoje) throws SQLException {
		Map<String, Object> f;
		try (PreparedStatement st = c.prepareStatement("SELECT " + CAMPOS + " FROM funcionarios WHERE id = ? AND empresa_id = ?")) {
			st.setInt(1, id);
			st.setInt(2, empresaId);
			try (ResultSet rs = st.executeQuery()) {
				if (!rs.next()) {
					throw ErroNegocio.naoEncontrado("Funcionário não encontrado.");
				}
				f = Banco.linha(rs);
			}
		}
		completar(f, hoje);
		f.put("tem_folhas", temFolhas(c, id));
		return f;
	}

	private static boolean temFolhas(Connection c, int id) throws SQLException {
		try (PreparedStatement st = c.prepareStatement("SELECT 1 FROM folha_funcionarios WHERE funcionario_id = ?")) {
			st.setInt(1, id);
			return st.executeQuery().next();
		}
	}

	private static void conferirCpf(Connection c, String cpf, int idAtual) throws SQLException {
		try (PreparedStatement st = c.prepareStatement("SELECT 1 FROM funcionarios WHERE cpf = ? AND id <> ?")) {
			st.setString(1, cpf);
			st.setInt(2, idAtual);
			if (st.executeQuery().next()) {
				throw ErroNegocio.conflito("cpf", "Já existe um funcionário com esse CPF.");
			}
		}
	}

	private static void existe(Connection c, int empresaId, int id) throws SQLException {
		try (PreparedStatement st = c.prepareStatement("SELECT 1 FROM funcionarios WHERE id = ? AND empresa_id = ?")) {
			st.setInt(1, id);
			st.setInt(2, empresaId);
			if (!st.executeQuery().next()) {
				throw ErroNegocio.naoEncontrado("Funcionário não encontrado.");
			}
		}
	}

	public static int inserir(Connection c, int empresaId, Dados d) throws SQLException {
		conferirCpf(c, d.cpf(), -1);
		try (PreparedStatement st = c.prepareStatement(
				"INSERT INTO funcionarios (nome, cpf, email, data_nascimento, data_admissao, cargo, salario_base, empresa_id, ativo) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
				Statement.RETURN_GENERATED_KEYS)) {
			st.setString(1, d.nome());
			st.setString(2, d.cpf());
			st.setString(3, d.email());
			st.setString(4, d.nascimento().toString());
			st.setString(5, d.admissao().toString());
			st.setString(6, d.cargo());
			st.setDouble(7, d.salarioBase());
			st.setInt(8, empresaId);
			st.executeUpdate();
			try (ResultSet chave = st.getGeneratedKeys()) {
				chave.next();
				return chave.getInt(1);
			}
		}
	}

	public static void atualizar(Connection c, int empresaId, int id, Dados d) throws SQLException {
		existe(c, empresaId, id);
		conferirCpf(c, d.cpf(), id);
		try (PreparedStatement st = c.prepareStatement(
				"UPDATE funcionarios SET nome = ?, cpf = ?, email = ?, data_nascimento = ?, data_admissao = ?, cargo = ?, "
						+ "salario_base = ? WHERE id = ? AND empresa_id = ?")) {
			st.setString(1, d.nome());
			st.setString(2, d.cpf());
			st.setString(3, d.email());
			st.setString(4, d.nascimento().toString());
			st.setString(5, d.admissao().toString());
			st.setString(6, d.cargo());
			st.setDouble(7, d.salarioBase());
			st.setInt(8, id);
			st.setInt(9, empresaId);
			st.executeUpdate();
		}
	}

	// Sem folhas: apaga de vez. Com folhas: só desliga (ativo = 0), para não perder o histórico.
	public static String excluir(Connection c, int empresaId, int id) throws SQLException {
		existe(c, empresaId, id);
		if (temFolhas(c, id)) {
			try (PreparedStatement st = c.prepareStatement("UPDATE funcionarios SET ativo = 0 WHERE id = ?")) {
				st.setInt(1, id);
				st.executeUpdate();
			}
			return "DESLIGADO";
		}
		boolean autoCommit = c.getAutoCommit();
		c.setAutoCommit(false);
		try {
			try (PreparedStatement st = c.prepareStatement("DELETE FROM funcionarios_beneficios WHERE funcionario_id = ?")) {
				st.setInt(1, id);
				st.executeUpdate();
			}
			try (PreparedStatement st = c.prepareStatement("DELETE FROM funcionarios WHERE id = ?")) {
				st.setInt(1, id);
				st.executeUpdate();
			}
			c.commit();
		} catch (SQLException | RuntimeException e) {
			c.rollback();
			throw e;
		} finally {
			c.setAutoCommit(autoCommit);
		}
		return "EXCLUIDO";
	}

	public static List<Map<String, Object>> cargos(Connection c) throws SQLException {
		try (ResultSet rs = c.createStatement().executeQuery("SELECT id, nome FROM cargos ORDER BY nome")) {
			return Banco.linhas(rs);
		}
	}
}
