package Folha_Pagamentos;

import java.util.Map;

// Rotas de funcionários e cargos
final class ApiFuncionarios {

	private ApiFuncionarios() {
	}

	private static FuncionarioDAO.Dados dados(Requisicao r) {
		return FuncionarioDAO.Dados.validar(r.texto("nome"), r.texto("cpf"), r.texto("email"), r.texto("data_nascimento"),
				r.texto("data_admissao"), r.texto("cargo"), r.texto("salario_base"));
	}

	static void registrar(Roteador rotas) {
		rotas.get("/api/funcionarios", r -> FuncionarioDAO.listar(r.conn, r.empresaId, r.texto("filtro"), r.texto("busca"), r.hoje()));

		rotas.get("/api/funcionarios/(\\d+)", r -> FuncionarioDAO.buscar(r.conn, r.empresaId, r.grupo(0), r.hoje()));

		rotas.post("/api/funcionarios", r -> Resposta.criada(Map.of("id", FuncionarioDAO.inserir(r.conn, r.empresaId, dados(r)))));

		rotas.put("/api/funcionarios/(\\d+)", r -> {
			FuncionarioDAO.atualizar(r.conn, r.empresaId, r.grupo(0), dados(r));
			return Map.of("ok", true);
		});

		rotas.delete("/api/funcionarios/(\\d+)", r -> Map.of("resultado", FuncionarioDAO.excluir(r.conn, r.empresaId, r.grupo(0))));

		rotas.get("/api/cargos", r -> FuncionarioDAO.cargos(r.conn));
	}
}
