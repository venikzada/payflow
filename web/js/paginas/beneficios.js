// Tela Benefícios: benefícios do funcionário e formulário com o limite de desconto calculado pela API
import { api, montarPagina, h, carregando, estadoErro, estadoVazio, confirmar, aviso, icone, esperar, limparErros, mostrarErroCampo } from "../app.js";
import { moeda, numero, numeroBR } from "../formato.js";

const principal = montarPagina("beneficios", { titulo: "Benefícios", subtitulo: "O desconto de cada benefício respeita o limite da lei" });
const parametro = new URLSearchParams(location.search).get("funcionario");

let funcionarios = [];
let catalogo = [];
let editando = null; // vínculo sendo editado
let pedidoLimite = 0;

function pct(n) {
  return numero(n, Number.isInteger(Number(n)) ? 0 : 2);
}

function montar() {
  principal.innerHTML = `
    <div class="grid g-2" style="align-items:start">
      <div class="panel">
        <div class="field" style="margin-bottom:18px"><label for="funcionario">Funcionário</label>
          <select class="input" id="funcionario">${funcionarios.map((f) => `<option value="${f.id}">${h(f.nome)} — ${h(f.cargo)}</option>`).join("")}</select></div>
        <div class="panel-title" style="font-size:18px">Benefícios ativos</div>
        <div id="lista-beneficios"></div>
      </div>
      <div class="panel">
        <div class="panel-title" id="titulo-form">Dar benefício</div>
        <div class="panel-sub" id="sub-form">Escolha o benefício, o valor e quanto será descontado na folha</div>
        <form class="form" id="form-beneficio" novalidate>
          <div class="field"><label for="beneficio">Benefício</label>
            <select class="input" id="beneficio" name="beneficio_id"><option value="">Escolha um benefício</option>
              ${catalogo.map((b) => `<option value="${b.id}">${h(b.nome)}</option>`).join("")}</select></div>
          <div class="grid g-2" style="gap:16px">
            <div class="field"><label for="valor">Valor do benefício</label><div class="input-prefixo"><span>R$</span><input class="input num" id="valor" name="valor" inputmode="decimal" placeholder="0,00"></div></div>
            <div class="field"><label for="percentual">Desconto na folha</label><div class="input-prefixo"><span>%</span><input class="input num" id="percentual" name="percentual" inputmode="decimal" placeholder="0"></div></div>
          </div>
          <div class="bloco">
            <div class="panel-title" style="font-size:16px;margin:0">Limite de desconto</div>
            <div class="bar"><i id="limite-barra"></i></div>
            <div style="font-size:14px" id="limite-texto" class="num">Escolha um benefício para ver o limite.</div>
            <div class="hint" style="font-size:13px;margin-top:6px" id="limite-regra"></div>
          </div>
          <div class="row">
            <button class="btn btn-ink" type="submit" id="dar" style="flex:1">${icone("mais")}Dar benefício</button>
            <button class="btn btn-soft" type="button" id="cancelar-edicao" hidden>Cancelar</button>
          </div>
        </form>
      </div>
    </div>`;

  const sel = principal.querySelector("#funcionario");
  if (parametro && funcionarios.some((f) => String(f.id) === parametro)) sel.value = parametro;
  sel.addEventListener("change", () => {
    sairDaEdicao();
    carregarLista();
  });

  const form = principal.querySelector("#form-beneficio");
  const limite = esperar(atualizarLimite, 200);
  form.querySelector("#beneficio").addEventListener("change", () => {
    const b = catalogo.find((x) => String(x.id) === form.querySelector("#beneficio").value);
    if (b) form.querySelector("#valor").value = numero(b.valor_padrao);
    atualizarLimite();
  });
  form.querySelector("#valor").addEventListener("input", limite);
  form.querySelector("#percentual").addEventListener("input", limite);
  form.querySelector("#cancelar-edicao").addEventListener("click", sairDaEdicao);
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    salvar();
  });
  carregarLista();
}

function funcionarioId() {
  return principal.querySelector("#funcionario").value;
}

async function carregarLista() {
  const lista = principal.querySelector("#lista-beneficios");
  carregando(lista);
  try {
    const itens = await api("GET", `/api/funcionarios/${funcionarioId()}/beneficios`);
    if (!itens.length) {
      lista.innerHTML = estadoVazio("Nenhum benefício ativo.");
      return;
    }
    lista.innerHTML = itens
      .map(
        (b) => `<div class="beneficio slip-line" style="align-items:center;padding:14px 0">
          <div><div style="font-weight:500">${h(b.nome)}</div>
            <div class="hint" style="font-size:13px">Valor ${moeda(b.valor)} · ${
              b.desconto_folha > 0
                ? `desconto na folha <span class="num">${moeda(b.desconto_folha)}</span> (${pct(b.percentual)}% ${b.sobre === "valor" ? "do valor do benefício" : "do salário"})`
                : "sem desconto na folha"
            }</div></div>
          <div class="row" style="flex-wrap:nowrap">
            <button class="btn btn-soft btn-sm" type="button" data-editar="${b.id}">${icone("editar")}Editar</button>
            <button class="btn btn-danger btn-sm" type="button" data-remover="${b.id}">Remover</button>
          </div>
        </div>`,
      )
      .join("");
    lista.querySelectorAll("[data-editar]").forEach((btn) => btn.addEventListener("click", () => entrarNaEdicao(itens.find((x) => String(x.id) === btn.dataset.editar))));
    lista.querySelectorAll("[data-remover]").forEach((btn) =>
      btn.addEventListener("click", () => remover(itens.find((x) => String(x.id) === btn.dataset.remover))),
    );
  } catch (erro) {
    estadoErro(lista, erro);
  }
}

function entrarNaEdicao(b) {
  editando = b;
  const form = principal.querySelector("#form-beneficio");
  limparErros(form);
  form.querySelector("#beneficio").value = String(b.beneficio_id);
  form.querySelector("#beneficio").disabled = true;
  form.querySelector("#valor").value = numero(b.valor);
  form.querySelector("#percentual").value = pct(b.percentual);
  principal.querySelector("#titulo-form").textContent = `Editar ${b.nome}`;
  principal.querySelector("#sub-form").textContent = "Mude o valor ou o desconto";
  principal.querySelector("#dar").innerHTML = `${icone("check")}Salvar`;
  principal.querySelector("#cancelar-edicao").hidden = false;
  atualizarLimite();
}

function sairDaEdicao() {
  editando = null;
  const form = principal.querySelector("#form-beneficio");
  form.reset();
  limparErros(form);
  form.querySelector("#beneficio").disabled = false;
  form.querySelector("#percentual").disabled = false;
  principal.querySelector("#titulo-form").textContent = "Dar benefício";
  principal.querySelector("#sub-form").textContent = "Escolha o benefício, o valor e quanto será descontado na folha";
  principal.querySelector("#dar").innerHTML = `${icone("mais")}Dar benefício`;
  principal.querySelector("#cancelar-edicao").hidden = true;
  atualizarLimite();
}

function lerNumero(id) {
  const n = numeroBR(principal.querySelector(id).value);
  return Number.isNaN(n) ? null : n;
}

async function atualizarLimite() {
  const form = principal.querySelector("#form-beneficio");
  const beneficio = form.querySelector("#beneficio").value;
  const barra = principal.querySelector("#limite-barra");
  const textoEl = principal.querySelector("#limite-texto");
  const regra = principal.querySelector("#limite-regra");
  const percentual = form.querySelector("#percentual");
  const valor = lerNumero("#valor");
  const pctDigitado = lerNumero("#percentual");
  if (!beneficio || valor === null) {
    barra.style.width = "0";
    textoEl.textContent = beneficio ? "Digite um valor válido." : "Escolha um benefício para ver o limite.";
    regra.textContent = "";
    return;
  }
  const meu = ++pedidoLimite;
  try {
    const r = await api("GET", "/api/beneficios/limite", {
      funcionario_id: funcionarioId(),
      beneficio_id: beneficio,
      valor,
      percentual: pctDigitado ?? 0,
    });
    if (meu !== pedidoLimite) return;
    if (!r.permite_desconto) {
      percentual.value = "";
      percentual.disabled = true;
      barra.style.width = "0";
      textoEl.textContent = "Esse benefício não permite desconto na folha.";
      regra.textContent = "";
      return;
    }
    percentual.disabled = false;
    const proporcao = r.percentual_maximo > 0 ? Math.min(1, Math.max(0, (pctDigitado ?? 0) / r.percentual_maximo)) : 0;
    barra.style.width = `${proporcao * 100}%`;
    textoEl.innerHTML = `<b style="font-weight:500">${moeda(r.desconto)}</b> <span class="hint" style="font-size:13px">descontado de</span> ${moeda(r.limite)} permitidos`;
    regra.textContent = `Limite: até ${pct(r.percentual_maximo)}% do ${r.sobre === "valor" ? "valor do benefício" : "salário"}`;
  } catch (erro) {
    if (meu === pedidoLimite) textoEl.textContent = erro.mensagem;
  }
}

async function salvar() {
  const form = principal.querySelector("#form-beneficio");
  limparErros(form);
  const valor = lerNumero("#valor");
  const percentual = lerNumero("#percentual");
  const dados = {
    valor: valor === null ? form.querySelector("#valor").value : valor,
    percentual: percentual === null ? form.querySelector("#percentual").value : percentual ?? 0,
  };
  const botao = principal.querySelector("#dar");
  botao.disabled = true;
  try {
    if (editando) {
      await api("PUT", `/api/funcionarios-beneficios/${editando.id}`, dados);
      aviso(`${editando.nome} atualizado.`);
    } else {
      const beneficio = form.querySelector("#beneficio").value;
      if (!beneficio) {
        mostrarErroCampo(form, { campo: "beneficio_id", mensagem: "Escolha um benefício." });
        return;
      }
      await api("POST", `/api/funcionarios/${funcionarioId()}/beneficios`, { beneficio_id: beneficio, ...dados });
      aviso("Benefício adicionado.");
    }
    sairDaEdicao();
    carregarLista();
  } catch (erro) {
    mostrarErroCampo(form, erro);
  } finally {
    botao.disabled = false;
  }
}

async function remover(b) {
  const ok = await confirmar({ titulo: "Remover benefício?", texto: `Remover ${b.nome}? O desconto deixa de entrar nas próximas folhas.`, botao: "Remover", perigo: true });
  if (!ok) return;
  try {
    await api("DELETE", `/api/funcionarios-beneficios/${b.id}`);
    aviso(`${b.nome} removido.`);
    if (editando?.id === b.id) sairDaEdicao();
    carregarLista();
  } catch (erro) {
    aviso(erro.mensagem, "erro");
  }
}

async function iniciar() {
  carregando(principal);
  try {
    [funcionarios, catalogo] = await Promise.all([api("GET", "/api/funcionarios"), api("GET", "/api/beneficios")]);
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
