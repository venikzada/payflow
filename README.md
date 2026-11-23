# Payflow · Sistema de Folha de Pagamento

Sistema de folha de pagamento em Java, com banco de dados SQLite, menu no console e versão web (HTML, CSS e JavaScript), desenvolvido para o projeto final da disciplina de Garantia da Qualidade de Software (Una Contagem).

**Documento de apresentação completo (sistema, linguagens, banco, regras, testes e roteiro da demonstração): [APRESENTACAO.md](APRESENTACAO.md)**

## Problema

Muitas pequenas empresas calculam a folha de pagamento à mão ou em planilhas, o que gera erros nos descontos de INSS e IRRF, benefícios descontados acima do permitido e falta de histórico confiável.

## Solução

Um sistema que cadastra empresas e funcionários, calcula a folha automaticamente com as regras de 2026, controla benefícios com limites de desconto e guarda o histórico de todas as folhas.

## ODS

ODS 8 - Trabalho Decente e Crescimento Econômico.

## Funcionalidades

- Login da empresa por CNPJ e senha (hash SHA-256 com salt), com 3 tentativas
- Cadastro de empresa e redefinição de senha
- Cadastro, edição de todos os dados e exclusão (ou desligamento) de funcionários
- Cálculo da folha: INSS progressivo, IRRF 2026 e FGTS
- Benefícios com desconto em % e limites:
  - Vale-transporte: até 6% do salário
  - Vale-refeição e vale-alimentação: até 20% do valor do benefício
  - Demais benefícios: até 10% do salário (regra do projeto)
- Editar e remover benefícios de um funcionário
- Folha lançada como PENDENTE, com opção de confirmar ou apagar (só pendentes)
- Histórico de folhas por funcionário e por mês da empresa, com totais

## Tecnologias

- Java 21
- SQLite com JDBC (sqlite-jdbc)
- Eclipse IDE
- Git e GitHub (Git Flow)

## Como executar

1. Clone o repositório e importe no Eclipse (File > Import > Existing Projects into Workspace).
2. Adicione o jar do sqlite-jdbc ao Build Path do projeto.
3. Crie o banco `database/folha.db` executando o script `database/folha2.db.sql` (por exemplo, no DB Browser for SQLite). O arquivo `folha.db` não vai para o Git.
4. Execute a classe `Folha_Pagamentos.folha`.

## Versão web

Além do menu no console, o sistema tem telas no navegador (HTML, CSS e JavaScript, na pasta `web/`). Elas usam o mesmo banco SQLite, e todas as contas (INSS, IRRF, FGTS, horas extras, faltas e limites de benefício) continuam sendo feitas no Java.

### Como rodar

1. Recrie o banco a partir de `database/folha2.db.sql`, por exemplo no DB Browser for SQLite ou com `sqlite3 database/folha.db < database/folha2.db.sql`. Assim entram os dados de exemplo de junho a novembro de 2026.
2. No Eclipse, rode a classe `Folha_Pagamentos.Servidor`. O `jdk.httpserver` já vem no JDK 21; não é preciso nenhum jar novo além do sqlite-jdbc.
3. Abra `http://localhost:8080` no navegador.
4. Entre com a empresa de exemplo: CNPJ `12.345.678/0001-99` e senha `123456`.

O servidor só aceita conexões do próprio computador (127.0.0.1).

### Bancos antigos

Ao iniciar, tanto o servidor quanto o console preparam o banco automaticamente: criam as colunas `email` e `ativo` em `funcionarios`, o tipo de lançamento "Benefício" e o "Vale Combustível", e convertem senhas guardadas em texto puro para o formato com hash. Assim o login de exemplo funciona mesmo num `folha.db` antigo. Os dados de exemplo novos só aparecem recriando o banco (passo 1).

### Telas

- **Acesso:** entrar (3 tentativas por CNPJ; depois disso, 5 minutos de bloqueio), cadastrar empresa e redefinir senha.
- **Início:** líquido do mês, funcionários, folhas pendentes, FGTS, gráfico dos últimos 6 meses e folhas do mês.
- **Funcionários:** lista com busca e filtro de experiência, cadastro, edição de todos os dados e ficha. O botão Excluir apaga de vez quem não tem folha; quem já tem fica desligado, para não perder o histórico.
- **Lançar folha:** horas extras 50% e 100%, faltas e atrasos, com o holerite calculado na hora. A folha é gravada como pendente.
- **Folhas pendentes:** confirmar ou apagar.
- **Histórico:** por mês (com totais) ou por funcionário, com impressão do holerite.
- **Benefícios:** dar, editar e remover, com o limite de desconto mostrado na hora.

### Regras de horas extras e faltas

- Valor da hora = salário base ÷ 220.
- Hora extra 50% = hora × 1,5; hora extra 100% = hora × 2.
- Dia de falta = salário base ÷ 30; atraso = hora × horas.
- Faltas e atrasos diminuem a base do INSS, do IRRF e do FGTS.

O console também pergunta horas extras, faltas e atrasos ao lançar a folha (Enter = 0), e dá o mesmo valor que a tela.

### Testes

Os testes automáticos ficam em `test/` (JUnit 5 para o Java e `node:test` para a formatação do front). A pasta `test/` não é uma pasta de código do Eclipse, então não interfere no projeto.

## Versionamento

Git Flow: `main` (versões entregues), `develop` (integração) e branches `feature/...` para cada funcionalidade, sempre integradas por Pull Request. Commits semânticos (`feat:`, `fix:`, `docs:`).

Professor: Daniel Henrique Matos de Paiva
