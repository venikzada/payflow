// Tela Início: resumo do mês, gráfico dos últimos meses e folhas do mês
import { api, montarPagina, icone, h, carregando, estadoErro, estadoVazio, definirSubtitulo } from "../app.js";
import { moeda, moedaPartes, mesExtenso, mesAno, nomeMes, iniciais } from "../formato.js";

const principal = montarPagina("inicio", {
  titulo: "Início",
  acoes: `<a class="btn btn-ink" href="/lancar.html">${icone("mais")}Lançar folha</a>`,
});

const STATUS = { CONFIRMADA: ["st-ok", "Confirmada"], PENDENTE: ["st-pend", "Pendente"] };

function valorMoeda(n) {
  const p = moedaPartes(n);
  return `<span class="cur">${p.simbolo}</span>${p.valor}`;
}

function cartoes(r) {
  const anterior = r.mes === 1 ? 12 : r.mes - 1;
  let delta = "";
  if (r.variacao_liquido !== null) {
    const sobe = r.variacao_liquido >= 0;
    delta = `<span class="delta">${icone(sobe ? "cima" : "baixo")}${Math.abs(r.variacao_liquido)}%</span><span class="hint">que ${nomeMes(anterior).toLowerCase()}</span>`;
  }
  return `
    <div class="panel"><div class="grid g-4" style="gap:14px">
      <div class="kpi is-accent" data-kpi="liquido">
        <div class="kpi-head">Líquido a pagar<span class="icon-btn">${icone("carteira")}</span></div>
        <div><div class="value num">${valorMoeda(r.liquido_total)}</div><div class="foot">${delta}</div></div>
      </div>
      <div class="kpi" data-kpi="funcionarios">
        <div class="kpi-head">Funcionários<span class="icon-btn">${icone("funcionarios")}</span></div>
        <div><div class="value num">${r.funcionarios}</div><div class="foot"><span class="delta st-exp" style="background:var(--warn-soft)">${r.em_experiencia}</span><span class="hint">em experiência</span></div></div>
      </div>
      <div class="kpi" data-kpi="pendentes">
        <div class="kpi-head">Folhas pendentes<span class="icon-btn">${icone("pendentes")}</span></div>
        <div><div class="value num">${r.pendentes}</div><div class="foot"><span class="hint">aguardando confirmação</span></div></div>
      </div>
      <div class="kpi" data-kpi="fgts">
        <div class="kpi-head">FGTS do mês<span class="icon-btn">${icone("banco")}</span></div>
        <div><div class="value num">${valorMoeda(r.fgts_total)}</div><div class="foot"><span class="hint">pago pela empresa</span></div></div>
      </div>
    </div></div>`;
}

function grafico(serie) {
  const maior = Math.max(...serie.map((s) => s.liquido + s.descontos), 0);
  const passo = Math.max(1000, Math.ceil(maior / 3 / 1000) * 1000);
  const topo = passo * 3;
  const altura = 180;
  const px = (v) => Math.round((v / topo) * altura);
  const marcas = [3, 2, 1, 0].map((i) => (i === 0 ? "0" : `${(passo * i) / 1000}k`));
  const barras = serie
    .map((s) => {
      const titulo = `${mesAno(s.mes, s.ano)} · Líquido ${moeda(s.liquido)} · Descontos ${moeda(s.descontos)}`;
      return `<div class="colw" data-barra title="${h(titulo)}">
        ${s.descontos > 0 ? `<div class="seg-o" style="height:${Math.max(px(s.descontos), 3)}px"></div>` : ""}
        ${s.liquido > 0 ? `<div class="seg-k" style="height:${px(s.liquido)}px"></div>` : ""}
      </div>`;
    })
    .join("");
  return `
    <div class="panel">
      <div class="panel-title">Folha dos últimos meses</div>
      <div class="panel-sub">Quanto foi pago e quanto foi descontado em cada mês</div>
      <div class="chart">
        <div class="chart-head">Líquido e descontos<div class="legend"><span><i style="background:#1C1C1C"></i>Líquido</span><span><i style="background:#F05A28"></i>Descontos</span></div></div>
        <div class="plot">
          <div class="yaxis">${marcas.map((m) => `<span>${m}</span>`).join("")}</div>
          <div class="cols">${barras}</div>
        </div>
        <div class="xaxis">${serie.map((s) => `<span>${nomeMes(s.mes).slice(0, 3)}</span>`).join("")}</div>
      </div>
    </div>`;
}

function tabela(r) {
  const titulo = `Folhas de ${nomeMes(r.mes).toLowerCase()}`;
  if (!r.folhas.length) {
    return `<div class="panel"><div class="panel-title">${titulo}</div>
      ${estadoVazio(`Nenhuma folha lançada em ${mesExtenso(r.mes, r.ano).toLowerCase()}.`, `<a class="btn btn-ink btn-sm" href="/lancar.html">${icone("mais")}Lançar folha</a>`)}</div>`;
  }
  const linhas = r.folhas
    .map((f) => {
      const [classe, rotulo] = STATUS[f.status] || ["st-ok", f.status];
      return `<tr class="clicavel" data-link="/historico.html?folha=${f.id}">
        <td><div class="who"><span class="av ${f.status === "PENDENTE" ? "o" : ""}">${h(iniciais(f.nome))}</span><div>${h(f.nome)}<small>${h(f.cargo)}</small></div></div></td>
        <td class="num">${moeda(f.salario_liquido)}</td>
        <td><span class="status ${classe}"><span>${rotulo}</span></span></td>
        <td class="dir"><a class="more" href="/historico.html?folha=${f.id}" aria-label="Ver holerite">···</a></td>
      </tr>`;
    })
    .join("");
  return `
    <div class="panel">
      <div class="panel-head"><div class="panel-title" style="margin:0">${titulo}</div><a class="btn btn-outline btn-sm" href="/historico.html">Ver histórico</a></div>
      <div class="tbl" id="folhas-mes"><table>
        <thead><tr><th>Funcionário</th><th>Líquido</th><th>Status</th><th></th></tr></thead>
        <tbody>${linhas}</tbody>
      </table></div>
    </div>`;
}

async function carregar() {
  carregando(principal);
  try {
    const r = await api("GET", "/api/resumo");
    definirSubtitulo(mesExtenso(r.mes, r.ano));
    principal.innerHTML = `${cartoes(r)}<div class="grid" style="grid-template-columns:minmax(0,1fr) minmax(0,1.6fr);margin-top:20px" id="linha-2">${grafico(r.serie)}${tabela(r)}</div>`;
    principal.querySelectorAll("tr[data-link]").forEach((tr) =>
      tr.addEventListener("click", (e) => {
        if (!e.target.closest("a")) location.href = tr.dataset.link;
      }),
    );
  } catch (erro) {
    estadoErro(principal, erro);
  }
}

carregar();
