// Base comum das telas: chamadas à API, menu, modal, avisos e ícones
import { cnpj as formatarCnpj } from "./formato.js";

// ---------- HTML seguro ----------
export function h(texto) {
  return String(texto ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}

// ---------- API ----------
export class ErroApi extends Error {
  constructor(status, mensagem, campo) {
    super(mensagem);
    this.status = status;
    this.mensagem = mensagem;
    this.campo = campo || null;
  }
}

export async function api(metodo, caminho, dados) {
  let url = caminho;
  const opcoes = { method: metodo, headers: {}, credentials: "same-origin" };
  if (dados) {
    const params = new URLSearchParams();
    for (const [k, v] of Object.entries(dados)) if (v !== undefined && v !== null) params.append(k, String(v));
    if (metodo === "GET" || metodo === "DELETE") {
      const q = params.toString();
      if (q) url += (url.includes("?") ? "&" : "?") + q;
    } else {
      opcoes.headers["Content-Type"] = "application/x-www-form-urlencoded; charset=utf-8";
      opcoes.body = params.toString();
    }
  }
  let resp;
  try {
    resp = await fetch(url, opcoes);
  } catch {
    throw new ErroApi(0, "Não foi possível falar com o servidor. Ele está rodando?");
  }
  let corpo = null;
  try {
    corpo = await resp.json();
  } catch {
    corpo = null;
  }
  if (resp.status === 401 && !location.pathname.endsWith("/index.html") && location.pathname !== "/") {
    location.href = "/index.html";
  }
  if (!resp.ok) throw new ErroApi(resp.status, corpo?.erro || "Algo deu errado. Tente de novo.", corpo?.campo);
  return corpo;
}

// ---------- ícones (traço simples) ----------
const ICONES = {
  inicio: '<path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/><path d="M10 21v-6h4v6"/>',
  funcionarios: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  lancar: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M12 12v6M9 15h6"/>',
  pendentes: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
  historico: '<path d="M3 3v5h5"/><path d="M3.05 13A9 9 0 1 0 6 5.3L3 8"/><path d="M12 7v5l4 2"/>',
  beneficios: '<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13"/><path d="M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7"/><path d="M7.5 8a2.5 2.5 0 0 1 0-5C11 3 12 8 12 8s1-5 4.5-5a2.5 2.5 0 0 1 0 5"/>',
  sair: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/>',
  busca: '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
  filtro: '<path d="M3 6h18M7 12h10M10 18h4"/>',
  mais: '<path d="M12 5v14M5 12h14"/>',
  editar: '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/>',
  lixo: '<path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M19 6l-1 14H6L5 6"/>',
  imprimir: '<path d="M6 9V2h12v7"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/>',
  carteira: '<path d="M19 7V5a2 2 0 0 0-2-2H5a2 2 0 0 0 0 4h14a2 2 0 0 1 2 2v3h-4a2 2 0 0 0 0 4h4v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5"/>',
  banco: '<path d="M3 21h18M5 21V10M19 21V10M9 21V10M15 21V10M2 10l10-7 10 7"/>',
  cima: '<path d="M12 19V5M5 12l7-7 7 7"/>',
  baixo: '<path d="M12 5v14M19 12l-7 7-7-7"/>',
  check: '<path d="M20 6 9 17l-5-5"/>',
  x: '<path d="M18 6 6 18M6 6l12 12"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  folha: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M8 13h8M8 17h5"/>',
  usuarioMais: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M19 8v6M22 11h-6"/>',
};

export function icone(nome, classe = "i") {
  return `<svg class="${classe}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONES[nome] || ""}</svg>`;
}

// ---------- casca da página ----------
const ITENS_MENU = [
  ["inicio", "Início", "/inicio.html"],
  ["funcionarios", "Funcionários", "/funcionarios.html"],
  ["lancar", "Lançar folha", "/lancar.html"],
  ["pendentes", "Folhas pendentes", "/pendentes.html"],
  ["historico", "Histórico", "/historico.html"],
  ["beneficios", "Benefícios", "/beneficios.html"],
];

function marca() {
  return `<a class="marca" href="/inicio.html"><b>${icone("folha")}</b>Folha de Pagamento</a>`;
}

export function montarPagina(ativa, { titulo, subtitulo = "", acoes = "" } = {}) {
  document.body.innerHTML = `
    <div class="app">
      <aside class="menu" aria-label="Menu">
        ${marca()}
        <nav class="nav">
          ${ITENS_MENU.map(([id, rotulo, link]) => `<a href="${link}" class="${id === ativa ? "on" : ""}" data-menu="${id}"${id === ativa ? ' aria-current="page"' : ""}>${icone(id)}<span>${rotulo}</span></a>`).join("")}
        </nav>
        <div class="espaco"></div>
        <div class="empresa-cartao">
          <div><b id="empresa-nome">&nbsp;</b><span id="empresa-cnpj">&nbsp;</span></div>
          <button class="btn btn-soft btn-sm" id="sair" type="button">${icone("sair")}Sair</button>
        </div>
      </aside>
      <div class="conteudo">
        <div class="barra-topo">${marca()}<button class="icon-btn act" id="abrir-menu" type="button" aria-label="Abrir menu">${icone("menu")}</button></div>
        <header class="page-head">
          <div><h1 id="titulo">${h(titulo)}</h1><p id="subtitulo">${h(subtitulo)}</p></div>
          <div class="acoes" id="acoes">${acoes}</div>
        </header>
        <main id="principal"></main>
      </div>
    </div>
    <div class="avisos" id="avisos" aria-live="polite"></div>`;
  document.title = `${titulo} · Folha de Pagamento`;

  const app = document.querySelector(".app");
  document.getElementById("abrir-menu").addEventListener("click", (e) => {
    e.stopPropagation();
    app.classList.toggle("menu-aberto");
  });
  document.querySelector(".conteudo").addEventListener("click", () => app.classList.remove("menu-aberto"));
  document.getElementById("sair").addEventListener("click", async () => {
    try {
      await api("POST", "/api/acesso/sair");
    } finally {
      location.href = "/index.html";
    }
  });

  api("GET", "/api/acesso/eu")
    .then((e) => {
      document.getElementById("empresa-nome").textContent = e.nome_fantasia;
      document.getElementById("empresa-cnpj").textContent = formatarCnpj(e.cnpj);
    })
    .catch(() => {});
  api("GET", "/api/folhas", { status: "PENDENTE" })
    .then((lista) => {
      if (lista.length) {
        const link = document.querySelector('[data-menu="pendentes"]');
        link.insertAdjacentHTML("beforeend", `<span class="badge">${lista.length}</span>`);
      }
    })
    .catch(() => {});

  return document.getElementById("principal");
}

export function definirSubtitulo(texto) {
  document.getElementById("subtitulo").textContent = texto;
}

// ---------- estados ----------
export function estadoVazio(texto, acaoHtml = "") {
  return `<div class="vazio"><span>${h(texto)}</span>${acaoHtml}</div>`;
}

export function carregando(el) {
  el.innerHTML = `<div class="carregando">Carregando…</div>`;
}

export function estadoErro(el, erro) {
  el.innerHTML = `<div class="erro-caixa">${h(erro?.mensagem || erro?.message || "Algo deu errado.")}</div>`;
}

// ---------- avisos ----------
export function aviso(texto, tipo = "ok") {
  let caixa = document.getElementById("avisos");
  if (!caixa) {
    caixa = document.createElement("div");
    caixa.id = "avisos";
    caixa.className = "avisos";
    document.body.appendChild(caixa);
  }
  const el = document.createElement("div");
  el.className = `aviso ${tipo === "erro" ? "erro" : ""}`;
  el.textContent = texto;
  caixa.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

// ---------- erros nos campos ----------
export function limparErros(form) {
  form.querySelectorAll(".input.err").forEach((i) => i.classList.remove("err"));
  form.querySelectorAll(".msg.err[data-erro]").forEach((m) => m.remove());
}

export function mostrarErroCampo(form, erro) {
  limparErros(form);
  const campo = erro?.campo ? form.querySelector(`[name="${erro.campo}"]`) : null;
  if (!campo) {
    aviso(erro?.mensagem || "Algo deu errado.", "erro");
    return;
  }
  campo.classList.add("err");
  const msg = document.createElement("div");
  msg.className = "msg err";
  msg.dataset.erro = "1";
  msg.textContent = erro.mensagem;
  (campo.closest(".field") || campo.parentElement).appendChild(msg);
  campo.focus();
}

// ---------- modal ----------
export function abrirModal(html, { largo = false, aoFechar } = {}) {
  const fundo = document.createElement("div");
  fundo.className = "modal-fundo";
  fundo.innerHTML = `<div class="modal ${largo ? "largo" : ""}" role="dialog" aria-modal="true">${html}</div>`;
  document.body.appendChild(fundo);
  const fechar = () => {
    fundo.remove();
    document.removeEventListener("keydown", tecla);
    aoFechar?.();
  };
  const tecla = (e) => {
    if (e.key === "Escape") fechar();
  };
  document.addEventListener("keydown", tecla);
  fundo.addEventListener("click", (e) => {
    if (e.target === fundo) fechar();
  });
  fundo.querySelectorAll("[data-fechar]").forEach((b) => b.addEventListener("click", fechar));
  return { el: fundo.querySelector(".modal"), fechar };
}

export function confirmar({ titulo, texto, botao = "Confirmar", perigo = false }) {
  return new Promise((resolver) => {
    let resposta = false;
    const { el, fechar } = abrirModal(
      `<h2>${h(titulo)}</h2><p>${h(texto)}</p>
       <div class="acoes">
         <button class="btn btn-soft" type="button" data-fechar>Cancelar</button>
         <button class="btn ${perigo ? "btn-danger" : "btn-ink"}" type="button" id="confirmar-sim">${h(botao)}</button>
       </div>`,
      { aoFechar: () => resolver(resposta) },
    );
    el.querySelector("#confirmar-sim").addEventListener("click", () => {
      resposta = true;
      fechar();
    });
    el.querySelector("#confirmar-sim").focus();
  });
}

// Espera o usuário parar de digitar antes de chamar a função
export function esperar(fn, ms) {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), ms);
  };
}
