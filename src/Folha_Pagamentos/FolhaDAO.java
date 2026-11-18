package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Acesso ao banco para as folhas de pagamento (usado pelo console e pela versão web)
public final class FolhaDAO {

	private FolhaDAO() {
	}

	// ===================== AUXILIARES =====================

	private static PreparedStatement preparar(Connection c, String sql, Object... params) throws SQLException {
		PreparedStatement st = c.prepareStatement(sql);
		for (int i = 0; i < params.length; i++) {
			st.setObject(i + 1, params[i]);
		}
		return st;
	}

	private static List<Map<String, Object>> consultar(Connection c, String sql, Object... params) throws SQLException {
		try (PreparedStatement st = preparar(c, sql, params); ResultSet rs = st.executeQuery()) {
			return Banco.linhas(rs);
		}
	}

	private static Map<String, Object> primeira(Connection c, String sql, Object... params) throws SQLException {
		List<Map<String, Object>> l = consultar(c, sql, params);
		return l.isEmpty() ? null : l.get(0);
	}

	private static double numero(Object o) {
		return o == null ? 0 : ((Number) o).doubleValue();
	}

	// ===================== MONTAR E LANÇAR =====================

	// Busca o salário e os benefícios do funcionário e monta a entrada do cálculo
	public static CalculoFolha.Entrada montarEntrada(Connection c, int empresaId, int funcionarioId, double he50,
			double he100, double diasFalta, double horasAtraso) throws SQLException {
		if (he50 < 0 || he100 < 0 || diasFalta < 0 || horasAtraso < 0) {
			throw ErroNegocio.invalido("Horas extras, faltas e atrasos não podem ser negativos.");
		}
		if (he50 > CalculoFolha.HORAS_MES || he100 > CalculoFolha.HORAS_MES) {
			throw ErroNegocio.invalido("Horas extras não podem passar de 220 no mês.");
		}
		if (diasFalta > CalculoFolha.DIAS_MES) {
			throw ErroNegocio.invalido("Dias de falta não podem passar de 30.");
		}
		if (horasAtraso > CalculoFolha.HORAS_MES) {
			throw ErroNegocio.invalido("Horas de atraso não podem passar de 220.");
		}
		Map<String, Object> f = primeira(c,
				"SELECT salario_base FROM funcionarios WHERE id = ? AND empresa_id = ? AND ativo = 1", funcionarioId,
				empresaId);
		if (f == null) {
			throw ErroNegocio.naoEncontrado("Funcionário não encontrado.");
		}
		List<CalculoFolha.DescontoBeneficio> beneficios = new ArrayList<>();
		for (Map<String, Object> b : consultar(c,
				"SELECT b.id, b.nome, fb.desconto_folha FROM funcionarios_beneficios fb "
						+ "JOIN beneficios b ON b.id = fb.beneficio_id "
						+ "WHERE fb.funcionario_id = ? AND fb.ativo = 1 AND fb.desconto_folha > 0 ORDER BY fb.id",
				funcionarioId)) {
			beneficios.add(new CalculoFolha.DescontoBeneficio(((Number) b.get("id")).intValue(), (String) b.get("nome"),
					numero(b.get("desconto_folha"))));
		}
		return new CalculoFolha.Entrada(numero(f.get("salario_base")), he50, he100, diasFalta, horasAtraso,
				beneficios);
	}

	public static boolean jaLancada(Connection c, int funcionarioId, int mes, int ano) throws SQLException {
		return primeira(c,
				"SELECT 1 FROM folha_funcionarios ff JOIN folhas_pagamento fp ON fp.id = ff.folha_id "
						+ "WHERE ff.funcionario_id = ? AND fp.mes = ? AND fp.ano = ?",
				funcionarioId, mes, ano) != null;
	}

	// Calcula e grava a folha do funcionário como PENDENTE. Devolve o id em folha_funcionarios.
	public static int lancar(Connection c, int empresaId, int funcionarioId, CalculoFolha.Entrada e, int mes, int ano)
			throws SQLException {
		if (primeira(c, "SELECT 1 FROM funcionarios WHERE id = ? AND empresa_id = ? AND ativo = 1", funcionarioId,
				empresaId) == null) {
			throw ErroNegocio.naoEncontrado("Funcionário não encontrado.");
		}
		if (jaLancada(c, funcionarioId, mes, ano)) {
			throw ErroNegocio.conflito("Esse funcionário já foi lançado neste mês.");
		}
		CalculoFolha.Resultado r = CalculoFolha.calcular(e);

		boolean autoCommit = c.getAutoCommit();
		c.setAutoCommit(false);
		try {
			int folhaId = folhaDoMes(c, mes, ano);
			int id;
			try (PreparedStatement st = c.prepareStatement(
					"INSERT INTO folha_funcionarios (folha_id, funcionario_id, salario_base, total_proventos, salario_bruto, "
							+ "base_inss, valor_inss, base_fgts, valor_fgts, base_irpf, valor_irpf, total_descontos, "
							+ "salario_liquido, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDENTE')",
					Statement.RETURN_GENERATED_KEYS)) {
				Object[] v = { folhaId, funcionarioId, r.salarioBase(), r.totalProventos(), r.totalProventos(),
						r.baseTributavel(), r.inss(), r.baseTributavel(), r.fgts(), r.baseIrrf(), r.irrf(),
						r.totalDescontos(), r.liquido() };
				for (int i = 0; i < v.length; i++) {
					st.setObject(i + 1, v[i]);
				}
				st.executeUpdate();
				try (ResultSet chave = st.getGeneratedKeys()) {
					chave.next();
					id = chave.getInt(1);
				}
			}
			for (CalculoFolha.Linha l : r.linhas()) {
				try (PreparedStatement st = preparar(c,
						"INSERT INTO lancamentos_folha (folha_funcionario_id, tipo_lancamento_id, referencia, valor, descricao) "
								+ "VALUES (?, ?, ?, ?, ?)",
						id, l.tipoLancamentoId(), l.referencia(), l.valor(), l.descricao())) {
					st.executeUpdate();
				}
			}
			c.commit();
			return id;
		} catch (SQLException | RuntimeException ex) {
			c.rollback();
			throw ex;
		} finally {
			c.setAutoCommit(autoCommit);
		}
	}

	// Usa a folha do mês/ano, ou cria uma nova se não existir
	private static int folhaDoMes(Connection c, int mes, int ano) throws SQLException {
		Map<String, Object> f = primeira(c, "SELECT id FROM folhas_pagamento WHERE mes = ? AND ano = ?", mes, ano);
		if (f != null) {
			return ((Number) f.get("id")).intValue();
		}
		try (PreparedStatement st = c.prepareStatement(
				"INSERT INTO folhas_pagamento (mes, ano, data_pagamento, status) VALUES (?, ?, ?, 'ABERTA')",
				Statement.RETURN_GENERATED_KEYS)) {
			st.setInt(1, mes);
			st.setInt(2, ano);
			st.setString(3, YearMonth.of(ano, mes).atEndOfMonth().toString());
			st.executeUpdate();
			try (ResultSet chave = st.getGeneratedKeys()) {
				chave.next();
				return chave.getInt(1);
			}
		}
	}

	// ===================== PENDENTES =====================

	public static List<Map<String, Object>> pendentes(Connection c, int empresaId) throws SQLException {
		return consultar(c,
				"SELECT ff.id, fp.mes, fp.ano, fu.id AS funcionario_id, fu.nome, fu.cargo, ff.salario_liquido "
						+ "FROM folha_funcionarios ff JOIN folhas_pagamento fp ON fp.id = ff.folha_id "
						+ "JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE fu.empresa_id = ? AND ff.status = 'PENDENTE' ORDER BY fp.ano, fp.mes, fu.nome",
				empresaId);
	}

	public static void confirmar(Connection c, int empresaId, int id) throws SQLException {
		try (PreparedStatement st = preparar(c,
				"UPDATE folha_funcionarios SET status = 'CONFIRMADA' WHERE id = ? AND status = 'PENDENTE' "
						+ "AND funcionario_id IN (SELECT id FROM funcionarios WHERE empresa_id = ?)",
				id, empresaId)) {
			if (st.executeUpdate() == 0) {
				throw ErroNegocio.naoEncontrado("Folha pendente não encontrada.");
			}
		}
	}

	public static void apagar(Connection c, int empresaId, int id) throws SQLException {
		Map<String, Object> f = primeira(c,
				"SELECT ff.status FROM folha_funcionarios ff JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE ff.id = ? AND fu.empresa_id = ?",
				id, empresaId);
		if (f == null) {
			throw ErroNegocio.naoEncontrado("Folha não encontrada.");
		}
		if (!"PENDENTE".equals(f.get("status"))) {
			throw ErroNegocio.conflito("Só folhas pendentes podem ser apagadas.");
		}
		boolean autoCommit = c.getAutoCommit();
		c.setAutoCommit(false);
		try {
			try (PreparedStatement st = preparar(c, "DELETE FROM lancamentos_folha WHERE folha_funcionario_id = ?", id)) {
				st.executeUpdate();
			}
			try (PreparedStatement st = preparar(c,
					"DELETE FROM folha_funcionarios WHERE id = ? AND status = 'PENDENTE'", id)) {
				st.executeUpdate();
			}
			c.commit();
		} catch (SQLException | RuntimeException ex) {
			c.rollback();
			throw ex;
		} finally {
			c.setAutoCommit(autoCommit);
		}
	}

	// ===================== HOLERITE =====================

	public static Map<String, Object> holerite(Connection c, int empresaId, int id) throws SQLException {
		Map<String, Object> f = primeira(c,
				"SELECT ff.id, fp.mes, fp.ano, ff.status, fp.data_pagamento, e.nome_fantasia, e.cnpj, "
						+ "fu.id AS funcionario_id, fu.nome, fu.cargo, fu.cpf, ff.salario_base, ff.total_proventos, "
						+ "ff.total_descontos, ff.salario_liquido, ff.valor_fgts, ff.base_inss, ff.base_irpf, "
						+ "ff.valor_inss, ff.valor_irpf "
						+ "FROM folha_funcionarios ff JOIN folhas_pagamento fp ON fp.id = ff.folha_id "
						+ "JOIN funcionarios fu ON fu.id = ff.funcionario_id JOIN empresas e ON e.id = fu.empresa_id "
						+ "WHERE ff.id = ? AND fu.empresa_id = ?",
				id, empresaId);
		if (f == null) {
			throw ErroNegocio.naoEncontrado("Folha não encontrada.");
		}

		List<Map<String, Object>> linhas = new ArrayList<>();
		for (Map<String, Object> l : consultar(c,
				"SELECT lf.tipo_lancamento_id, lf.descricao, lf.referencia, lf.valor, tl.nome, tl.tipo "
						+ "FROM lancamentos_folha lf JOIN tipos_lancamento tl ON tl.id = lf.tipo_lancamento_id "
						+ "WHERE lf.folha_funcionario_id = ? ORDER BY (tl.tipo = 'DESCONTO'), lf.id",
				id)) {
			boolean beneficio = ((Number) l.get("tipo_lancamento_id")).intValue() == 16;
			linhas.add(linhaHolerite(beneficio ? (String) l.get("descricao") : (String) l.get("nome"),
					l.get("referencia"), numero(l.get("valor")), (String) l.get("tipo")));
		}
		if (linhas.isEmpty()) {
			linhas.add(linhaHolerite("Salário Base", null, numero(f.get("salario_base")), "PROVENTO"));
			linhas.add(linhaHolerite("INSS", null, numero(f.get("valor_inss")), "DESCONTO"));
			linhas.add(linhaHolerite("IRPF", null, numero(f.get("valor_irpf")), "DESCONTO"));
		}

		// O que sobrar dos descontos (folhas antigas) aparece como "Benefícios"
		double somaDescontos = 0;
		for (Map<String, Object> l : linhas) {
			if ("DESCONTO".equals(l.get("tipo"))) {
				somaDescontos += (Double) l.get("valor");
			}
		}
		double residuo = numero(f.get("total_descontos")) - somaDescontos;
		if (residuo > 0.005) {
			linhas.add(linhaHolerite("Benefícios", null, CalculoFolha.centavos(residuo), "DESCONTO"));
		}

		Map<String, Object> empresa = new LinkedHashMap<>();
		empresa.put("nome_fantasia", f.get("nome_fantasia"));
		empresa.put("cnpj", f.get("cnpj"));
		Map<String, Object> funcionario = new LinkedHashMap<>();
		funcionario.put("id", f.get("funcionario_id"));
		funcionario.put("nome", f.get("nome"));
		funcionario.put("cargo", f.get("cargo"));
		funcionario.put("cpf", f.get("cpf"));

		Map<String, Object> h = new LinkedHashMap<>();
		h.put("id", f.get("id"));
		h.put("mes", f.get("mes"));
		h.put("ano", f.get("ano"));
		h.put("status", f.get("status"));
		h.put("data_pagamento", f.get("data_pagamento"));
		h.put("empresa", empresa);
		h.put("funcionario", funcionario);
		h.put("linhas", linhas);
		for (String k : List.of("salario_base", "total_proventos", "total_descontos", "salario_liquido", "valor_fgts",
				"base_inss", "base_irpf", "valor_inss", "valor_irpf")) {
			h.put(k, numero(f.get(k)));
		}
		return h;
	}

	private static Map<String, Object> linhaHolerite(String descricao, Object referencia, double valor, String tipo) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("descricao", descricao);
		m.put("referencia", referencia);
		m.put("valor", valor);
		m.put("tipo", tipo);
		return m;
	}

	// ===================== HISTÓRICO =====================

	public static List<Map<String, Object>> historicoFuncionario(Connection c, int empresaId, int funcionarioId)
			throws SQLException {
		return consultar(c,
				"SELECT ff.id, fp.mes, fp.ano, ff.status, ff.salario_base, ff.total_proventos, ff.total_descontos, "
						+ "ff.salario_liquido, ff.valor_fgts FROM folha_funcionarios ff "
						+ "JOIN folhas_pagamento fp ON fp.id = ff.folha_id JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE ff.funcionario_id = ? AND fu.empresa_id = ? ORDER BY fp.ano DESC, fp.mes DESC",
				funcionarioId, empresaId);
	}

	public static Map<String, Object> historicoMes(Connection c, int empresaId, int mes, int ano) throws SQLException {
		List<Map<String, Object>> folhas = consultar(c,
				"SELECT ff.id, fu.id AS funcionario_id, fu.nome, fu.cargo, ff.status, ff.salario_base, ff.total_proventos, "
						+ "ff.total_descontos, ff.salario_liquido, ff.valor_fgts FROM folha_funcionarios ff "
						+ "JOIN folhas_pagamento fp ON fp.id = ff.folha_id JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE fp.mes = ? AND fp.ano = ? AND fu.empresa_id = ? ORDER BY fu.nome",
				mes, ano, empresaId);
		double proventos = 0, descontos = 0, liquido = 0, fgts = 0;
		for (Map<String, Object> f : folhas) {
			proventos += numero(f.get("total_proventos"));
			descontos += numero(f.get("total_descontos"));
			liquido += numero(f.get("salario_liquido"));
			fgts += numero(f.get("valor_fgts"));
		}
		Map<String, Object> totais = new LinkedHashMap<>();
		totais.put("quantidade", folhas.size());
		totais.put("proventos", CalculoFolha.centavos(proventos));
		totais.put("descontos", CalculoFolha.centavos(descontos));
		totais.put("liquido", CalculoFolha.centavos(liquido));
		totais.put("fgts", CalculoFolha.centavos(fgts));

		Map<String, Object> m = new LinkedHashMap<>();
		m.put("mes", mes);
		m.put("ano", ano);
		m.put("folhas", folhas);
		m.put("totais", totais);
		return m;
	}

	// ===================== RESUMO DO INÍCIO =====================

	@SuppressWarnings("unchecked")
	public static Map<String, Object> resumo(Connection c, int empresaId, int mes, int ano, java.time.LocalDate hoje)
			throws SQLException {
		YearMonth atual = YearMonth.of(ano, mes);
		Map<String, Object> doMes = historicoMes(c, empresaId, mes, ano);
		Map<String, Object> totais = (Map<String, Object>) doMes.get("totais");
		YearMonth anterior = atual.minusMonths(1);
		double liquidoAnterior = numero(((Map<String, Object>) historicoMes(c, empresaId, anterior.getMonthValue(),
				anterior.getYear()).get("totais")).get("liquido"));
		double liquido = numero(totais.get("liquido"));

		List<Map<String, Object>> serie = new ArrayList<>();
		for (int i = 5; i >= 0; i--) {
			YearMonth ym = atual.minusMonths(i);
			Map<String, Object> t = (Map<String, Object>) historicoMes(c, empresaId, ym.getMonthValue(), ym.getYear())
					.get("totais");
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("mes", ym.getMonthValue());
			item.put("ano", ym.getYear());
			item.put("liquido", t.get("liquido"));
			item.put("descontos", t.get("descontos"));
			serie.add(item);
		}

		Map<String, Object> ativos = primeira(c, "SELECT COUNT(*) AS n FROM funcionarios WHERE empresa_id = ? AND ativo = 1",
				empresaId);

		Map<String, Object> m = new LinkedHashMap<>();
		m.put("mes", mes);
		m.put("ano", ano);
		m.put("liquido_total", liquido);
		m.put("descontos_total", totais.get("descontos"));
		m.put("fgts_total", totais.get("fgts"));
		m.put("variacao_liquido",
				liquidoAnterior == 0 ? null : (int) Math.round((liquido - liquidoAnterior) / liquidoAnterior * 100));
		m.put("funcionarios", ((Number) ativos.get("n")).intValue());
		m.put("em_experiencia", FuncionarioDAO.listar(c, empresaId, "experiencia", "", hoje).size());
		m.put("pendentes", pendentes(c, empresaId).size());
		m.put("serie", serie);
		m.put("folhas", doMes.get("folhas"));
		return m;
	}
}
