package Folha_Pagamentos;

import java.util.Map;

// Rotas de acesso: entrar, cadastrar empresa, redefinir senha, sair e "quem sou eu"
final class ApiAcesso {

	private ApiAcesso() {
	}

	static final String COOKIE = "sessao=%s; Path=/; HttpOnly; SameSite=Lax";
	static final String COOKIE_SAIR = "sessao=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";

	private static String cnpj(Requisicao r) {
		String cnpj = r.texto("cnpj").replaceAll("\\D", "");
		if (!cnpj.matches("\\d{14}")) {
			throw ErroNegocio.invalido("cnpj", "CNPJ deve ter 14 números.");
		}
		return cnpj;
	}

	private static void conferirConfirmacao(Requisicao r) {
		if (!r.texto("senha").equals(r.texto("confirmacao"))) {
			throw ErroNegocio.invalido("confirmacao", "As senhas não conferem.");
		}
	}

	static void registrar(Roteador rotas, Sessoes sessoes) {
		rotas.publico("POST", "/api/acesso/entrar", r -> {
			String cnpj = cnpj(r);
			if (sessoes.bloqueado(cnpj)) {
				throw ErroNegocio.bloqueado("Muitas tentativas. Tente de novo em 5 minutos.");
			}
			Map<String, Object> empresa = EmpresaDAO.autenticar(r.conn, cnpj, r.texto("senha"));
			if (empresa == null) {
				int n = sessoes.registrarFalha(cnpj);
				throw new ErroNegocio(401, null,
						"CNPJ ou senha incorretos. Tentativa " + n + " de " + Sessoes.MAX_TENTATIVAS + ".");
			}
			sessoes.limparFalhas(cnpj);
			String token = sessoes.criar(((Number) empresa.get("id")).intValue());
			return Resposta.ok(empresa, String.format(COOKIE, token));
		});

		rotas.publico("POST", "/api/acesso/cadastrar", r -> {
			cnpj(r);
			EmpresaDAO.validarSenha(r.texto("senha"));
			conferirConfirmacao(r);
			int id = EmpresaDAO.cadastrar(r.conn, r.texto("cnpj"), r.texto("razao_social"), r.texto("nome_fantasia"),
					r.texto("email"), r.texto("telefone"), r.texto("senha"));
			return Resposta.criada(Map.of("id", id));
		});

		rotas.publico("POST", "/api/acesso/redefinir", r -> {
			cnpj(r);
			EmpresaDAO.validarSenha(r.texto("senha"));
			conferirConfirmacao(r);
			EmpresaDAO.redefinirSenha(r.conn, r.texto("cnpj"), r.texto("email"), r.texto("telefone"), r.texto("senha"));
			sessoes.limparFalhas(cnpj(r));
			return Map.of("ok", true);
		});

		rotas.publico("POST", "/api/acesso/sair", r -> {
			sessoes.encerrar(r.token);
			return Resposta.ok(Map.of("ok", true), COOKIE_SAIR);
		});

		rotas.get("/api/acesso/eu", r -> EmpresaDAO.buscar(r.conn, r.empresaId));
	}
}
