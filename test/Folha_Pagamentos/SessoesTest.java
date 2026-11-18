package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class SessoesTest {

	@Test
	void bloqueiaETiraBloqueio() {
		var r = new RelogioTeste();
		var s = new Sessoes(r);
		s.registrarFalha("1");
		s.registrarFalha("1");
		assertFalse(s.bloqueado("1"));
		assertEquals(3, s.registrarFalha("1"));
		assertTrue(s.bloqueado("1"));
		r.avancar(Duration.ofMinutes(5).plusSeconds(1));
		assertFalse(s.bloqueado("1"));
	}

	@Test
	void tokenGuardaEmpresa() {
		var s = new Sessoes(new RelogioTeste());
		String t = s.criar(7);
		assertEquals(7, s.empresaDo(t));
		s.encerrar(t);
		assertNull(s.empresaDo(t));
		assertNull(s.empresaDo(null));
	}
}
