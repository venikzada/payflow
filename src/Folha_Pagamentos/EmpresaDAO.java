package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Acesso ao banco para as empresas e seus usuários (login, cadastro e senha)
public final class EmpresaDAO {

	private EmpresaDAO() {
	}

	private static String soDigitos(String s) {
		return s == null ? "" : s.replaceAll("\\D", "");
	}

	// Devolve a empresa se o CNPJ e a senha conferirem, ou null
	public static Map<String, Object> autenticar(Connection c, String cnpj, String senha) throws SQLException {
		try (PreparedStatement st = c.prepareStatement("SELECT e.id, u.senha FROM empresas e "
				+ "JOIN usuarios u ON u.empresa_id = e.id WHERE e.cnpj = ? AND e.ativo = 1 AND u.ativo = 1")) {
			st.setString(1, cnpj);
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					if (Senha.confereSenha(senha, rs.getString("senha"))) {
						return buscar(c, rs.getInt("id"));
					}
				}
			}
		}
		return null;
	}

	public static int cadastrar(Connection c, String cnpj, String razaoSocial, String nomeFantasia, String email,
			String telefone, String senha) throws SQLException {
		cnpj = soDigitos(cnpj);
		if (!cnpj.matches("\\d{14}")) {
			throw ErroNegocio.invalido("cnpj", "CNPJ deve ter 14 números.");
		}
		try (PreparedStatement st = c.prepareStatement("SELECT 1 FROM empresas WHERE cnpj = ?")) {
			st.setString(1, cnpj);
			if (st.executeQuery().next()) {
				throw ErroNegocio.conflito("cnpj", "Essa empresa já está cadastrada. Use a opção Entrar.");
			}
		}
		if (razaoSocial == null || razaoSocial.isBlank()) {
			throw ErroNegocio.invalido("razao_social", "Campo obrigatório: Razão social");
		}
		if (nomeFantasia == null || nomeFantasia.isBlank()) {
			throw ErroNegocio.invalido("nome_fantasia", "Campo obrigatório: Nome fantasia");
		}
		if (email == null || !Leitor.emailValido(email)) {
			throw ErroNegocio.invalido("email", "Email inválido.");
		}
		if (telefone == null || telefone.isBlank()) {
			throw ErroNegocio.invalido("telefone", "Campo obrigatório: Telefone");
		}
		validarSenha(senha);

		String hoje = LocalDate.now().toString();
		int empresaId;
		try (PreparedStatement st = c.prepareStatement(
				"INSERT INTO empresas (razao_social, nome_fantasia, cnpj, email, telefone, data_cadastro) VALUES (?, ?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS)) {
			st.setString(1, razaoSocial.trim());
			st.setString(2, nomeFantasia.trim());
			st.setString(3, cnpj);
			st.setString(4, email.trim());
			st.setString(5, telefone.trim());
			st.setString(6, hoje);
			st.executeUpdate();
			try (ResultSet chave = st.getGeneratedKeys()) {
				chave.next();
				empresaId = chave.getInt(1);
			}
		}
		criarUsuario(c, empresaId, cnpj, email.trim(), senha);
		return empresaId;
	}

	static void validarSenha(String senha) {
		if (senha == null || senha.length() < 6) {
			throw ErroNegocio.invalido("senha", "Senha muito curta. Use pelo menos 6 caracteres.");
		}
	}

	private static void criarUsuario(Connection c, int empresaId, String usuario, String email, String senha)
			throws SQLException {
		try (PreparedStatement st = c.prepareStatement(
				"INSERT INTO usuarios (empresa_id, usuario, senha, email, data_cadastro) VALUES (?, ?, ?, ?, ?)")) {
			st.setInt(1, empresaId);
			st.setString(2, usuario);
			st.setString(3, Senha.criarHash(senha));
			st.setString(4, email);
			st.setString(5, LocalDate.now().toString());
			st.executeUpdate();
		}
	}

	// Confere CNPJ, email e telefone com o cadastro e troca a senha
	public static void redefinirSenha(Connection c, String cnpj, String email, String telefone, String novaSenha)
			throws SQLException {
		cnpj = soDigitos(cnpj);
		Integer empresaId = null;
		try (PreparedStatement st = c.prepareStatement("SELECT id, email, telefone FROM empresas WHERE cnpj = ? AND ativo = 1")) {
			st.setString(1, cnpj);
			try (ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					String telBanco = soDigitos(rs.getString("telefone"));
					boolean confere = rs.getString("email").equalsIgnoreCase(email == null ? "" : email.trim())
							&& !telBanco.isEmpty() && telBanco.equals(soDigitos(telefone));
					if (confere) {
						empresaId = rs.getInt("id");
					}
				}
			}
		}
		if (empresaId == null) {
			throw ErroNegocio.invalido("Os dados não conferem. Não foi possível redefinir a senha.");
		}
		validarSenha(novaSenha);
		int alterados;
		try (PreparedStatement st = c.prepareStatement("UPDATE usuarios SET senha = ? WHERE empresa_id = ?")) {
			st.setString(1, Senha.criarHash(novaSenha));
			st.setInt(2, empresaId);
			alterados = st.executeUpdate();
		}
		if (alterados == 0) {
			criarUsuario(c, empresaId, cnpj, email.trim(), novaSenha);
		}
	}

	public static Map<String, Object> buscar(Connection c, int empresaId) throws SQLException {
		try (PreparedStatement st = c.prepareStatement(
				"SELECT id, razao_social, nome_fantasia, cnpj, email, telefone FROM empresas WHERE id = ?")) {
			st.setInt(1, empresaId);
			try (ResultSet rs = st.executeQuery()) {
				List<Map<String, Object>> l = Banco.linhas(rs);
				if (l.isEmpty()) {
					throw ErroNegocio.naoEncontrado("Empresa não encontrada.");
				}
				return l.get(0);
			}
		}
	}
}
