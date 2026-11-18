BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "beneficios" (
	"id"	INTEGER,
	"nome"	TEXT NOT NULL UNIQUE,
	"descricao"	TEXT,
	"tipo"	TEXT NOT NULL,
	"valor_padrao"	REAL DEFAULT 0,
	"ativo"	INTEGER NOT NULL DEFAULT 1,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "cargos" (
	"id"	INTEGER,
	"nome"	TEXT NOT NULL UNIQUE,
	"descricao"	TEXT,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "empresas" (
	"id"	INTEGER,
	"razao_social"	TEXT NOT NULL,
	"nome_fantasia"	TEXT NOT NULL,
	"cnpj"	TEXT NOT NULL UNIQUE,
	"email"	TEXT NOT NULL,
	"telefone"	TEXT,
	"data_cadastro"	TEXT NOT NULL,
	"ativo"	INTEGER NOT NULL DEFAULT 1,
PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "folha_funcionarios" (
	"id"	INTEGER,
	"folha_id"	INTEGER NOT NULL,
	"funcionario_id"	INTEGER NOT NULL,
	"salario_base"	REAL NOT NULL,
	"total_proventos"	REAL NOT NULL,
	"salario_bruto"	REAL NOT NULL,
	"base_inss"	REAL NOT NULL,
	"valor_inss"	REAL NOT NULL,
	"base_fgts"	REAL NOT NULL,
	"valor_fgts"	REAL NOT NULL,
	"base_irpf"	REAL NOT NULL,
	"valor_irpf"	REAL NOT NULL,
	"total_descontos"	REAL NOT NULL,
	"salario_liquido"	REAL NOT NULL, 
	"status"	TEXT DEFAULT 'CONFIRMADA',
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("folha_id") REFERENCES "folhas_pagamento"("id"),
	FOREIGN KEY("funcionario_id") REFERENCES "funcionarios"("id")
);
CREATE TABLE IF NOT EXISTS "folhas_pagamento" (
	"id"	INTEGER,
	"mes"	INTEGER NOT NULL,
	"ano"	INTEGER NOT NULL,
	"data_pagamento"	TEXT,
	"status"	TEXT NOT NULL,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "funcionarios" (
	"id"	INTEGER,
	"nome"	TEXT NOT NULL,
	"cpf"	TEXT NOT NULL UNIQUE,
	"data_nascimento"	TEXT NOT NULL,
	"data_admissao"	TEXT NOT NULL,
	"cargo"	TEXT NOT NULL,
	"salario_base"	REAL NOT NULL,
	"empresa_id"	INTEGER,
	"email"	TEXT,
	"ativo"	INTEGER NOT NULL DEFAULT 1,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "funcionarios_beneficios" (
	"id"	INTEGER,
	"funcionario_id"	INTEGER NOT NULL,
	"beneficio_id"	INTEGER NOT NULL,
	"valor"	REAL NOT NULL,
	"desconto_folha"	REAL NOT NULL DEFAULT 0,
	"data_inicio"	TEXT NOT NULL,
	"data_fim"	TEXT,
	"ativo"	INTEGER NOT NULL DEFAULT 1,
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("beneficio_id") REFERENCES "beneficios"("id"),
	FOREIGN KEY("funcionario_id") REFERENCES "funcionarios"("id")
);
CREATE TABLE IF NOT EXISTS "lancamentos_folha" (
	"id"	INTEGER,
	"folha_funcionario_id"	INTEGER NOT NULL,
	"tipo_lancamento_id"	INTEGER NOT NULL,
	"referencia"	REAL,
	"valor"	REAL NOT NULL,
	"descricao"	TEXT,
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("folha_funcionario_id") REFERENCES "folha_funcionarios"("id"),
	FOREIGN KEY("tipo_lancamento_id") REFERENCES "tipos_lancamento"("id")
);
CREATE TABLE IF NOT EXISTS "tipos_lancamento" (
	"id"	INTEGER,
	"nome"	TEXT NOT NULL UNIQUE,
	"tipo"	TEXT NOT NULL,
	"descricao"	TEXT,
	"incide_inss"	INTEGER NOT NULL DEFAULT 0,
	"incide_fgts"	INTEGER NOT NULL DEFAULT 0,
	"incide_irpf"	INTEGER NOT NULL DEFAULT 0,
	"operacao_base"	TEXT NOT NULL DEFAULT 'SOMA',
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "usuarios" (
	"id"	INTEGER,
	"empresa_id"	INTEGER NOT NULL,
	"usuario"	TEXT NOT NULL UNIQUE,
	"senha"	TEXT NOT NULL,
	"email"	TEXT NOT NULL,
	"ativo"	INTEGER NOT NULL DEFAULT 1,
	"data_cadastro"	TEXT NOT NULL,
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("empresa_id") REFERENCES "empresas"("id")
);
INSERT INTO "beneficios" VALUES (1,'Vale Transporte','Benefício destinado ao deslocamento do funcionário','TRANSPORTE',300.0,1);
INSERT INTO "beneficios" VALUES (2,'Vale Refeição','Benefício destinado à alimentação durante a jornada de trabalho','ALIMENTACAO',500.0,1);
INSERT INTO "beneficios" VALUES (3,'Vale Alimentação','Benefício destinado à compra de alimentos','ALIMENTACAO',400.0,1);
INSERT INTO "beneficios" VALUES (4,'Plano de Saúde','Assistência médica oferecida ao funcionário','SAUDE',250.0,1);
INSERT INTO "beneficios" VALUES (5,'Plano Odontológico','Assistência odontológica oferecida ao funcionário','SAUDE',50.0,1);
INSERT INTO "beneficios" VALUES (6,'Vale Combustível','Benefício destinado ao abastecimento do veículo do funcionário','TRANSPORTE',200.0,1);
INSERT INTO "cargos" VALUES (1,'Auxiliar Administrativo','Profissional responsável por atividades administrativas');
INSERT INTO "cargos" VALUES (2,'Analista Administrativo','Profissional responsável por atividades de análise e suporte administrativo');
INSERT INTO "cargos" VALUES (3,'Analista Financeiro','Profissional responsável pelo controle financeiro');
INSERT INTO "cargos" VALUES (4,'Auxiliar de RH','Profissional de apoio às rotinas de pessoal');
INSERT INTO "cargos" VALUES (5,'Técnico de Manutenção','Profissional responsável pela manutenção predial');
INSERT INTO "cargos" VALUES (6,'Coordenadora Administrativa','Profissional responsável pela coordenação administrativa');
INSERT INTO "empresas" VALUES (1,'Administracao de Facilities LTDA','Administracao de Facilities','12345678000199','contato@facilities.com.br','(31) 3333-4444','2026-10-03',1);
INSERT INTO "folha_funcionarios" VALUES (1,1,1,2500.0,2727.27,2727.27,2627.27,212.14,2627.27,210.18,2415.13,0.0,312.14,2415.13,'CONFIRMADA');
INSERT INTO "folhas_pagamento" VALUES (1,9,2026,'2026-09-30','ABERTA');
INSERT INTO "funcionarios" VALUES (1,'João da Silva','52998224725','1995-05-20','2026-01-10','Auxiliar Administrativo',2500.0,1,'joao.silva@facilities.com.br',1);
INSERT INTO "funcionarios" VALUES (2,'Maria Oliveira','16899535009','1998-08-15','2026-02-05','Analista Administrativo',3500.0,1,'maria.oliveira@facilities.com.br',1);
INSERT INTO "funcionarios" VALUES (3,'Carlos Souza','39053344705','1988-04-22','2025-03-17','Analista Financeiro',6200.0,1,'carlos.souza@facilities.com.br',1);
INSERT INTO "funcionarios" VALUES (4,'Ana Ribeiro','74697131401','2001-12-03','2026-09-01','Auxiliar de RH',2100.0,1,'ana.ribeiro@facilities.com.br',1);
INSERT INTO "funcionarios" VALUES (5,'Pedro Almeida','28117463075','1992-07-30','2025-08-04','Técnico de Manutenção',2800.0,1,'pedro.almeida@facilities.com.br',1);
INSERT INTO "funcionarios" VALUES (6,'Juliana Costa','85351346893','1985-02-14','2024-11-11','Coordenadora Administrativa',5200.0,1,'juliana.costa@facilities.com.br',1);
INSERT INTO "funcionarios_beneficios" VALUES (1,1,1,300.0,150.0,'2026-01-10',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (2,1,2,500.0,50.0,'2026-01-10',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (3,1,4,250.0,100.0,'2026-01-10',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (4,2,1,300.0,150.0,'2026-02-05',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (5,2,3,400.0,0.0,'2026-02-05',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (6,2,5,50.0,25.0,'2026-02-05',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (7,3,4,250.0,120.0,'2025-03-17',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (8,4,1,300.0,126.0,'2026-09-01',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (9,5,1,300.0,168.0,'2025-08-04',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (10,5,2,500.0,100.0,'2025-08-04',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (11,6,4,250.0,125.0,'2024-11-11',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (12,6,3,400.0,0.0,'2024-11-11',NULL,1);
INSERT INTO "lancamentos_folha" VALUES (1,1,1,30.0,2500.0,'Salário mensal - Setembro/2026');
INSERT INTO "lancamentos_folha" VALUES (2,1,11,NULL,212.14,'INSS - Setembro/2026 - calculado');
INSERT INTO "lancamentos_folha" VALUES (3,1,2,10.0,227.27,'10 horas extras com adicional de 50%');
INSERT INTO "lancamentos_folha" VALUES (4,1,13,NULL,100.0,'Falta - Setembro/2026');
INSERT INTO "tipos_lancamento" VALUES (1,'Salário Base','PROVENTO','Salário mensal do funcionário',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (2,'Hora Extra 50%','PROVENTO','Hora extra com adicional de 50%',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (3,'Hora Extra 100%','PROVENTO','Hora extra com adicional de 100%',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (4,'Adicional Noturno','PROVENTO','Adicional referente ao trabalho noturno',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (5,'Insalubridade','PROVENTO','Adicional de insalubridade',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (6,'Periculosidade','PROVENTO','Adicional de periculosidade',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (7,'Comissão','PROVENTO','Comissões recebidas pelo funcionário',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (8,'DSR','PROVENTO','Descanso Semanal Remunerado',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (9,'Vale Transporte','DESCONTO','Desconto referente ao vale transporte',0,0,0,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (10,'Vale Refeição','DESCONTO','Desconto referente ao vale refeição',0,0,0,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (11,'INSS','DESCONTO','Contribuição previdenciária',0,0,0,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (12,'IRPF','DESCONTO','Imposto de Renda Retido na Fonte',0,0,0,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (13,'Falta','DESCONTO','Desconto referente a faltas',1,1,1,'SUBTRAI');
INSERT INTO "tipos_lancamento" VALUES (14,'Atraso','DESCONTO','Desconto referente a atrasos',1,1,1,'SUBTRAI');
INSERT INTO "tipos_lancamento" VALUES (15,'Adiantamento Salarial','DESCONTO','Adiantamento recebido anteriormente',0,0,0,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (16,'Benefício','DESCONTO','Desconto de benefício do funcionário',0,0,0,'SOMA');
INSERT INTO "usuarios" VALUES (1,1,'admin_facilities','k+9pncshZD95MaVqkkSZGw==:1RAy5ZabPG/+3XVDjtjhbpFRPrnCh4VHvyocDo41kSE=','admin@facilities.com.br',1,'2026-10-03');
INSERT INTO "folhas_pagamento" VALUES (2,6,2026,'2026-06-30','ABERTA');
INSERT INTO "folhas_pagamento" VALUES (3,7,2026,'2026-07-31','ABERTA');
INSERT INTO "folhas_pagamento" VALUES (4,8,2026,'2026-08-31','ABERTA');
INSERT INTO "folhas_pagamento" VALUES (5,10,2026,'2026-10-31','ABERTA');
INSERT INTO "folhas_pagamento" VALUES (6,11,2026,'2026-11-30','ABERTA');
INSERT INTO "folha_funcionarios" VALUES (2,2,1,2500.0,2500.0,2500.0,2500.0,200.69,2500.0,200.0,2299.31,0.0,500.69,1999.31,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (3,2,2,3500.0,3500.0,3500.0,3500.0,308.6,3500.0,280.0,3191.4,0.0,483.6,3016.4,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (4,2,3,6200.0,6200.0,6200.0,6200.0,669.51,6200.0,496.0,5530.49,459.03,1248.54,4951.46,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (5,2,5,2800.0,2800.0,2800.0,2800.0,227.69,2800.0,224.0,2572.31,0.0,495.69,2304.31,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (6,2,6,5200.0,5200.0,5200.0,5200.0,529.51,5200.0,416.0,4670.49,71.62,726.13,4473.87,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (7,3,1,2500.0,2568.18,2568.18,2568.18,206.82,2568.18,205.45,2361.36,0.0,506.82,2061.36,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (8,3,2,3500.0,3500.0,3500.0,3500.0,308.6,3500.0,280.0,3191.4,0.0,483.6,3016.4,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (9,3,3,6200.0,6200.0,6200.0,6200.0,669.51,6200.0,496.0,5530.49,459.03,1248.54,4951.46,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (10,3,5,2800.0,2800.0,2800.0,2800.0,227.69,2800.0,224.0,2572.31,0.0,495.69,2304.31,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (11,3,6,5200.0,5200.0,5200.0,5026.67,505.25,5026.67,402.13,4521.42,9.55,813.13,4386.87,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (12,4,1,2500.0,2500.0,2500.0,2500.0,200.69,2500.0,200.0,2299.31,0.0,500.69,1999.31,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (13,4,2,3500.0,3643.18,3643.18,3643.18,325.78,3643.18,291.45,3317.4,0.0,500.78,3142.4,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (14,4,3,6200.0,6200.0,6200.0,6200.0,669.51,6200.0,496.0,5530.49,459.03,1248.54,4951.46,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (15,4,5,2800.0,2800.0,2800.0,2800.0,227.69,2800.0,224.0,2572.31,0.0,495.69,2304.31,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (16,4,6,5200.0,5200.0,5200.0,5200.0,529.51,5200.0,416.0,4670.49,71.62,726.13,4473.87,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (17,1,2,3500.0,3500.0,3500.0,3500.0,308.6,3500.0,280.0,3191.4,0.0,483.6,3016.4,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (18,1,3,6200.0,6200.0,6200.0,6200.0,669.51,6200.0,496.0,5530.49,459.03,1248.54,4951.46,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (19,1,4,2100.0,2100.0,2100.0,2100.0,164.69,2100.0,168.0,1935.31,0.0,290.69,1809.31,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (20,1,5,2800.0,2800.0,2800.0,2800.0,227.69,2800.0,224.0,2572.31,0.0,495.69,2304.31,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (21,1,6,5200.0,5200.0,5200.0,5200.0,529.51,5200.0,416.0,4670.49,71.62,726.13,4473.87,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (22,5,1,2500.0,2500.0,2500.0,2500.0,200.69,2500.0,200.0,2299.31,0.0,500.69,1999.31,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (23,5,2,3500.0,3500.0,3500.0,3500.0,308.6,3500.0,280.0,3191.4,0.0,483.6,3016.4,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (24,5,3,6200.0,6200.0,6200.0,6200.0,669.51,6200.0,496.0,5530.49,459.03,1248.54,4951.46,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (25,5,4,2100.0,2100.0,2100.0,2100.0,164.69,2100.0,168.0,1935.31,0.0,290.69,1809.31,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (26,5,5,2800.0,2952.73,2952.73,2952.73,242.93,2952.73,236.22,2709.8,0.0,510.93,2441.8,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (27,5,6,5200.0,5200.0,5200.0,5200.0,529.51,5200.0,416.0,4670.49,71.62,726.13,4473.87,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (28,6,2,3500.0,3500.0,3500.0,3500.0,308.6,3500.0,280.0,3191.4,0.0,483.6,3016.4,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (29,6,3,6200.0,6200.0,6200.0,6200.0,669.51,6200.0,496.0,5530.49,459.03,1248.54,4951.46,'CONFIRMADA');
INSERT INTO "folha_funcionarios" VALUES (30,6,4,2100.0,2100.0,2100.0,2100.0,164.69,2100.0,168.0,1935.31,0.0,290.69,1809.31,'PENDENTE');
INSERT INTO "folha_funcionarios" VALUES (31,6,5,2800.0,2914.55,2914.55,2914.55,238.35,2914.55,233.16,2676.2,0.0,506.35,2408.2,'PENDENTE');
INSERT INTO "folha_funcionarios" VALUES (32,6,6,5200.0,5200.0,5200.0,5200.0,529.51,5200.0,416.0,4670.49,71.62,726.13,4473.87,'CONFIRMADA');
INSERT INTO "lancamentos_folha" VALUES (5,2,1,30.0,2500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (6,2,11,NULL,200.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (7,2,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (8,2,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (9,2,10,NULL,50.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (10,2,16,NULL,100.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (11,3,1,30.0,3500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (12,3,11,NULL,308.6,'INSS');
INSERT INTO "lancamentos_folha" VALUES (13,3,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (14,3,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (15,3,16,NULL,25.0,'Plano Odontológico');
INSERT INTO "lancamentos_folha" VALUES (16,4,1,30.0,6200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (17,4,11,NULL,669.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (18,4,12,NULL,459.03,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (19,4,16,NULL,120.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (20,5,1,30.0,2800.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (21,5,11,NULL,227.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (22,5,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (23,5,9,NULL,168.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (24,5,10,NULL,100.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (25,6,1,30.0,5200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (26,6,11,NULL,529.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (27,6,12,NULL,71.62,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (28,6,16,NULL,125.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (29,7,1,30.0,2500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (30,7,2,4.0,68.18,'Hora Extra 50%');
INSERT INTO "lancamentos_folha" VALUES (31,7,11,NULL,206.82,'INSS');
INSERT INTO "lancamentos_folha" VALUES (32,7,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (33,7,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (34,7,10,NULL,50.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (35,7,16,NULL,100.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (36,8,1,30.0,3500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (37,8,11,NULL,308.6,'INSS');
INSERT INTO "lancamentos_folha" VALUES (38,8,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (39,8,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (40,8,16,NULL,25.0,'Plano Odontológico');
INSERT INTO "lancamentos_folha" VALUES (41,9,1,30.0,6200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (42,9,11,NULL,669.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (43,9,12,NULL,459.03,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (44,9,16,NULL,120.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (45,10,1,30.0,2800.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (46,10,11,NULL,227.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (47,10,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (48,10,9,NULL,168.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (49,10,10,NULL,100.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (50,11,1,30.0,5200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (51,11,13,1.0,173.33,'Falta');
INSERT INTO "lancamentos_folha" VALUES (52,11,11,NULL,505.25,'INSS');
INSERT INTO "lancamentos_folha" VALUES (53,11,12,NULL,9.55,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (54,11,16,NULL,125.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (55,12,1,30.0,2500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (56,12,11,NULL,200.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (57,12,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (58,12,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (59,12,10,NULL,50.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (60,12,16,NULL,100.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (61,13,1,30.0,3500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (62,13,2,6.0,143.18,'Hora Extra 50%');
INSERT INTO "lancamentos_folha" VALUES (63,13,11,NULL,325.78,'INSS');
INSERT INTO "lancamentos_folha" VALUES (64,13,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (65,13,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (66,13,16,NULL,25.0,'Plano Odontológico');
INSERT INTO "lancamentos_folha" VALUES (67,14,1,30.0,6200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (68,14,11,NULL,669.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (69,14,12,NULL,459.03,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (70,14,16,NULL,120.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (71,15,1,30.0,2800.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (72,15,11,NULL,227.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (73,15,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (74,15,9,NULL,168.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (75,15,10,NULL,100.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (76,16,1,30.0,5200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (77,16,11,NULL,529.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (78,16,12,NULL,71.62,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (79,16,16,NULL,125.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (80,17,1,30.0,3500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (81,17,11,NULL,308.6,'INSS');
INSERT INTO "lancamentos_folha" VALUES (82,17,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (83,17,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (84,17,16,NULL,25.0,'Plano Odontológico');
INSERT INTO "lancamentos_folha" VALUES (85,18,1,30.0,6200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (86,18,11,NULL,669.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (87,18,12,NULL,459.03,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (88,18,16,NULL,120.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (89,19,1,30.0,2100.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (90,19,11,NULL,164.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (91,19,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (92,19,9,NULL,126.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (93,20,1,30.0,2800.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (94,20,11,NULL,227.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (95,20,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (96,20,9,NULL,168.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (97,20,10,NULL,100.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (98,21,1,30.0,5200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (99,21,11,NULL,529.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (100,21,12,NULL,71.62,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (101,21,16,NULL,125.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (102,22,1,30.0,2500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (103,22,11,NULL,200.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (104,22,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (105,22,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (106,22,10,NULL,50.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (107,22,16,NULL,100.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (108,23,1,30.0,3500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (109,23,11,NULL,308.6,'INSS');
INSERT INTO "lancamentos_folha" VALUES (110,23,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (111,23,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (112,23,16,NULL,25.0,'Plano Odontológico');
INSERT INTO "lancamentos_folha" VALUES (113,24,1,30.0,6200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (114,24,11,NULL,669.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (115,24,12,NULL,459.03,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (116,24,16,NULL,120.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (117,25,1,30.0,2100.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (118,25,11,NULL,164.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (119,25,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (120,25,9,NULL,126.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (121,26,1,30.0,2800.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (122,26,2,8.0,152.73,'Hora Extra 50%');
INSERT INTO "lancamentos_folha" VALUES (123,26,11,NULL,242.93,'INSS');
INSERT INTO "lancamentos_folha" VALUES (124,26,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (125,26,9,NULL,168.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (126,26,10,NULL,100.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (127,27,1,30.0,5200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (128,27,11,NULL,529.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (129,27,12,NULL,71.62,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (130,27,16,NULL,125.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (131,28,1,30.0,3500.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (132,28,11,NULL,308.6,'INSS');
INSERT INTO "lancamentos_folha" VALUES (133,28,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (134,28,9,NULL,150.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (135,28,16,NULL,25.0,'Plano Odontológico');
INSERT INTO "lancamentos_folha" VALUES (136,29,1,30.0,6200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (137,29,11,NULL,669.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (138,29,12,NULL,459.03,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (139,29,16,NULL,120.0,'Plano de Saúde');
INSERT INTO "lancamentos_folha" VALUES (140,30,1,30.0,2100.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (141,30,11,NULL,164.69,'INSS');
INSERT INTO "lancamentos_folha" VALUES (142,30,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (143,30,9,NULL,126.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (144,31,1,30.0,2800.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (145,31,2,6.0,114.55,'Hora Extra 50%');
INSERT INTO "lancamentos_folha" VALUES (146,31,11,NULL,238.35,'INSS');
INSERT INTO "lancamentos_folha" VALUES (147,31,12,NULL,0.0,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (148,31,9,NULL,168.0,'Vale Transporte');
INSERT INTO "lancamentos_folha" VALUES (149,31,10,NULL,100.0,'Vale Refeição');
INSERT INTO "lancamentos_folha" VALUES (150,32,1,30.0,5200.0,'Salário Base');
INSERT INTO "lancamentos_folha" VALUES (151,32,11,NULL,529.51,'INSS');
INSERT INTO "lancamentos_folha" VALUES (152,32,12,NULL,71.62,'IRPF');
INSERT INTO "lancamentos_folha" VALUES (153,32,16,NULL,125.0,'Plano de Saúde');
CREATE UNIQUE INDEX IF NOT EXISTS "idx_folha_funcionario_unico" ON "folha_funcionarios" (
	"folha_id",
	"funcionario_id"
);
CREATE UNIQUE INDEX IF NOT EXISTS "idx_funcionario_beneficio_ativo" ON "funcionarios_beneficios" (
	"funcionario_id",
	"beneficio_id",
	"ativo"
);
COMMIT;
