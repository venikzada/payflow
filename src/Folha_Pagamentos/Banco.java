package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Funções comuns de banco: abrir conexão, preparar bancos antigos e ler resultados
public final class Banco {

	private Banco() {
	}

	public static Connection abrir(String url) throws SQLException {
		return DriverManager.getConnection(url);
	}

	// Deixa qualquer banco (novo ou antigo) pronto para o sistema. Pode rodar várias vezes.
	public static void preparar(Connection c) throws SQLException {
		if (!temColuna(c, "funcionarios", "email")) {
			c.createStatement().executeUpdate("ALTER TABLE funcionarios ADD COLUMN email TEXT");
		}
		if (!temColuna(c, "funcionarios", "ativo")) {
			c.createStatement().executeUpdate("ALTER TABLE funcionarios ADD COLUMN ativo INTEGER NOT NULL DEFAULT 1");
		}

		c.createStatement().executeUpdate("INSERT OR IGNORE INTO tipos_lancamento VALUES "
				+ "(16, 'Benefício', 'DESCONTO', 'Desconto de benefício do funcionário', 0, 0, 0, 'SOMA')");

		try (PreparedStatement st = c.prepareStatement(
				"INSERT OR IGNORE INTO beneficios (nome, descricao, tipo, valor_padrao) VALUES (?, ?, ?, ?)")) {
			st.setString(1, "Vale Combustível");
			st.setString(2, "Benefício destinado ao abastecimento do veículo do funcionário");
			st.setString(3, "TRANSPORTE");
			st.setDouble(4, 200.0);
			st.executeUpdate();
		}

		// Senhas antigas em texto puro viram "salt:hash"
		List<Map<String, Object>> semHash;
		try (ResultSet rs = c.createStatement().executeQuery("SELECT id, senha FROM usuarios WHERE instr(senha, ':') = 0")) {
			semHash = linhas(rs);
		}
		for (Map<String, Object> u : semHash) {
			try (PreparedStatement st = c.prepareStatement("UPDATE usuarios SET senha = ? WHERE id = ?")) {
				st.setString(1, Senha.criarHash((String) u.get("senha")));
				st.setInt(2, ((Number) u.get("id")).intValue());
				st.executeUpdate();
			}
		}
	}

	private static boolean temColuna(Connection c, String tabela, String coluna) throws SQLException {
		try (ResultSet rs = c.createStatement().executeQuery("PRAGMA table_info(" + tabela + ")")) {
			while (rs.next()) {
				if (rs.getString("name").equalsIgnoreCase(coluna)) {
					return true;
				}
			}
		}
		return false;
	}

	// Transforma a linha atual do ResultSet num Map (coluna → valor)
	public static Map<String, Object> linha(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 1; i <= md.getColumnCount(); i++) {
			m.put(md.getColumnLabel(i), rs.getObject(i));
		}
		return m;
	}

	public static List<Map<String, Object>> linhas(ResultSet rs) throws SQLException {
		List<Map<String, Object>> lista = new ArrayList<>();
		while (rs.next()) {
			lista.add(linha(rs));
		}
		return lista;
	}
}
