// Desenha o holerite (prévia da simulação ou folha já gravada)
import { h } from "./app.js";
import { moeda, moedaPartes, numero } from "./formato.js";

function ehDesconto(l) {
  return l.tipo ? l.tipo === "DESCONTO" : Boolean(l.desconto);
}

function referencia(l) {
  if (l.referencia === null || l.referencia === undefined) return "";
  const d = String(l.descricao);
  const n = numero(l.referencia, Number.isInteger(Number(l.referencia)) ? 0 : 2);
  if (/^Hora Extra|^Atraso/.test(d)) return `${n} h`;
  if (/^Falta/.test(d)) return Number(l.referencia) === 1 ? "1 dia" : `${n} dias`;
  return "";
}

function linha(l) {
  const ref = referencia(l);
  const desc = ehDesconto(l);
  const valor = desc ? (l.valor > 0 ? `<span class="num neg">− ${moeda(l.valor)}</span>` : `<span class="num muted">${moeda(0)}</span>`) : `<span class="num">${moeda(l.valor)}</span>`;
  return `<div class="slip-line"><span>${h(l.descricao)}${ref ? `<small>${ref}</small>` : ""}</span>${valor}</div>`;
}

const STATUS = { PENDENTE: ["st-pend", "Pendente"], CONFIRMADA: ["st-ok", "Confirmada"] };

// dados: paraMapa() da simulação (linhas com "desconto") ou holerite gravado (linhas com "tipo")
export function renderizarHolerite(dados, { titulo = "Holerite", subtitulo = "", status = null, imprimir = false } = {}) {
  const linhas = dados.linhas || [];
  const proventos = linhas.filter((l) => !ehDesconto(l));
  const descontos = linhas.filter(ehDesconto);
  let etiqueta = "";
  if (status === "PREVIA") etiqueta = `<span class="tag">Prévia</span>`;
  else if (status && STATUS[status]) etiqueta = `<span class="status ${STATUS[status][0]}"><span>${STATUS[status][1]}</span></span>`;
  const liquido = moedaPartes(dados.salario_liquido);
  return `
    <div class="holerite ${imprimir ? "imprimir" : ""}">
      <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px;margin-bottom:4px">
        <div><div class="panel-title">${h(titulo)}</div>${subtitulo ? `<div class="hint">${h(subtitulo)}</div>` : ""}</div>
        ${etiqueta}
      </div>
      <div class="slip-sec">Proventos</div>
      ${proventos.map(linha).join("")}
      <div class="slip-line" style="border-bottom:0"><span class="muted">Total de proventos</span><span class="num">${moeda(dados.total_proventos)}</span></div>
      <div class="slip-sec">Descontos</div>
      ${descontos.map(linha).join("")}
      <div class="slip-line" style="border-bottom:0"><span class="muted">Total de descontos</span><span class="num neg">− ${moeda(dados.total_descontos)}</span></div>
      <div class="slip-total">
        <div class="hint">Salário líquido</div>
        <div class="value num"><span class="cur">${liquido.simbolo}</span>${liquido.valor}</div>
        <div class="hint" style="font-size:13px;margin-top:4px">FGTS pago pela empresa: ${moeda(dados.valor_fgts)}</div>
      </div>
    </div>`;
}
