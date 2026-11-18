// Formatação de valores para exibir na tela (só formata — nenhuma conta da folha é feita aqui)

const MESES = ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"];

const formatoNumero = (casas) =>
  new Intl.NumberFormat("pt-BR", { minimumFractionDigits: casas, maximumFractionDigits: casas });

export function numero(n, casas = 2) {
  return formatoNumero(casas).format(Number(n) || 0);
}

export function moeda(n) {
  const v = Number(n) || 0;
  return (v < 0 ? "-" : "") + "R$ " + numero(Math.abs(v));
}

export function moedaPartes(n) {
  const v = Number(n) || 0;
  return { simbolo: "R$", valor: (v < 0 ? "-" : "") + numero(Math.abs(v)) };
}

// "1,5" → 1.5 · "1.234,56" → 1234.56 · "3500.00" → 3500 · "" → 0 · inválido → NaN
export function numeroBR(texto) {
  let t = String(texto ?? "").replace(/R\$/g, "").replace(/\s/g, "");
  if (t === "") return 0;
  if (t.includes(",")) t = t.replace(/\./g, "").replace(",", ".");
  else if (/^-?\d{1,3}(\.\d{3})+$/.test(t)) t = t.replace(/\./g, ""); // "3.500" é três mil e quinhentos
  if (!/^-?\d*\.?\d+$/.test(t) && !/^-?\d+\.?$/.test(t)) return NaN;
  return Number(t);
}

export function soDigitos(t) {
  return String(t ?? "").replace(/\D/g, "");
}

export function data(iso) {
  if (!iso) return "";
  const [a, m, d] = String(iso).slice(0, 10).split("-");
  return `${d}/${m}/${a}`;
}

export function cpf(d) {
  return mascarar("cpf", d);
}

export function cnpj(d) {
  return mascarar("cnpj", d);
}

export function telefone(d) {
  return mascarar("telefone", d);
}

export function mesAno(m, a) {
  return `${MESES[m - 1].slice(0, 3)}/${a}`;
}

export function mesExtenso(m, a) {
  return `${MESES[m - 1]} de ${a}`;
}

export function nomeMes(m) {
  return MESES[m - 1];
}

export function iniciais(nome) {
  const partes = String(nome ?? "").trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return "";
  if (partes.length === 1) return partes[0][0].toUpperCase();
  return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
}

// Aplica a máscara aos poucos, enquanto a pessoa digita
export function mascarar(tipo, valor) {
  const d = soDigitos(valor);
  if (tipo === "cpf") {
    const x = d.slice(0, 11);
    return x
      .replace(/^(\d{3})(\d)/, "$1.$2")
      .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
      .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3-$4");
  }
  if (tipo === "cnpj") {
    const x = d.slice(0, 14);
    return x
      .replace(/^(\d{2})(\d)/, "$1.$2")
      .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
      .replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3/$4")
      .replace(/^(\d{2})\.(\d{3})\.(\d{3})\/(\d{4})(\d)/, "$1.$2.$3/$4-$5");
  }
  if (tipo === "telefone") {
    const x = d.slice(0, 11);
    if (x.length <= 2) return x.length ? `(${x}` : "";
    const ddd = x.slice(0, 2);
    const resto = x.slice(2);
    if (x.length <= 10) {
      return resto.length > 4 ? `(${ddd}) ${resto.slice(0, 4)}-${resto.slice(4)}` : `(${ddd}) ${resto}`;
    }
    return `(${ddd}) ${resto.slice(0, 5)}-${resto.slice(5)}`;
  }
  return String(valor ?? "");
}
