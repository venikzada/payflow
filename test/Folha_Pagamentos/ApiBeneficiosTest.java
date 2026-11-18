package Folha_Pagamentos;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiBeneficiosTest {

	ServidorTeste srv;
	ServidorTeste.Cliente cli;

	@BeforeEach
	void subir() throws Exception {
		srv = new ServidorTeste();
		cli = srv.cliente();
		cli.entrarExemplo();
	}

	@AfterEach
	void descer() {
		srv.close();
	}

	@Test
	void catalogo() {
		assertTrue(cli.get("/api/beneficios").tem("Vale Combustível"));
	}

	@Test
	void limites() {
		var vt = cli.get("/api/beneficios/limite?funcionario_id=1&beneficio_id=1&valor=300&percentual=6");
		assertTrue(vt.tem("\"limite\":150"), vt.corpo());
		assertTrue(vt.tem("\"percentual_maximo\":6"));
		assertTrue(vt.tem("\"sobre\":\"salario\""));
		assertTrue(vt.tem("\"desconto\":150"));
		var vr = cli.get("/api/beneficios/limite?funcionario_id=1&beneficio_id=2&valor=500&percentual=0");
		assertTrue(vr.tem("\"limite\":100"));
		assertTrue(vr.tem("\"percentual_maximo\":20"));
		assertTrue(vr.tem("\"sobre\":\"valor\""));
	}

	@Test
	void duplicadoEAcimaDoLimite() {
		var d = cli.post("/api/funcionarios/1/beneficios", "beneficio_id", "1", "valor", "300", "percentual", "1");
		assertEquals(409, d.status());
		assertTrue(d.tem("já tem o benefício Vale Transporte"));
		var l = cli.post("/api/funcionarios/1/beneficios", "beneficio_id", "5", "valor", "50", "percentual", "11");
		assertEquals(400, l.status());
		assertTrue(l.tem("Percentual inválido. Digite entre 0 e 10,00."), l.corpo());
		var neg = cli.post("/api/funcionarios/1/beneficios", "beneficio_id", "5", "valor", "-5", "percentual", "0");
		assertEquals(400, neg.status());
		assertTrue(neg.tem("\"campo\":\"valor\""));
	}

	@Test
	void darEditarESimular() {
		var r = cli.post("/api/funcionarios/1/beneficios", "beneficio_id", "5", "valor", "50", "percentual", "1");
		assertEquals(201, r.status());
		var lista = cli.get("/api/funcionarios/1/beneficios");
		assertTrue(lista.tem("Plano Odontológico"));
		assertTrue(lista.tem("\"desconto_folha\":25"));
		assertTrue(cli.post("/api/folhas/simular", "funcionario_id", "1").tem("\"salario_liquido\":1974.31"));
		String id = r.corpo().replaceAll("\\D", "");
		assertEquals(200, cli.put("/api/funcionarios-beneficios/" + id, "valor", "50", "percentual", "2").status());
		assertTrue(cli.get("/api/funcionarios/1/beneficios").tem("\"desconto_folha\":50"));
	}

	@Test
	void removerDuasVezes() {
		for (int i = 0; i < 2; i++) {
			var r = cli.post("/api/funcionarios/1/beneficios", "beneficio_id", "5", "valor", "50", "percentual", "0");
			assertEquals(201, r.status(), r.corpo());
			var del = cli.delete("/api/funcionarios-beneficios/" + r.corpo().replaceAll("\\D", ""));
			assertEquals(200, del.status(), del.corpo());
		}
		assertFalse(cli.get("/api/funcionarios/1/beneficios").tem("Plano Odontológico"));
	}

	@Test
	void outraEmpresa404() {
		var b = srv.cliente();
		b.entrarComoBeta();
		assertEquals(404, b.get("/api/funcionarios/1/beneficios").status());
		assertEquals(404, b.post("/api/funcionarios/1/beneficios", "beneficio_id", "5", "valor", "50", "percentual", "0").status());
		assertEquals(404, b.put("/api/funcionarios-beneficios/1", "valor", "50", "percentual", "0").status());
		assertEquals(404, b.delete("/api/funcionarios-beneficios/1").status());
		assertEquals(404, b.get("/api/beneficios/limite?funcionario_id=1&beneficio_id=1&valor=300&percentual=0").status());
	}
}
