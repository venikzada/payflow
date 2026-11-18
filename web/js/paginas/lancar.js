// Tela Lançar folha: escolhe o funcionário, informa extras e vê o holerite calculado na hora
import { api, montarPagina, icone, h, carregando, estadoErro, estadoVazio, aviso, esperar, definirSubtitulo } from "../app.js";
import { moeda, mesExtenso, mesAno, nomeMes } from "../formato.js";
import { renderizarHolerite } from "../holerite.js";

const principal = montarPagina("lancar", { titulo: "Lançar folha", subtitulo: "Calcula na hora e grava como pendente" });
const parametro = new URLSearchParams(location.search).get("funcionario");

let funcionarios = [];
let pedido = 0;
let ultima = null;

const CAMPOS = [
  ["he50", "Horas extras 50%", "horas"],
  ["he100", "Horas extras 100%", "horas"],
  ["faltas", "Dias de falta", "dias"],
  ["atrasos", "Horas de atraso", "horas"],
];

function montar() {
  principal.innerHTML = `
    <div class="grid g-2" style="align-items:start">
      <div class="panel">
        <div class="panel-title">Dados do mês</div>
        <div class="panel-sub">Deixe em branco o que não aconteceu no mês</div>
        <form class="form" id="form-lancar" novalidate>
          <div class="field"><label for="funcionario">Funcionário</label>
            <select class="input" id="funcionario" name="funcionario_id">
              <option value="">Escolha um funcionário</option>
              ${funcionarios.map((f) => `<option value="${f.id}">${h(f.nome)} — ${h(f.cargo)}</option>`).join("")}
            </select></div>
          <div class="bloco" id="cartao-funcionario" hidden></div>
          <div class="grid g-2" style="gap:16px">
            ${CAMPOS.map(([id, rotulo, unidade]) => `<div class="field"><label for="${id}">${rotulo}</label><input class="input num" id="${id}" name="${id}" inputmode="decimal" placeholder="0" autocomplete="off"><div class="msg">${unidade}</div></div>`).join("")}
          </div>
          <div class="hint" id="valor-hora"></div>
          <div id="ja-lancada" class="aviso-caixa" hidden></div>
          <div id="acoes-lancar"><button class="btn btn-ink btn-full" id="lancar" type="submit" disabled>${icone("check")}Lançar como pendente</button></div>
        </form>
      </div>
      <div class="panel" id="previa" style="position:sticky;top:16px">${estadoVazio("Escolha um funcionário para ver o holerite.")}</div>
    </div>`;

  const form = principal.querySelector("#form-lancar");
  const select = form.querySelector("#funcionario");
  const recalcular = esperar(simular, 200);
  select.addEventListener("change", () => {
    limparCampos();
    simular();
  });
  CAMPOS.forEach(([id]) => form.querySelector(`#${id}`).addEventListener("input", recalcular));
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    lancar();
  });
  if (parametro && funcionarios.some((f) => String(f.id) === parametro)) {
    select.value = parametro;
    simular();
  }
}

function dadosForm() {
  const form = principal.querySelector("#form-lancar");
  const d = { funcionario_id: form.querySelector("#funcionario").value };
  CAMPOS.forEach(([id]) => (d[id] = form.querySelector(`#${id}`).value.trim()));
  return d;
}

function limparErrosCampos() {
  CAMPOS.forEach(([id]) => {
    const i = principal.querySelector(`#${id}`);
    i.classList.remove("err");
    const msg = i.parentElement.querySelector(".msg");
    msg.classList.remove("err");
    msg.textContent = CAMPOS.find((c) => c[0] === id)[2];
  });
}

function limparCampos() {
  CAMPOS.forEach(([id]) => (principal.querySelector(`#${id}`).value = ""));
  limparErrosCampos();
}

function atualizarCartao() {
  const id = principal.querySelector("#funcionario").value;
  const f = funcionarios.find((x) => String(x.id) === id);
  const cartao = principal.querySelector("#cartao-funcionario");
  cartao.hidden = !f;
  if (f) cartao.innerHTML = `<div class="who"><div>${h(f.nome)}<small>${h(f.cargo)} · salário base <span class="num">${moeda(f.salario_base)}</span></small></div></div>`;
}

async function simular() {
  atualizarCartao();
  const d = dadosForm();
  const previa = principal.querySelector("#previa");
  const botao = principal.querySelector("#lancar");
  const jaLancada = principal.querySelector("#ja-lancada");
  if (!d.funcionario_id) {
    ultima = null;
    botao.disabled = true;
    jaLancada.hidden = true;
    principal.querySelector("#valor-hora").textContent = "";
    previa.innerHTML = estadoVazio("Escolha um funcionário para ver o holerite.");
    return;
  }
  const meu = ++pedido;
  try {
    const r = await api("POST", "/api/folhas/simular", d);
    if (meu !== pedido) return;
    limparErrosCampos();
    ultima = r;
    definirSubtitulo(`Competência: ${mesExtenso(r.mes, r.ano)}`);
    const f = funcionarios.find((x) => String(x.id) === d.funcionario_id);
    previa.innerHTML = renderizarHolerite(r, { titulo: `Holerite · ${mesAno(r.mes, r.ano)}`, subtitulo: `${f.nome} · ${f.cargo}`, status: "PREVIA" });
    principal.querySelector("#valor-hora").textContent = `Valor da hora: ${moeda(r.valor_hora)}`;
    jaLancada.hidden = !r.ja_lancada;
    if (r.ja_lancada) {
      jaLancada.innerHTML = `<span>Esse funcionário já tem folha em ${nomeMes(r.mes).toLowerCase()}.</span><a href="/historico.html?funcionario=${d.funcionario_id}">Ver no histórico</a>`;
    }
    botao.disabled = r.ja_lancada;
  } catch (erro) {
    if (meu !== pedido) return;
    botao.disabled = true;
    limparErrosCampos();
    const campo = erro.campo && principal.querySelector(`#${erro.campo}`);
    if (campo) {
      campo.classList.add("err");
      const msg = campo.parentElement.querySelector(".msg");
      msg.classList.add("err");
      msg.textContent = erro.mensagem;
    } else {
      aviso(erro.mensagem, "erro");
    }
  }
}

async function lancar() {
  const botao = principal.querySelector("#lancar");
  if (botao.disabled || !ultima) return;
  botao.disabled = true;
  try {
    const folha = await api("POST", "/api/folhas", dadosForm());
    aviso("Folha lançada como pendente.");
    principal.querySelector("#previa").innerHTML = renderizarHolerite(folha, {
      titulo: `Holerite · ${mesAno(folha.mes, folha.ano)}`,
      subtitulo: `${folha.funcionario.nome} · ${folha.funcionario.cargo}`,
      status: folha.status,
    });
    principal.querySelector("#acoes-lancar").innerHTML = `
      <div class="row" id="depois-de-lancar">
        <a class="btn btn-accent" href="/pendentes.html">${icone("pendentes")}Ver folhas pendentes</a>
        <button class="btn btn-soft" type="button" id="lancar-outra">Lançar outra</button>
      </div>`;
    principal.querySelector("#lancar-outra").addEventListener("click", () => (location.href = "/lancar.html"));
  } catch (erro) {
    aviso(erro.mensagem, "erro");
    botao.disabled = false;
  }
}

async function iniciar() {
  carregando(principal);
  try {
    funcionarios = await api("GET", "/api/funcionarios");
    if (!funcionarios.length) {
      principal.innerHTML = `<div class="panel">${estadoVazio("Nenhum funcionário cadastrado.", `<a class="btn btn-soft btn-sm" href="/funcionarios.html">Cadastrar funcionário</a>`)}</div>`;
      return;
    }
    montar();
  } catch (erro) {
    estadoErro(principal, erro);
  }
}

iniciar();
