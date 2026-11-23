package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Scanner;

public class FolhaPagamento {

	// Lista as folhas pendentes e deixa confirmar ou apagar
	public static void folhasAConfirmar(Scanner sc, Connection conn, int empresaId) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
				"SELECT ff.id, fp.mes, fp.ano, fu.nome, ff.salario_liquido FROM folha_funcionarios ff "
						+ "JOIN folhas_pagamento fp ON fp.id = ff.folha_id "
						+ "JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE fu.empresa_id = ? AND ff.status = 'PENDENTE' ORDER BY fp.ano, fp.mes, fu.nome");
		stmt.setInt(1, empresaId);
		ResultSet rs = stmt.executeQuery();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		// Mostra as folhas pendentes
		System.out.println();
		System.out.println("=========== FOLHAS A CONFIRMAR ===========");
		boolean achou = false;
		while (rs.next()) {
			achou = true;
			System.out.println(rs.getInt("id") + " - " + String.format("%02d/%d", rs.getInt("mes"), rs.getInt("ano"))
					+ " - " + rs.getString("nome") + " | Líquido: " + moeda.format(rs.getDouble("salario_liquido")));
		}
		if (!achou) {
			System.out.println("Nenhuma folha pendente.");
			return;
		}

		// Confere se a folha escolhida é pendente e da empresa
		System.out.print("Digite o ID da folha (0 para voltar): ");
		int id = sc.nextInt();
		sc.nextLine();
		if (id == 0) {
			return;
		}

		PreparedStatement stmtOk = conn.prepareStatement(
				"SELECT 1 FROM folha_funcionarios ff JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE ff.id = ? AND ff.status = 'PENDENTE' AND fu.empresa_id = ?");
		stmtOk.setInt(1, id);
		stmtOk.setInt(2, empresaId);
		if (!stmtOk.executeQuery().next()) {
			System.out.println("Folha pendente não encontrada.");
			return;
		}

		System.out.println("1 - Confirmar folha");
		System.out.println("2 - Apagar folha");
		System.out.println("0 - Voltar");
		System.out.print("Escolha: ");
		int opcao = sc.nextInt();
		sc.nextLine();

		if (opcao == 1) {
			// Confirma a folha
			PreparedStatement stmtConf = conn.prepareStatement(
					"UPDATE folha_funcionarios SET status = 'CONFIRMADA' WHERE id = ? AND status = 'PENDENTE'");
			stmtConf.setInt(1, id);
			stmtConf.executeUpdate();
			System.out.println("Folha confirmada com sucesso!");
		} else if (opcao == 2) {
			// Apaga a folha pendente depois de pedir confirmação
			String resposta = Leitor.lerObrigatorio(sc, "Tem certeza que quer apagar? (s/n): ");
			if (resposta.equalsIgnoreCase("s")) {
				FolhaDAO.apagar(conn, empresaId, id);
				System.out.println("Folha apagada com sucesso!");
			} else {
				System.out.println("Nada foi apagado.");
			}
		} else if (opcao != 0) {
			System.out.println("Opção inválida.");
		}
	}

	// Calcula e lança a folha de um funcionário (fica PENDENTE até confirmar)
	public static void lancarFolha(Scanner sc, Connection conn, int empresaId) throws SQLException {
		// Lista os funcionários ativos da empresa
		PreparedStatement stmtListar = conn
				.prepareStatement("SELECT id, nome, cargo FROM funcionarios WHERE empresa_id = ? AND ativo = 1");
		stmtListar.setInt(1, empresaId);
		ResultSet lista = stmtListar.executeQuery();

		System.out.println("Funcionários cadastrados:");
		while (lista.next()) {
			System.out.println(lista.getInt("id") + " - " + lista.getString("nome") + " - " + lista.getString("cargo"));
		}

		System.out.print("Digite o ID do funcionário: ");
		int idEscolhido = sc.nextInt();
		sc.nextLine();

		// Pede horas extras, faltas e atrasos (Enter = 0)
		double he50 = Leitor.lerNumeroOpcional(sc, "Horas extras 50% (Enter = 0): ");
		double he100 = Leitor.lerNumeroOpcional(sc, "Horas extras 100% (Enter = 0): ");
		double faltas = Leitor.lerNumeroOpcional(sc, "Dias de falta (Enter = 0): ");
		double atrasos = Leitor.lerNumeroOpcional(sc, "Horas de atraso (Enter = 0): ");

		// Calcula e grava usando as mesmas regras da versão web
		LocalDate hoje = LocalDate.now();
		int folhaId;
		try {
			CalculoFolha.Entrada entrada = FolhaDAO.montarEntrada(conn, empresaId, idEscolhido, he50, he100, faltas,
					atrasos);
			folhaId = FolhaDAO.lancar(conn, empresaId, idEscolhido, entrada, hoje.getMonthValue(), hoje.getYear());
		} catch (ErroNegocio e) {
			if (e.status() == 409) {
				System.out.println(e.getMessage() + " Veja a folha dele na opção 3 (Histórico).");
			} else {
				System.out.println(e.getMessage());
			}
			return;
		}

		imprimirHolerite(FolhaDAO.holerite(conn, empresaId, folhaId));
	}

	// Mostra o holerite na tela
	@SuppressWarnings("unchecked")
	static void imprimirHolerite(Map<String, Object> h) {
		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
		Map<String, Object> empresa = (Map<String, Object>) h.get("empresa");
		Map<String, Object> funcionario = (Map<String, Object>) h.get("funcionario");
		List<Map<String, Object>> linhas = (List<Map<String, Object>>) h.get("linhas");
		String cnpj = ((String) empresa.get("cnpj")).replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})",
				"$1.$2.$3/$4-$5");

		System.out.println();
		System.out.println("=========== FOLHA DE PAGAMENTO ===========");
		System.out.println("Empresa: " + empresa.get("nome_fantasia"));
		System.out.println("CNPJ: " + cnpj);
		System.out.println("Referência: " + String.format("%02d/%d", h.get("mes"), h.get("ano")));
		System.out.println("Funcionário: " + funcionario.get("nome"));
		System.out.println("Cargo: " + funcionario.get("cargo"));
		System.out.println("------------------------------------------");
		System.out.println("PROVENTOS");
		for (Map<String, Object> l : linhas) {
			if ("PROVENTO".equals(l.get("tipo"))) {
				System.out.println(String.format("%-17s", l.get("descricao") + ":") + moeda.format(l.get("valor")));
			}
		}
		System.out.println("Total Proventos: " + moeda.format(h.get("total_proventos")));
		System.out.println("------------------------------------------");
		System.out.println("DESCONTOS");
		for (Map<String, Object> l : linhas) {
			if ("DESCONTO".equals(l.get("tipo"))) {
				System.out.println(String.format("%-17s", l.get("descricao") + ":") + moeda.format(l.get("valor")));
			}
		}
		System.out.println("Total descontos: " + moeda.format(h.get("total_descontos")));
		System.out.println("------------------------------------------");
		System.out.println("SALÁRIO LÍQUIDO: " + moeda.format(h.get("salario_liquido")));
		System.out.println("------------------------------------------");
		System.out.println("FGTS (empresa):  " + moeda.format(h.get("valor_fgts")));
		System.out.println("==========================================");
	}
}
