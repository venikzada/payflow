// Tela Folhas pendentes: confirmar ou apagar as folhas lançadas
import { api, montarPagina, h, carregando, estadoErro, estadoVazio, confirmar, aviso, icone, definirSubtitulo } from "../app.js";
import { moeda, mesAno, iniciais } from "../formato.js";
import { abrirHolerite } from "../holerite.js";

const principal = montarPagina("pendentes", {
  titulo: "Folhas pendentes",
  acoes: `<a class="btn btn-ink" href="/lancar.html">${icone("mais")}Lançar folha</a>`,
});

function atualizarBadge(n) {
  const link = document.querySelector('[data-menu="pendentes"]');
  link.querySelector(".badge")?.remove();
  if (n) link.insertAdjacentHTML("beforeend", `<span class="badge">${n}</span>`);
}

async function carregar() {
  if (!principal.children.length) carregando(principal);
  try {
    const lista = await api("GET", "/api/folhas", { status: "PENDENTE" });
    definirSubtitulo(lista.length ? `${lista.length} aguardando confirmação` : "Tudo confirmado");
    atualizarBadge(lista.length);
    if (!lista.length) {
      principal.innerHTML = `<div class="panel">${estadoVazio("Nenhuma folha pendente.", `<a class="btn btn-soft btn-sm" href="/lancar.html">Lançar folha</a>`)}</div>`;
      return;
    }
    principal.innerHTML = `<div class="panel">
      <div class="panel-sub">Confira o holerite antes de confirmar. Só folhas pendentes podem ser apagadas.</div>
      <div class="tbl" id="lista-pendentes"><table>
        <thead><tr><th>Mês</th><th>Funcionário</th><th>Líquido</th><th class="dir">Ações</th></tr></thead>
        <tbody>${lista
          .map(
            (f) => `<tr class="clicavel" data-id="${f.id}">
            <td>${mesAno(f.mes, f.ano)}</td>
            <td><div class="who"><span class="av o">${h(iniciais(f.nome))}</span><div>${h(f.nome)}<small>${h(f.cargo)}</small></div></div></td>
            <td class="num">${moeda(f.salario_liquido)}</td>
            <td class="dir"><div class="row" style="justify-content:flex-end">
              <button class="btn btn-accent btn-sm" type="button" data-confirmar="${f.id}">${icone("check")}Confirmar</button>
              <button class="btn btn-danger btn-sm" type="button" data-apagar="${f.id}" data-nome="${h(f.nome)}" data-mes="${mesAno(f.mes, f.ano)}">Apagar</button>
            </div></td>
          </tr>`,
          )
          .join("")}</tbody></table></div></div>`;

    principal.querySelectorAll("tr[data-id]").forEach((tr) =>
      tr.addEventListener("click", (e) => {
        if (!e.target.closest("button")) abrirHolerite(tr.dataset.id);
      }),
    );
    principal.querySelectorAll("[data-confirmar]").forEach((b) =>
      b.addEventListener("click", async () => {
        b.disabled = true;
        try {
          await api("POST", `/api/folhas/${b.dataset.confirmar}/confirmar`);
          aviso("Folha confirmada.");
          carregar();
        } catch (erro) {
          aviso(erro.mensagem, "erro");
          b.disabled = false;
        }
      }),
    );
    principal.querySelectorAll("[data-apagar]").forEach((b) =>
      b.addEventListener("click", async () => {
        const ok = await confirmar({
          titulo: "Apagar folha?",
          texto: `Apagar a folha de ${b.dataset.nome} (${b.dataset.mes})? Isso não pode ser desfeito.`,
          botao: "Apagar",
          perigo: true,
        });
        if (!ok) return;
        try {
          await api("DELETE", `/api/folhas/${b.dataset.apagar}`);
          aviso("Folha apagada.");
          carregar();
        } catch (erro) {
          aviso(erro.mensagem, "erro");
        }
      }),
    );
  } catch (erro) {
    estadoErro(principal, erro);
  }
}

carregar();
