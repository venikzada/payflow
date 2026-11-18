// Tela Funcionários: lista com busca e filtro de experiência
import { api, montarPagina, icone, h, carregando, estadoErro, estadoVazio, definirSubtitulo, esperar } from "../app.js";
import { moeda, data, iniciais } from "../formato.js";
import { abrirFormFuncionario } from "../form-funcionario.js";

const principal = montarPagina("funcionarios", {
  titulo: "Funcionários",
  acoes: `<button class="btn btn-soft" id="novo-funcionario" type="button">${icone("usuarioMais")}Novo funcionário</button>`,
});

principal.innerHTML = `
  <div class="panel">
    <div class="toolbar">
      <div class="input-busca" style="width:min(320px,100%)">${icone("busca")}<input class="input" id="busca" type="search" placeholder="Buscar pelo nome" aria-label="Buscar funcionário"></div>
      <div class="segmento" role="group" aria-label="Filtrar">
        <button class="btn btn-outline btn-sm on" data-filtro="todos" type="button">Todos</button>
        <button class="btn btn-outline btn-sm" data-filtro="experiencia" type="button">Em experiência</button>
        <button class="btn btn-outline btn-sm" data-filtro="efetivados" type="button">Efetivados</button>
      </div>
    </div>
    <div id="lista"></div>
  </div>`;

const lista = principal.querySelector("#lista");
let filtro = "todos";
let pedido = 0;

function situacao(f) {
  if (f.em_experiencia) {
    const dias = f.dias_restantes === 1 ? "1 dia" : `${f.dias_restantes} dias`;
    return `<span class="status st-exp"><span>Em experiência · faltam ${dias}</span></span>`;
  }
  return `<span class="status st-ok"><span>Efetivado</span></span>`;
}

async function carregar() {
  const meu = ++pedido;
  if (!lista.children.length) carregando(lista);
  try {
    const itens = await api("GET", "/api/funcionarios", { filtro, busca: document.getElementById("busca").value });
    if (meu !== pedido) return;
    if (filtro === "todos" && !document.getElementById("busca").value) {
      definirSubtitulo(itens.length === 1 ? "1 ativo" : `${itens.length} ativos`);
    }
    if (!itens.length) {
      lista.innerHTML = estadoVazio("Nenhum funcionário encontrado.");
      return;
    }
    lista.innerHTML = `<div class="tbl" id="lista-funcionarios"><table>
      <thead><tr><th>Funcionário</th><th>Admissão</th><th>Situação</th><th>Salário base</th><th></th></tr></thead>
      <tbody>${itens
        .map(
          (f) => `<tr class="clicavel" data-id="${f.id}">
          <td><div class="who"><span class="av ${f.em_experiencia ? "o" : ""}">${h(iniciais(f.nome))}</span><div>${h(f.nome)}<small>${h(f.cargo)}</small></div></div></td>
          <td>${data(f.data_admissao)}<small class="hint" style="display:block;font-size:13px">${h(f.tempo_empresa)}</small></td>
          <td>${situacao(f)}</td>
          <td class="num">${moeda(f.salario_base)}</td>
          <td class="dir"><a class="more" href="/funcionario.html?id=${f.id}" aria-label="Ver ficha">···</a></td>
        </tr>`,
        )
        .join("")}</tbody></table></div>`;
    lista.querySelectorAll("tr[data-id]").forEach((tr) =>
      tr.addEventListener("click", (e) => {
        if (!e.target.closest("a")) location.href = `/funcionario.html?id=${tr.dataset.id}`;
      }),
    );
  } catch (erro) {
    if (meu === pedido) estadoErro(lista, erro);
  }
}

principal.querySelectorAll("[data-filtro]").forEach((b) =>
  b.addEventListener("click", () => {
    filtro = b.dataset.filtro;
    principal.querySelectorAll("[data-filtro]").forEach((x) => x.classList.toggle("on", x === b));
    carregar();
  }),
);
document.getElementById("busca").addEventListener("input", esperar(carregar, 250));
document.getElementById("novo-funcionario").addEventListener("click", async () => {
  if (await abrirFormFuncionario()) carregar();
});

carregar();
