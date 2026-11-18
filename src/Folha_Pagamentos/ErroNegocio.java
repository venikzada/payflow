package Folha_Pagamentos;

// Erro de regra de negócio, com o status HTTP e (quando houver) o campo do formulário
public class ErroNegocio extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;
	private final String campo;

	public ErroNegocio(int status, String campo, String mensagem) {
		super(mensagem);
		this.status = status;
		this.campo = campo;
	}

	public int status() {
		return status;
	}

	public String campo() {
		return campo;
	}

	public static ErroNegocio invalido(String mensagem) {
		return new ErroNegocio(400, null, mensagem);
	}

	public static ErroNegocio invalido(String campo, String mensagem) {
		return new ErroNegocio(400, campo, mensagem);
	}

	public static ErroNegocio naoEncontrado(String mensagem) {
		return new ErroNegocio(404, null, mensagem);
	}

	public static ErroNegocio conflito(String mensagem) {
		return new ErroNegocio(409, null, mensagem);
	}

	public static ErroNegocio conflito(String campo, String mensagem) {
		return new ErroNegocio(409, campo, mensagem);
	}

	public static ErroNegocio bloqueado(String mensagem) {
		return new ErroNegocio(429, null, mensagem);
	}
}
