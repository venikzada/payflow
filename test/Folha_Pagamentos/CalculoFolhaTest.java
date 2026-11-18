package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CalculoFolhaTest {

	static final List<CalculoFolha.DescontoBeneficio> JOAO = List.of(
			new CalculoFolha.DescontoBeneficio(1, "Vale Transporte", 150),
			new CalculoFolha.DescontoBeneficio(2, "Vale Refeição", 50),
			new CalculoFolha.DescontoBeneficio(4, "Plano de Saúde", 100));

	static CalculoFolha.Resultado calc(double sal, double he50, double he100, double faltas, double atrasos,
			List<CalculoFolha.DescontoBeneficio> b) {
		return CalculoFolha.calcular(new CalculoFolha.Entrada(sal, he50, he100, faltas, atrasos, b));
	}

	@Test
	void joaoSemExtras() {
		var r = calc(2500, 0, 0, 0, 0, JOAO);
		assertEquals(200.69, r.inss());
		assertEquals(0.0, r.irrf());
		assertEquals(200.00, r.fgts());
		assertEquals(500.69, r.totalDescontos());
		assertEquals(1999.31, r.liquido());
		assertEquals(6, r.linhas().size());
	}

	@Test
	void joaoComDezHorasExtras50() {
		var r = calc(2500, 10, 0, 0, 0, JOAO);
		assertEquals(170.45, r.valorHE50());
		assertEquals(2670.45, r.totalProventos());
		assertEquals(216.03, r.inss());
		assertEquals(213.64, r.fgts());
		assertEquals(2154.42, r.liquido());
	}

	@Test
	void joaoComUmaFalta() {
		var r = calc(2500, 0, 0, 1, 0, JOAO);
		assertEquals(83.33, r.valorFaltas());
		assertEquals(2416.67, r.baseTributavel());
		assertEquals(193.19, r.inss());
		assertEquals(193.33, r.fgts());
		assertEquals(576.52, r.totalDescontos());
		assertEquals(1923.48, r.liquido());
	}

	@Test
	void horaExtra100EAtraso() {
		assertEquals(45.45, calc(2500, 0, 2, 0, 0, List.of()).valorHE100());
		assertEquals(34.09, calc(2500, 0, 0, 0, 3, List.of()).valorAtrasos());
	}

	@Test
	void carlosPagaIrrf() {
		var r = calc(6200, 0, 0, 0, 0, List.of());
		assertEquals(669.51, r.inss());
		assertEquals(459.03, r.irrf());
		assertEquals(496.00, r.fgts());
		assertEquals(5071.46, r.liquido());
	}

	@Test
	void mariaIgnoraBeneficioSemDesconto() {
		var r = calc(3500, 0, 0, 0, 0, List.of(
				new CalculoFolha.DescontoBeneficio(1, "Vale Transporte", 150),
				new CalculoFolha.DescontoBeneficio(3, "Vale Alimentação", 0),
				new CalculoFolha.DescontoBeneficio(5, "Plano Odontológico", 25)));
		assertEquals(308.60, r.inss());
		assertEquals(3016.40, r.liquido());
		assertTrue(r.linhas().stream().noneMatch(l -> l.descricao().equals("Vale Alimentação")));
	}

	@Test
	void linhasUsamNomesDosTipos() {
		var r = calc(2500, 10, 0, 1, 0, JOAO);
		assertEquals(List.of("Salário Base", "Hora Extra 50%", "Falta", "INSS", "IRPF", "Vale Transporte", "Vale Refeição", "Plano de Saúde"),
				r.linhas().stream().map(CalculoFolha.Linha::descricao).toList());
		assertEquals(List.of(1, 2, 13, 11, 12, 9, 10, 16),
				r.linhas().stream().map(CalculoFolha.Linha::tipoLancamentoId).toList());
	}

	@Test
	void tipoDoBeneficio() {
		assertEquals(9, CalculoFolha.tipoDoBeneficio("Vale Transporte"));
		assertEquals(10, CalculoFolha.tipoDoBeneficio("Vale Refeição"));
		assertEquals(16, CalculoFolha.tipoDoBeneficio("Vale Alimentação"));
		assertEquals(16, CalculoFolha.tipoDoBeneficio("Vale Combustível"));
		assertEquals(16, CalculoFolha.tipoDoBeneficio("Plano de Saúde"));
	}

	@Test
	void mapaUsaNomesDoBanco() {
		Map<String, Object> m = calc(2500, 0, 0, 0, 0, JOAO).paraMapa();
		assertEquals(1999.31, m.get("salario_liquido"));
		assertEquals(200.69, m.get("valor_inss"));
		assertTrue(m.containsKey("linhas"));
		assertTrue(m.containsKey("valor_hora"));
	}
}
