// Tela de acesso: entrar, cadastrar empresa e redefinir senha
import { api, aviso, limparErros, mostrarErroCampo } from "../app.js";
import { mascarar } from "../formato.js";

const TEXTOS = {
  entrar: ["Bem-vindo de volta", "Entre com o CNPJ e a senha da empresa."],
  cadastrar: ["Cadastre sua empresa", "Preencha os dados para começar a usar o sistema."],
  redefinir: ["Redefinir senha", "Confirme os dados da empresa para criar uma nova senha."],
};

function mudarModo(modo) {
  document.querySelectorAll("[data-modo]").forEach((b) => b.classList.toggle("on", b.dataset.modo === modo));
  document.querySelectorAll("[data-form]").forEach((f) => (f.hidden = f.dataset.form !== modo));
  document.getElementById("acesso-titulo").textContent = TEXTOS[modo][0];
  document.getElementById("acesso-sub").textContent = TEXTOS[modo][1];
  document.getElementById("erro-acesso").hidden = true;
}

function dados(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function enviando(botao, acao) {
  botao.disabled = true;
  try {
    await acao();
  } finally {
    botao.disabled = false;
  }
}

// Já está logado? Vai direto para o Início.
api("GET", "/api/acesso/eu")
  .then(() => (location.href = "/inicio.html"))
  .catch(() => {});

document.querySelectorAll("[data-modo]").forEach((b) => b.addEventListener("click", () => mudarModo(b.dataset.modo)));
document.querySelectorAll("[data-mascara]").forEach((i) =>
  i.addEventListener("input", () => (i.value = mascarar(i.dataset.mascara, i.value))),
);

const formEntrar = document.getElementById("form-entrar");
formEntrar.addEventListener("submit", (e) => {
  e.preventDefault();
  const caixa = document.getElementById("erro-acesso");
  caixa.hidden = true;
  limparErros(formEntrar);
  enviando(document.getElementById("entrar"), async () => {
    try {
      await api("POST", "/api/acesso/entrar", dados(formEntrar));
      location.href = "/inicio.html";
    } catch (erro) {
      if (erro.campo) {
        mostrarErroCampo(formEntrar, erro);
      } else {
        caixa.textContent = erro.mensagem;
        caixa.hidden = false;
      }
    }
  });
});

const formCadastrar = document.getElementById("form-cadastrar");
formCadastrar.addEventListener("submit", (e) => {
  e.preventDefault();
  limparErros(formCadastrar);
  enviando(formCadastrar.querySelector("button[type=submit]"), async () => {
    try {
      const d = dados(formCadastrar);
      await api("POST", "/api/acesso/cadastrar", d);
      formCadastrar.reset();
      mudarModo("entrar");
      document.getElementById("cnpj").value = mascarar("cnpj", d.cnpj);
      document.getElementById("senha").focus();
      aviso("Empresa cadastrada! Agora é só entrar.");
    } catch (erro) {
      mostrarErroCampo(formCadastrar, erro);
    }
  });
});

const formRedefinir = document.getElementById("form-redefinir");
formRedefinir.addEventListener("submit", (e) => {
  e.preventDefault();
  limparErros(formRedefinir);
  enviando(formRedefinir.querySelector("button[type=submit]"), async () => {
    try {
      const d = dados(formRedefinir);
      await api("POST", "/api/acesso/redefinir", d);
      formRedefinir.reset();
      mudarModo("entrar");
      document.getElementById("cnpj").value = mascarar("cnpj", d.cnpj);
      aviso("Senha redefinida!");
    } catch (erro) {
      mostrarErroCampo(formRedefinir, erro);
    }
  });
});
