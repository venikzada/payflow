// Formulário de funcionário (novo e editar), aberto num modal
import { api, abrirModal, h, aviso, limparErros, mostrarErroCampo } from "./app.js";
import { mascarar, numero, numeroBR } from "./formato.js";

let cargosCache = null;

async function cargos() {
  if (!cargosCache) {
    try {
      cargosCache = await api("GET", "/api/cargos");
    } catch {
      cargosCache = [];
    }
  }
  return cargosCache;
}

// Abre o formulário. Devolve o id salvo, ou null se a pessoa cancelar.
export async function abrirFormFuncionario({ funcionario = null } = {}) {
  const f = funcionario || {};
  const lista = await cargos();
  return new Promise((resolver) => {
    let salvo = null;
    const { el, fechar } = abrirModal(
      `<div class="modal-topo"><h2>${funcionario ? "Editar funcionário" : "Novo funcionário"}</h2></div>
      <form class="form" id="form-funcionario" novalidate>
        <div class="field"><label>Nome completo</label><input class="input" name="nome" value="${h(f.nome)}" placeholder="Ex.: Maria Oliveira"></div>
        <div class="grid g-2" style="gap:16px">
          <div class="field"><label>CPF</label><input class="input num" name="cpf" inputmode="numeric" value="${h(f.cpf ? mascarar("cpf", f.cpf) : "")}" placeholder="000.000.000-00"><div class="msg">Com ou sem pontos e traço</div></div>
          <div class="field"><label>E-mail</label><input class="input" name="email" type="email" value="${h(f.email)}" placeholder="nome@empresa.com.br"></div>
        </div>
        <div class="grid g-2" style="gap:16px">
          <div class="field"><label>Nascimento</label><input class="input" name="data_nascimento" type="date" value="${h(f.data_nascimento)}"></div>
          <div class="field"><label>Admissão</label><input class="input" name="data_admissao" type="date" value="${h(f.data_admissao)}"></div>
        </div>
        <div class="grid g-2" style="gap:16px">
          <div class="field"><label>Cargo</label><input class="input" name="cargo" list="lista-cargos" value="${h(f.cargo)}" placeholder="Escolha ou digite">
            <datalist id="lista-cargos">${lista.map((c) => `<option value="${h(c.nome)}">`).join("")}</datalist></div>
          <div class="field"><label>Salário base</label><div class="input-prefixo"><span>R$</span><input class="input num" name="salario_base" inputmode="decimal" value="${f.salario_base !== undefined ? numero(f.salario_base) : ""}" placeholder="0,00"></div></div>
        </div>
        <div class="acoes" style="display:flex;justify-content:flex-end;gap:10px;margin-top:4px">
          <button class="btn btn-soft" type="button" data-fechar>Cancelar</button>
          <button class="btn btn-ink" type="submit" id="salvar-funcionario">Salvar</button>
        </div>
      </form>`,
      { largo: true, aoFechar: () => resolver(salvo) },
    );

    const form = el.querySelector("#form-funcionario");
    const cpf = form.querySelector('[name="cpf"]');
    cpf.addEventListener("input", () => (cpf.value = mascarar("cpf", cpf.value)));
    form.querySelector('[name="nome"]').focus();

    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      limparErros(form);
      const dados = Object.fromEntries(new FormData(form).entries());
      const salario = numeroBR(dados.salario_base);
      dados.salario_base = Number.isNaN(salario) ? dados.salario_base : String(salario);
      const botao = form.querySelector("#salvar-funcionario");
      botao.disabled = true;
      try {
        if (funcionario) {
          await api("PUT", `/api/funcionarios/${funcionario.id}`, dados);
          salvo = funcionario.id;
        } else {
          salvo = (await api("POST", "/api/funcionarios", dados)).id;
        }
        aviso(funcionario ? "Funcionário atualizado." : "Funcionário cadastrado.");
        fechar();
      } catch (erro) {
        mostrarErroCampo(form, erro);
      } finally {
        botao.disabled = false;
      }
    });
  });
}
