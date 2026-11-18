// Ficha do funcionário: dados, benefícios, histórico e ações (lançar, editar, excluir)
import { api, montarPagina, icone, h, carregando, estadoErro, estadoVazio, confirmar, aviso } from "../app.js";
import { moeda, data, cpf, mesAno } from "../formato.js";
import { abrirFormFuncionario } from "../form-funcionario.js";

const id = new URLSearchParams(location.search).get("id");
const principal = montarPagina("funcionarios", { titulo: "Funcionário" });

const STATUS = { CONFIRMADA: ["st-ok", "Confirmada"], PENDENTE: ["st-pend", "Pendente"] };

function item(rotulo, valor) {
  return `<div class="slip-line"><span class="muted">${rotulo}</span><span>${valor}</span></div>`;
}

async function carregar() {
  carregando(principal);
  try {
    const [f, beneficios, folhas] = await Promise.all([
      api("GET", `/api/funcionarios/${id}`),
      api("GET", `/api/funcionarios/${id}/beneficios`),
      api("GET", "/api/folhas", { funcionario_id: id }),
    ]);
    document.getElementById("titulo").textContent = f.nome;
    document.getElementById("subtitulo").textContent = f.cargo;
    document.title = `${f.nome} · Folha de Pagamento`;

    const acoes = document.getElementById("acoes");
    if (f.ativo) {
      acoes.innerHTML = `
        <a class="btn btn-ink" href="/lancar.html?funcionario=${f.id}">${icone("mais")}Lançar folha</a>
        <button class="btn btn-soft" id="editar" type="button">${icone("editar")}Editar</button>
        <button class="btn btn-danger" id="excluir" type="button">${icone("lixo")}Excluir</button>`;
      acoes.querySelector("#editar").addEventListener("click", async () => {
        if (await abrirFormFuncionario({ funcionario: f })) carregar();
      });
      acoes.querySelector("#excluir").addEventListener("click", () => excluir(f));
    } else {
      acoes.innerHTML = `<span class="tag">Desligado</span>`;
    }

    const situacao = f.em_experiencia
      ? `<span class="status st-exp"><span>Em experiência até ${data(f.fim_experiencia)}</span></span>`
      : `<span class="status st-ok"><span>Efetivado</span></span>`;

    const listaBeneficios = beneficios.length
      ? beneficios
          .map(
            (b) => `<div class="slip-line"><span>${h(b.nome)}<small>${moeda(b.valor)}</small></span>
              <span class="num ${b.desconto_folha > 0 ? "neg" : "muted"}">${b.desconto_folha > 0 ? "− " + moeda(b.desconto_folha) : "sem desconto"}</span></div>`,
          )
          .join("")
      : estadoVazio("Nenhum benefício ativo.");

    const listaFolhas = folhas.length
      ? `<div class="tbl"><table><thead><tr><th>Mês</th><th>Líquido</th><th>Status</th><th></th></tr></thead><tbody>
        ${folhas
          .map((x) => {
            const [classe, rotulo] = STATUS[x.status] || ["st-ok", x.status];
            return `<tr class="clicavel" data-link="/historico.html?folha=${x.id}"><td>${mesAno(x.mes, x.ano)}</td><td class="num">${moeda(x.salario_liquido)}</td>
              <td><span class="status ${classe}"><span>${rotulo}</span></span></td><td class="dir"><a class="more" href="/historico.html?folha=${x.id}" aria-label="Ver holerite">···</a></td></tr>`;
          })
          .join("")}</tbody></table></div>`
      : estadoVazio("Nenhuma folha lançada ainda.");

    principal.innerHTML = `
      <div class="grid g-side">
        <div class="stack">
          <div class="panel" id="ficha-dados">
            <div class="panel-title" style="margin-bottom:8px">Dados</div>
            ${item("CPF", `<span class="num">${cpf(f.cpf)}</span>`)}
            ${item("E-mail", h(f.email || "Não informado"))}
            ${item("Nascimento", data(f.data_nascimento))}
            ${item("Admissão", data(f.data_admissao))}
            ${item("Tempo de empresa", h(f.tempo_empresa))}
            ${item("Situação", situacao)}
            <div class="slip-total"><div class="hint">Salário base</div><div class="value num"><span class="cur">R$</span>${moeda(f.salario_base).replace("R$ ", "")}</div></div>
          </div>
          <div class="panel">
            <div class="panel-head"><div class="panel-title" style="margin:0">Benefícios ativos</div><a class="btn btn-outline btn-sm" href="/beneficios.html?funcionario=${f.id}">Gerenciar</a></div>
            ${listaBeneficios}
          </div>
        </div>
        <div class="panel" id="ficha-historico">
          <div class="panel-title">Histórico de folhas</div>
          <div class="panel-sub">Todas as folhas lançadas para ${h(f.nome.split(" ")[0])}</div>
          ${listaFolhas}
        </div>
      </div>`;
    principal.querySelectorAll("tr[data-link]").forEach((tr) =>
      tr.addEventListener("click", (e) => {
        if (!e.target.closest("a")) location.href = tr.dataset.link;
      }),
    );
  } catch (erro) {
    estadoErro(principal, erro);
  }
}

async function excluir(f) {
  const ok = await confirmar(
    f.tem_folhas
      ? {
          titulo: "Desligar funcionário?",
          texto: `${f.nome} já tem folhas no histórico, então vai ser desligado: some das listas e do lançamento, mas o histórico continua.`,
          botao: "Desligar",
          perigo: true,
        }
      : { titulo: "Excluir funcionário?", texto: `${f.nome} será apagado de vez, junto com os benefícios dele.`, botao: "Excluir", perigo: true },
  );
  if (!ok) return;
  try {
    const r = await api("DELETE", `/api/funcionarios/${f.id}`);
    sessionStorage.setItem("aviso", r.resultado === "DESLIGADO" ? `${f.nome} foi desligado.` : `${f.nome} foi excluído.`);
    location.href = "/funcionarios.html";
  } catch (erro) {
    aviso(erro.mensagem, "erro");
  }
}

if (!id) {
  estadoErro(principal, { mensagem: "Funcionário não informado." });
} else {
  carregar();
}
