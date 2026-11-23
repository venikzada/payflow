package Folha_Pagamentos;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

// Relógio que só anda quando o teste manda
final class RelogioTeste extends Clock {

	private Instant agora = Instant.parse("2026-11-14T15:00:00Z");

	void avancar(Duration d) {
		agora = agora.plus(d);
	}

	@Override
	public ZoneId getZone() {
		return ZoneId.of("America/Sao_Paulo");
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}

	@Override
	public Instant instant() {
		return agora;
	}
}
