package Folha_Pagamentos;

import java.time.LocalDate;
import java.util.Map;

// Rotas de folhas: simular, lançar, pendentes, confirmar, apagar, histórico, holerite e resumo do Início
final class ApiFolhas {

	private ApiFolhas() {
	}

	private static CalculoFolha.Entrada entrada(Requisicao r) throws Exception {
		return FolhaDAO.montarEntrada(r.conn, r.empresaId, r.inteiro("funcionario_id"), r.numero("he50"),
				r.numero("he100"), r.numero("faltas"), r.numero("atrasos"));
	}

	static void registrar(Roteador rotas) {
		rotas.post("/api/folhas/simular", r -> {
			LocalDate hoje = r.hoje();
			Map<String, Object> m = CalculoFolha.calcular(entrada(r)).paraMapa();
			m.put("ja_lancada", FolhaDAO.jaLancada(r.conn, r.inteiro("funcionario_id"), hoje.getMonthValue(), hoje.getYear()));
			m.put("mes", hoje.getMonthValue());
			m.put("ano", hoje.getYear());
			return m;
		});

		rotas.post("/api/folhas", r -> {
			LocalDate hoje = r.hoje();
			int id = FolhaDAO.lancar(r.conn, r.empresaId, r.inteiro("funcionario_id"), entrada(r), hoje.getMonthValue(),
					hoje.getYear());
			return Resposta.criada(FolhaDAO.holerite(r.conn, r.empresaId, id));
		});

		rotas.get("/api/folhas", r -> {
			if (r.texto("status").equalsIgnoreCase("PENDENTE")) {
				return FolhaDAO.pendentes(r.conn, r.empresaId);
			}
			if (!r.texto("funcionario_id").isEmpty()) {
				int id = r.inteiro("funcionario_id");
				FuncionarioDAO.buscar(r.conn, r.empresaId, id, r.hoje());
				return FolhaDAO.historicoFuncionario(r.conn, r.empresaId, id);
			}
			if (!r.texto("mes").isEmpty() && !r.texto("ano").isEmpty()) {
				return FolhaDAO.historicoMes(r.conn, r.empresaId, mes(r), r.inteiro("ano"));
			}
			throw ErroNegocio.invalido("Informe status, funcionario_id ou mes e ano.");
		});

		rotas.get("/api/folhas/(\\d+)", r -> FolhaDAO.holerite(r.conn, r.empresaId, r.grupo(0)));

		rotas.post("/api/folhas/(\\d+)/confirmar", r -> {
			FolhaDAO.confirmar(r.conn, r.empresaId, r.grupo(0));
			return Map.of("ok", true);
		});

		rotas.delete("/api/folhas/(\\d+)", r -> {
			FolhaDAO.apagar(r.conn, r.empresaId, r.grupo(0));
			return Map.of("ok", true);
		});

		rotas.get("/api/resumo", r -> {
			LocalDate hoje = r.hoje();
			int mes = r.texto("mes").isEmpty() ? hoje.getMonthValue() : mes(r);
			int ano = r.texto("ano").isEmpty() ? hoje.getYear() : r.inteiro("ano");
			return FolhaDAO.resumo(r.conn, r.empresaId, mes, ano, hoje);
		});
	}

	private static int mes(Requisicao r) {
		int mes = r.inteiro("mes");
		if (mes < 1 || mes > 12) {
			throw ErroNegocio.invalido("mes", "Mês inválido. Digite de 1 a 12.");
		}
		return mes;
	}
}
