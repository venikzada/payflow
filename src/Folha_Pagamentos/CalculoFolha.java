package Folha_Pagamentos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Cálculo da folha de um funcionário: proventos, INSS, IRRF, FGTS, benefícios e líquido.
// Não acessa o banco: recebe os números e devolve o holerite pronto.
public final class CalculoFolha {

	public static final double HORAS_MES = 220;
	public static final double DIAS_MES = 30;

	public record DescontoBeneficio(int beneficioId, String nome, double valor) {
	}

	public record Entrada(double salarioBase, double horasExtras50, double horasExtras100, double diasFalta,
			double horasAtraso, List<DescontoBeneficio> beneficios) {
	}

	public record Linha(int tipoLancamentoId, String descricao, Double referencia, double valor, boolean desconto) {
	}

	public record Resultado(double salarioBase, double valorHora, double valorHE50, double valorHE100,
			double valorFaltas, double valorAtrasos, double totalProventos, double baseTributavel, double inss,
			double baseIrrf, double irrf, double fgts, double totalBeneficios, double totalDescontos, double liquido,
			List<Linha> linhas) {

		public Map<String, Object> paraMapa() {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("salario_base", salarioBase);
			m.put("valor_hora", centavos(valorHora));
			m.put("valor_he50", valorHE50);
			m.put("valor_he100", valorHE100);
			m.put("valor_faltas", valorFaltas);
			m.put("valor_atrasos", valorAtrasos);
			m.put("total_proventos", totalProventos);
			m.put("base_inss", baseTributavel);
			m.put("base_fgts", baseTributavel);
			m.put("base_irpf", baseIrrf);
			m.put("valor_inss", inss);
			m.put("valor_irpf", irrf);
			m.put("valor_fgts", fgts);
			m.put("total_beneficios", totalBeneficios);
			m.put("total_descontos", totalDescontos);
			m.put("salario_liquido", liquido);
			List<Map<String, Object>> itens = new ArrayList<>();
			for (Linha l : linhas) {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("tipo_lancamento_id", l.tipoLancamentoId());
				item.put("descricao", l.descricao());
				item.put("referencia", l.referencia());
				item.put("valor", l.valor());
				item.put("desconto", l.desconto());
				itens.add(item);
			}
			m.put("linhas", itens);
			return m;
		}
	}

	private CalculoFolha() {
	}

	public static double centavos(double v) {
		return Math.round(v * 100) / 100.0;
	}

	// Tipo de lançamento usado para guardar o desconto de cada benefício
	public static int tipoDoBeneficio(String nome) {
		String n = nome.toLowerCase(Locale.of("pt", "BR"));
		if (n.contains("transporte")) {
			return 9;
		}
		if (n.contains("refei")) {
			return 10;
		}
		return 16;
	}

	public static Resultado calcular(Entrada e) {
		double salario = e.salarioBase();
		double valorHora = salario / HORAS_MES;

		// Proventos
		double he50 = centavos(valorHora * 1.5 * e.horasExtras50());
		double he100 = centavos(valorHora * 2 * e.horasExtras100());
		double totalProventos = centavos(salario + he50 + he100);

		// Faltas e atrasos diminuem a base dos impostos
		double faltas = centavos(salario / DIAS_MES * e.diasFalta());
		double atrasos = centavos(valorHora * e.horasAtraso());
		double base = Math.max(0, centavos(totalProventos - faltas - atrasos));

		// Impostos (mesmas regras do console)
		descontos impostos = new descontos();
		double inss = impostos.InssDesconto(base);
		double irrf = impostos.IrrfDesconto(base, inss);
		double baseIrrf = centavos(base - inss);
		double fgts = centavos(base * 0.08);

		// Linhas do holerite, com os nomes da tabela tipos_lancamento
		List<Linha> linhas = new ArrayList<>();
		linhas.add(new Linha(1, "Salário Base", 30.0, salario, false));
		if (he50 > 0) {
			linhas.add(new Linha(2, "Hora Extra 50%", e.horasExtras50(), he50, false));
		}
		if (he100 > 0) {
			linhas.add(new Linha(3, "Hora Extra 100%", e.horasExtras100(), he100, false));
		}
		if (faltas > 0) {
			linhas.add(new Linha(13, "Falta", e.diasFalta(), faltas, true));
		}
		if (atrasos > 0) {
			linhas.add(new Linha(14, "Atraso", e.horasAtraso(), atrasos, true));
		}
		linhas.add(new Linha(11, "INSS", null, inss, true));
		linhas.add(new Linha(12, "IRPF", null, irrf, true));

		double totalBeneficios = 0;
		for (DescontoBeneficio b : e.beneficios()) {
			double valor = centavos(b.valor());
			if (valor > 0) {
				totalBeneficios += valor;
				linhas.add(new Linha(tipoDoBeneficio(b.nome()), b.nome(), null, valor, true));
			}
		}
		totalBeneficios = centavos(totalBeneficios);

		double totalDescontos = centavos(inss + irrf + totalBeneficios + faltas + atrasos);
		double liquido = centavos(totalProventos - totalDescontos);

		return new Resultado(salario, valorHora, he50, he100, faltas, atrasos, totalProventos, base, inss, baseIrrf,
				irrf, fgts, totalBeneficios, totalDescontos, liquido, List.copyOf(linhas));
	}
}
