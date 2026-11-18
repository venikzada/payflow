package Folha_Pagamentos;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Guarda quem está logado (token → empresa) e conta as tentativas erradas de login
final class Sessoes {

	static final int MAX_TENTATIVAS = 3;
	static final Duration BLOQUEIO = Duration.ofMinutes(5);

	private record Falhas(int quantidade, Instant ultima) {
	}

	private final Clock relogio;
	private final SecureRandom aleatorio = new SecureRandom();
	private final Map<String, Integer> tokens = new ConcurrentHashMap<>();
	private final Map<String, Falhas> falhas = new ConcurrentHashMap<>();

	Sessoes(Clock relogio) {
		this.relogio = relogio;
	}

	String criar(int empresaId) {
		byte[] bytes = new byte[32];
		aleatorio.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		tokens.put(token, empresaId);
		return token;
	}

	Integer empresaDo(String token) {
		return token == null ? null : tokens.get(token);
	}

	void encerrar(String token) {
		if (token != null) {
			tokens.remove(token);
		}
	}

	private boolean expirou(Falhas f) {
		return !relogio.instant().isBefore(f.ultima().plus(BLOQUEIO));
	}

	boolean bloqueado(String cnpj) {
		Falhas f = falhas.get(cnpj);
		if (f == null || f.quantidade() < MAX_TENTATIVAS) {
			return false;
		}
		if (expirou(f)) {
			falhas.remove(cnpj);
			return false;
		}
		return true;
	}

	int registrarFalha(String cnpj) {
		Falhas atual = falhas.get(cnpj);
		int quantidade = (atual == null || expirou(atual)) ? 1 : atual.quantidade() + 1;
		falhas.put(cnpj, new Falhas(quantidade, relogio.instant()));
		return quantidade;
	}

	void limparFalhas(String cnpj) {
		falhas.remove(cnpj);
	}
}
