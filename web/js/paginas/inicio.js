// Tela Início: resumo do mês, gráfico dos últimos meses e folhas do mês
import { api, montarPagina, icone, h, carregando, estadoErro, estadoVazio, definirSubtitulo } from "../app.js";
import { moeda, moedaPartes, mesExtenso, mesAno, nomeMes, iniciais, numero } from "../formato.js";

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

// ---------- gráfico dos últimos meses: barras (padrão) ou linhas ----------
const CHAVE_GRAFICO = "payflow.grafico";
const COR_LIQUIDO = "#1C1C1C";
const COR_DESCONTOS = "#F05A28";

function escala(maior) {
  const passo = Math.max(1000, Math.ceil(maior / 3 / 1000) * 1000);
  return { passo, topo: passo * 3, marcas: [3, 2, 1, 0].map((i) => (i === 0 ? "0" : `${(passo * i) / 1000}k`)) };
}

function lerModo() {
  try {
    return localStorage.getItem(CHAVE_GRAFICO) === "linhas" ? "linhas" : "barras";
  } catch {
    return "barras";
  }
}

function guardarModo(modo) {
  try {
    localStorage.setItem(CHAVE_GRAFICO, modo);
  } catch {
    /* sem localStorage: só não lembra a escolha */
  }
}

function grafico() {
  return `
    <div class="panel">
      <div class="panel-head" style="margin-bottom:4px">
        <div><div class="panel-title">Folha dos últimos meses</div><div class="panel-sub" style="margin:0">Quanto foi pago e quanto foi descontado em cada mês</div></div>
        <div class="abas abas-sm" role="group" aria-label="Tipo de gráfico">
          <button type="button" data-grafico="barras" aria-pressed="false">Barras</button>
          <button type="button" data-grafico="linhas" aria-pressed="false">Linhas</button>
        </div>
      </div>
      <div class="chart" style="margin-top:14px">
        <div class="chart-head">Líquido e descontos<div class="legend"><span><i style="background:${COR_LIQUIDO}"></i>Líquido</span><span><i style="background:${COR_DESCONTOS}"></i>Descontos</span></div></div>
        <div id="grafico-area"></div>
      </div>
    </div>`;
}

function barras(serie) {
  const { topo, marcas } = escala(Math.max(...serie.map((s) => s.liquido + s.descontos), 0));
  const altura = 180;
  const px = (v) => Math.round((v / topo) * altura);
  const cols = serie
    .map((s) => {
      const titulo = `${mesAno(s.mes, s.ano)} · Líquido ${moeda(s.liquido)} · Descontos ${moeda(s.descontos)}`;
      return `<div class="colw" data-barra title="${h(titulo)}">
        ${s.descontos > 0 ? `<div class="seg-o" style="height:${Math.max(px(s.descontos), 3)}px"></div>` : ""}
        ${s.liquido > 0 ? `<div class="seg-k" style="height:${px(s.liquido)}px"></div>` : ""}
      </div>`;
    })
    .join("");
  return `
    <div class="plot">
      <div class="yaxis">${marcas.map((m) => `<span>${m}</span>`).join("")}</div>
      <div class="cols">${cols}</div>
    </div>
    <div class="xaxis">${serie.map((s) => `<span>${nomeMes(s.mes).slice(0, 3)}</span>`).join("")}</div>`;
}

function resumido(v) {
  return v >= 1000 ? `R$ ${numero(v / 1000, 1)}k` : moeda(v);
}

// Linhas em SVG desenhadas na largura real do quadro (texto sempre no tamanho certo)
function linhas(area, serie) {
  const largura = Math.max(260, area.clientWidth);
  const altura = 210;
  const m = { esq: 40, dir: 56, topo: 14, base: 28 };
  const w = largura - m.esq - m.dir;
  const hgt = altura - m.topo - m.base;
  const { topo, marcas } = escala(Math.max(...serie.map((s) => Math.max(s.liquido, s.descontos)), 0));
  const x = (i) => m.esq + (serie.length === 1 ? w / 2 : (i * w) / (serie.length - 1));
  const y = (v) => m.topo + hgt - (v / topo) * hgt;
  const caminho = (campo) => serie.map((s, i) => `${i ? "L" : "M"}${x(i).toFixed(1)} ${y(s[campo]).toFixed(1)}`).join(" ");
  const grade = marcas
    .map((rot, i) => {
      const yy = m.topo + (hgt * i) / 3;
      return `<line x1="${m.esq}" x2="${largura - m.dir}" y1="${yy}" y2="${yy}" stroke="#DCDCDC" stroke-width="1" ${i < 3 ? 'stroke-dasharray="3 4"' : ""}/>
        <text x="${m.esq - 8}" y="${yy + 4}" text-anchor="end" font-size="12" fill="#6B6B6B">${rot}</text>`;
    })
    .join("");
  const meses = serie
    .map((s, i) => `<text x="${x(i)}" y="${altura - 8}" text-anchor="middle" font-size="12" fill="#6B6B6B">${nomeMes(s.mes).slice(0, 3)}</text>`)
    .join("");
  const pontos = (campo, cor) =>
    serie.map((s, i) => `<circle data-ponto cx="${x(i)}" cy="${y(s[campo])}" r="4.5" fill="${cor}" stroke="#F5F5F5" stroke-width="2"/>`).join("");
  const ultimo = serie.length - 1;
  const rotuloFinal = (campo) =>
    `<text x="${x(ultimo) + 10}" y="${y(serie[ultimo][campo]) + 4}" font-size="12" font-weight="500" fill="#1A1A1A">${resumido(serie[ultimo][campo])}</text>`;
  const alvos = serie
    .map((_, i) => {
      const meio = (a, b) => (a + b) / 2;
      const x0 = i === 0 ? m.esq - 20 : meio(x(i - 1), x(i));
      const x1 = i === ultimo ? largura - m.dir + 20 : meio(x(i), x(i + 1));
      return `<rect data-alvo="${i}" x="${x0}" y="${m.topo}" width="${x1 - x0}" height="${hgt}" fill="transparent"/>`;
    })
    .join("");

  area.innerHTML = `
    <div style="position:relative">
      <svg id="grafico-linhas" width="${largura}" height="${altura}" viewBox="0 0 ${largura} ${altura}" role="img" aria-label="Gráfico de linhas: líquido e descontos dos últimos 6 meses" style="display:block;overflow:visible">
        ${grade}${meses}
        <line id="grafico-guia" x1="0" x2="0" y1="${m.topo}" y2="${m.topo + hgt}" stroke="#9A9A9A" stroke-width="1" visibility="hidden"/>
        <path data-linha d="${caminho("liquido")}" fill="none" stroke="${COR_LIQUIDO}" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>
        <path data-linha d="${caminho("descontos")}" fill="none" stroke="${COR_DESCONTOS}" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>
        ${pontos("liquido", COR_LIQUIDO)}${pontos("descontos", COR_DESCONTOS)}
        ${rotuloFinal("liquido")}${rotuloFinal("descontos")}
        ${alvos}
      </svg>
      <div id="grafico-dica" class="grafico-dica" hidden></div>
    </div>`;

  const svg = area.querySelector("#grafico-linhas");
  const guia = area.querySelector("#grafico-guia");
  const dica = area.querySelector("#grafico-dica");
  const esconder = () => {
    dica.hidden = true;
    guia.setAttribute("visibility", "hidden");
  };
  svg.querySelectorAll("[data-alvo]").forEach((alvo) => {
    alvo.addEventListener("mousemove", () => {
      const i = Number(alvo.dataset.alvo);
      const s = serie[i];
      guia.setAttribute("x1", x(i));
      guia.setAttribute("x2", x(i));
      guia.setAttribute("visibility", "visible");
      dica.innerHTML = `<b>${mesAno(s.mes, s.ano)}</b>
        <span><i style="background:${COR_LIQUIDO}"></i>Líquido <span class="num">${moeda(s.liquido)}</span></span>
        <span><i style="background:${COR_DESCONTOS}"></i>Descontos <span class="num">${moeda(s.descontos)}</span></span>`;
      dica.hidden = false;
      const esquerda = Math.min(Math.max(x(i) - dica.offsetWidth / 2, 0), largura - dica.offsetWidth);
      dica.style.left = `${esquerda}px`;
      dica.style.top = `${Math.max(0, Math.min(y(s.liquido), y(s.descontos)) - dica.offsetHeight - 14)}px`;
    });
  });
  svg.addEventListener("mouseleave", esconder);
}

function ligarGrafico(serie) {
  const area = principal.querySelector("#grafico-area");
  const botoes = principal.querySelectorAll("[data-grafico]");
  let modo = lerModo();
  const desenhar = () => {
    botoes.forEach((b) => {
      b.classList.toggle("on", b.dataset.grafico === modo);
      b.setAttribute("aria-pressed", String(b.dataset.grafico === modo));
    });
    if (modo === "linhas") linhas(area, serie);
    else area.innerHTML = barras(serie);
  };
  botoes.forEach((b) =>
    b.addEventListener("click", () => {
      modo = b.dataset.grafico;
      guardarModo(modo);
      desenhar();
    }),
  );
  // redesenha as linhas quando a largura do quadro muda
  let largura = area.clientWidth;
  new ResizeObserver(() => {
    if (modo === "linhas" && Math.abs(area.clientWidth - largura) > 4) {
      largura = area.clientWidth;
      linhas(area, serie);
    }
  }).observe(area);
  desenhar();
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
    principal.innerHTML = `${cartoes(r)}<div class="grid" style="grid-template-columns:minmax(0,1fr) minmax(0,1.6fr);margin-top:20px" id="linha-2">${grafico()}${tabela(r)}</div>`;
    ligarGrafico(r.serie);
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
