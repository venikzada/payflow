// Tela Histórico: folhas por mês (com totais) ou por funcionário; abre e imprime o holerite
import { api, montarPagina, h, carregando, estadoErro, estadoVazio } from "../app.js";
import { moeda, moedaPartes, mesAno, nomeMes, iniciais } from "../formato.js";
import { abrirHolerite } from "../holerite.js";

const principal = montarPagina("historico", { titulo: "Histórico", subtitulo: "Todas as folhas lançadas" });
const params = new URLSearchParams(location.search);
const hoje = new Date();
const STATUS = { CONFIRMADA: ["st-ok", "Confirmada"], PENDENTE: ["st-pend", "Pendente"] };

let aba = params.get("funcionario") ? "funcionario" : "mes";
let funcionarios = [];

function status(s) {
  const [classe, rotulo] = STATUS[s] || ["st-ok", s];
  return `<span class="status ${classe}"><span>${rotulo}</span></span>`;
}

function ligarLinhas(el) {
  el.querySelectorAll("tr[data-folha]").forEach((tr) => tr.addEventListener("click", () => abrirHolerite(tr.dataset.folha)));
}

function totalCartao(chave, rotulo, valor, moedaSim = true) {
  const p = moedaPartes(valor);
  return `<div class="kpi" data-total="${chave}" style="min-height:0;gap:18px"><div class="kpi-head">${rotulo}</div>
    <div class="value num" style="font-size:28px">${moedaSim ? `<span class="cur">${p.simbolo}</span>${p.valor}` : valor}</div></div>`;
}

async function porMes(destino) {
  const mes = Number(destino.querySelector("#hist-mes").value);
  const ano = Number(destino.querySelector("#hist-ano").value);
  const corpo = destino.querySelector("#resultado");
  carregando(corpo);
  try {
    const r = await api("GET", "/api/folhas", { mes, ano });
    const t = r.totais;
    const tabela = r.folhas.length
      ? `<div class="tbl" id="lista-historico"><table><thead><tr><th>Funcionário</th><th>Proventos</th><th>Descontos</th><th>Líquido</th><th>Status</th></tr></thead><tbody>
        ${r.folhas
          .map(
            (f) => `<tr class="clicavel" data-folha="${f.id}">
            <td><div class="who"><span class="av">${h(iniciais(f.nome))}</span><div>${h(f.nome)}<small>${h(f.cargo)}</small></div></div></td>
            <td class="num">${moeda(f.total_proventos)}</td><td class="num">${moeda(f.total_descontos)}</td><td class="num">${moeda(f.salario_liquido)}</td><td>${status(f.status)}</td></tr>`,
          )
          .join("")}</tbody></table></div>`
      : estadoVazio(`Nenhuma folha lançada em ${nomeMes(mes).toLowerCase()} de ${ano}.`);
    corpo.innerHTML = `
      <div class="grid g-4" style="gap:14px;margin-bottom:20px">
        ${totalCartao("quantidade", "Folhas", t.quantidade, false)}
        ${totalCartao("proventos", "Proventos", t.proventos)}
        ${totalCartao("descontos", "Descontos", t.descontos)}
        ${totalCartao("liquido", "Líquido", t.liquido)}
      </div>${tabela}`;
    ligarLinhas(corpo);
  } catch (erro) {
    estadoErro(corpo, erro);
  }
}

async function porFuncionario(destino) {
  const id = destino.querySelector("#hist-funcionario").value;
  const corpo = destino.querySelector("#resultado");
  if (!id) {
    corpo.innerHTML = estadoVazio("Escolha um funcionário.");
    return;
  }
  carregando(corpo);
  try {
    const lista = await api("GET", "/api/folhas", { funcionario_id: id });
    corpo.innerHTML = lista.length
      ? `<div class="tbl" id="lista-historico"><table><thead><tr><th>Mês</th><th>Proventos</th><th>Descontos</th><th>Líquido</th><th>FGTS</th><th>Status</th></tr></thead><tbody>
        ${lista
          .map(
            (f) => `<tr class="clicavel" data-folha="${f.id}"><td>${mesAno(f.mes, f.ano)}</td><td class="num">${moeda(f.total_proventos)}</td>
            <td class="num">${moeda(f.total_descontos)}</td><td class="num">${moeda(f.salario_liquido)}</td><td class="num">${moeda(f.valor_fgts)}</td><td>${status(f.status)}</td></tr>`,
          )
          .join("")}</tbody></table></div>`
      : estadoVazio("Nenhuma folha lançada para esse funcionário.");
    ligarLinhas(corpo);
  } catch (erro) {
    estadoErro(corpo, erro);
  }
}

function montar() {
  const anos = [];
  for (let a = hoje.getFullYear(); a >= hoje.getFullYear() - 3; a--) anos.push(a);
  principal.innerHTML = `
    <div class="panel">
      <div class="toolbar">
        <div class="abas" style="margin:0;min-width:min(360px,100%)">
          <button type="button" data-aba="mes" class="${aba === "mes" ? "on" : ""}">Por mês</button>
          <button type="button" data-aba="funcionario" class="${aba === "funcionario" ? "on" : ""}">Por funcionário</button>
        </div>
        <div class="row" id="filtros"></div>
      </div>
      <div id="resultado"></div>
    </div>`;
  const filtros = principal.querySelector("#filtros");

  function desenharFiltros() {
    principal.querySelectorAll("[data-aba]").forEach((b) => b.classList.toggle("on", b.dataset.aba === aba));
    if (aba === "mes") {
      filtros.innerHTML = `
        <select class="input" id="hist-mes" aria-label="Mês" style="width:170px">${Array.from({ length: 12 }, (_, i) => `<option value="${i + 1}" ${i === hoje.getMonth() ? "selected" : ""}>${nomeMes(i + 1)}</option>`).join("")}</select>
        <select class="input" id="hist-ano" aria-label="Ano" style="width:120px">${anos.map((a) => `<option>${a}</option>`).join("")}</select>`;
      filtros.querySelectorAll("select").forEach((s) => s.addEventListener("change", () => porMes(principal)));
      porMes(principal);
    } else {
      filtros.innerHTML = `<select class="input" id="hist-funcionario" aria-label="Funcionário" style="width:260px">
        <option value="">Escolha um funcionário</option>
        ${funcionarios.map((f) => `<option value="${f.id}">${h(f.nome)}</option>`).join("")}</select>`;
      const sel = filtros.querySelector("#hist-funcionario");
      if (params.get("funcionario")) sel.value = params.get("funcionario");
      sel.addEventListener("change", () => porFuncionario(principal));
      porFuncionario(principal);
    }
  }

  principal.querySelectorAll("[data-aba]").forEach((b) =>
    b.addEventListener("click", () => {
      aba = b.dataset.aba;
      desenharFiltros();
    }),
  );
  desenharFiltros();
}

async function iniciar() {
  carregando(principal);
  try {
    funcionarios = await api("GET", "/api/funcionarios");
    const pedido = params.get("funcionario");
    if (pedido && !funcionarios.some((f) => String(f.id) === pedido)) {
      // funcionário desligado: continua aparecendo no histórico
      try {
        const f = await api("GET", `/api/funcionarios/${pedido}`);
        funcionarios.push({ id: f.id, nome: `${f.nome} (desligado)` });
      } catch {
        /* não existe ou é de outra empresa */
      }
    }
    montar();
    if (params.get("folha")) abrirHolerite(params.get("folha"));
  } catch (erro) {
    estadoErro(principal, erro);
  }
}

iniciar();
