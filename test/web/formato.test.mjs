import { test } from "node:test";
import assert from "node:assert/strict";
import * as f from "../../web/js/formato.js";

test("moeda", () => {
  assert.equal(f.moeda(1999.31), "R$ 1.999,31");
  assert.equal(f.moeda(-10), "-R$ 10,00");
  assert.equal(f.moeda(0), "R$ 0,00");
  assert.deepEqual(f.moedaPartes(11896.48), { simbolo: "R$", valor: "11.896,48" });
  assert.equal(f.numero(1234.5), "1.234,50");
  assert.equal(f.numero(6, 0), "6");
});

test("numeroBR", () => {
  assert.equal(f.numeroBR(""), 0);
  assert.equal(f.numeroBR("1,5"), 1.5);
  assert.equal(f.numeroBR("1.234,56"), 1234.56);
  assert.equal(f.numeroBR("3500.00"), 3500);
  assert.equal(f.numeroBR("R$ 2.300,00"), 2300);
  assert.ok(Number.isNaN(f.numeroBR("abc")));
});

test("documentos e datas", () => {
  assert.equal(f.cpf("52998224725"), "529.982.247-25");
  assert.equal(f.cnpj("12345678000199"), "12.345.678/0001-99");
  assert.equal(f.telefone("3133334444"), "(31) 3333-4444");
  assert.equal(f.telefone("31999998888"), "(31) 99999-8888");
  assert.equal(f.data("2026-01-10"), "10/01/2026");
  assert.equal(f.mesAno(11, 2026), "Nov/2026");
  assert.equal(f.mesExtenso(11, 2026), "Novembro de 2026");
  assert.equal(f.iniciais("João da Silva"), "JS");
  assert.equal(f.iniciais("Ana"), "A");
  assert.equal(f.soDigitos("529.982.247-25"), "52998224725");
  assert.equal(f.mascarar("cpf", "5299822"), "529.982.2");
  assert.equal(f.mascarar("cnpj", "123456780001"), "12.345.678/0001");
  assert.equal(f.mascarar("telefone", "3199999"), "(31) 9999-9");
});
