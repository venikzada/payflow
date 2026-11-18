package Folha_Pagamentos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

// Auxiliar dos testes: cria um banco novo a partir do script do projeto
final class BancoTeste {

	private BancoTeste() {
	}

	static Path novoArquivo() throws Exception {
		Path arquivo = Files.createTempFile("folha-teste-", ".db");
		arquivo.toFile().deleteOnExit();
		String script = Files.readString(Path.of("database/folha2.db.sql"), StandardCharsets.UTF_8);
		try (Connection c = DriverManager.getConnection(url(arquivo)); Statement st = c.createStatement()) {
			for (String comando : script.split(";\\s*\\R")) {
				if (!comando.isBlank()) {
					st.execute(comando);
				}
			}
			Banco.preparar(c);
		}
		return arquivo;
	}

	static String url(Path p) {
		return "jdbc:sqlite:" + p;
	}

	static Connection novo() throws Exception {
		return DriverManager.getConnection(url(novoArquivo()));
	}

	static Object valor(Connection c, String sql, Object... params) throws Exception {
		try (PreparedStatement st = c.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				st.setObject(i + 1, params[i]);
			}
			try (ResultSet rs = st.executeQuery()) {
				return rs.next() ? rs.getObject(1) : null;
			}
		}
	}
}
