package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

class ConsoleTest {

	interface Acao {
		void rodar() throws Exception;
	}

	static String capturar(Acao acao) throws Exception {
		PrintStream original = System.out;
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		System.setOut(new PrintStream(saida, true, StandardCharsets.UTF_8));
		try {
			acao.rodar();
		} finally {
			System.setOut(original);
		}
		return saida.toString(StandardCharsets.UTF_8).replace(' ', ' ');
	}

	static Scanner scanner(String entrada) {
		Scanner sc = new Scanner(entrada);
		sc.useLocale(java.util.Locale.of("pt", "BR"));
		return sc;
	}

	private static int inteiro(Object o) {
		return ((Number) o).intValue();
	}

	@Test
	void numeroBR() {
		assertEquals(0.0, Leitor.numeroBR(""));
		assertEquals(1.5, Leitor.numeroBR("1,5"));
		assertEquals(1234.56, Leitor.numeroBR("1.234,56"));
		assertEquals(3500.0, Leitor.numeroBR("3500.00"));
		assertThrows(NumberFormatException.class, () -> Leitor.numeroBR("abc"));
	}

	@Test
	void lerNumeroOpcional() throws Exception {
		capturar(() -> {
			assertEquals(0.0, Leitor.lerNumeroOpcional(scanner("\n"), "X: "));
			assertEquals(3.0, Leitor.lerNumeroOpcional(scanner("-2\n3\n"), "X: "));
			assertEquals(1.5, Leitor.lerNumeroOpcional(scanner("1,5\n"), "X: "));
		});
	}

	@Test
	void lancarPeloConsoleComHorasExtras() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			String saida = capturar(() -> FolhaPagamento.lancarFolha(scanner("1\n10\n\n\n\n"), c, 1));
			LocalDate hoje = LocalDate.now();
			assertEquals(2154.42, BancoTeste.valor(c,
					"SELECT ff.salario_liquido FROM folha_funcionarios ff JOIN folhas_pagamento fp ON fp.id=ff.folha_id "
							+ "WHERE ff.funcionario_id=1 AND fp.mes=? AND fp.ano=?",
					hoje.getMonthValue(), hoje.getYear()));
			assertTrue(saida.contains("Hora Extra 50%"), saida);
			assertTrue(saida.contains("R$ 2.154,42"), saida);
		}
	}

	@Test
	void lancarDuasVezesPeloConsoleAvisa() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			capturar(() -> FolhaPagamento.lancarFolha(scanner("1\n\n\n\n\n"), c, 1));
			String saida = capturar(() -> FolhaPagamento.lancarFolha(scanner("1\n\n\n\n\n"), c, 1));
			assertTrue(saida.contains("Esse funcionário já foi lançado neste mês. Veja a folha dele na opção 3 (Histórico)."), saida);
		}
	}

	@Test
	void consoleEscondeDesligado() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			c.createStatement().executeUpdate("UPDATE funcionarios SET ativo=0 WHERE id=2");
			String s1 = capturar(() -> FuncionarioMenu.escolherFuncionario(scanner("0\n"), c, 1));
			String s2 = capturar(() -> FuncionarioMenu.listarFuncionarios(scanner("1\n"), c, 1));
			String s3 = capturar(() -> FolhaPagamento.lancarFolha(scanner("2\n\n\n\n\n"), c, 1));
			assertFalse(s1.contains("Maria Oliveira"));
			assertTrue(s1.contains("João da Silva"));
			assertFalse(s2.contains("Maria Oliveira"));
			assertTrue(s3.contains("Funcionário não encontrado."), s3);
		}
	}

	@Test
	void apagarPeloConsoleRemoveLinhas() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			int id = FolhaDAO.lancar(c, 1, 1, FolhaDAO.montarEntrada(c, 1, 1, 0, 0, 0, 0), 11, 2026);
			capturar(() -> FolhaPagamento.folhasAConfirmar(scanner(id + "\n2\ns\n"), c, 1));
			assertEquals(0, inteiro(BancoTeste.valor(c, "SELECT COUNT(*) FROM lancamentos_folha WHERE folha_funcionario_id=?", id)));
			assertEquals(0, inteiro(BancoTeste.valor(c, "SELECT COUNT(*) FROM folha_funcionarios WHERE id=?", id)));
		}
	}

	@Test
	void historicoSeparaFaltas() throws Exception {
		try (Connection c = BancoTeste.novo()) {
			FolhaDAO.lancar(c, 1, 1, FolhaDAO.montarEntrada(c, 1, 1, 0, 0, 1, 0), 11, 2026);
			String s = capturar(() -> Historico.historicoFuncionario(scanner("1\n"), c, 1));
			assertTrue(s.contains("Faltas/atrasos:  R$ 83,33"), s);
			assertTrue(s.contains("Benefícios:      R$ 300,00"), s);
		}
	}

	@Test
	void tempoDeEmpresaComData() {
		assertEquals("10 meses e 4 dias", FuncionarioMenu.tempoDeEmpresa(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 11, 14)));
	}
}
