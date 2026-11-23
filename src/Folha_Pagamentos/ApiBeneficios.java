package Folha_Pagamentos;

import java.util.Map;

// Rotas de benefícios: catálogo, limite, dar, editar e remover
final class ApiBeneficios {

	private ApiBeneficios() {
	}

	static void registrar(Roteador rotas) {
		rotas.get("/api/beneficios", r -> BeneficioDAO.catalogo(r.conn));

		rotas.get("/api/beneficios/limite", r -> BeneficioDAO.limite(r.conn, r.empresaId, r.inteiro("funcionario_id"),
				r.inteiro("beneficio_id"), r.numero("valor"), r.numero("percentual")));

		rotas.get("/api/funcionarios/(\\d+)/beneficios", r -> BeneficioDAO.doFuncionario(r.conn, r.empresaId, r.grupo(0)));

		rotas.post("/api/funcionarios/(\\d+)/beneficios", r -> Resposta.criada(Map.of("id", BeneficioDAO.atribuir(r.conn,
				r.empresaId, r.grupo(0), r.inteiro("beneficio_id"), r.numero("valor"), r.numero("percentual"), r.hoje()))));

		rotas.put("/api/funcionarios-beneficios/(\\d+)", r -> {
			BeneficioDAO.editar(r.conn, r.empresaId, r.grupo(0), r.numero("valor"), r.numero("percentual"));
			return Map.of("ok", true);
		});

		rotas.delete("/api/funcionarios-beneficios/(\\d+)", r -> {
			BeneficioDAO.remover(r.conn, r.empresaId, r.grupo(0), r.hoje());
			return Map.of("ok", true);
		});
	}
}
