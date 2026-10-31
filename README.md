# Dark Chicken — Comanda Digital

Projeto acadêmico fullstack para uma dark kitchen, com Angular 21, Java 17 e Spring Boot. O trabalho aplica modelagem de requisitos, API REST, autenticação JWT, autorização por perfis, persistência relacional e migrações versionadas.

**Estado atual da publicação:** demonstração parcial funcional. Cardápio e autenticação de clientes estão integrados à API. O fluxo visual de pedidos ainda usa armazenamento local e não deve ser apresentado como uma operação de delivery completa.

## Sprint 1 em revisão — ainda não implantado

Esta branch conecta checkout, histórico, acompanhamento e cozinha aos endpoints Spring existentes, preservando as telas e o schema. Acrescenta recuperação limitada de leituras, detalhe restrito ao proprietário, baixa de estoque em CONFIRMADO e testes. A publicação nos links abaixo continua na versão anterior até aprovação e deploy separado.

Consulte [contratos, validação, ambiente MySQL isolado e limitações](docs/SPRINT1.md). Testes unitários aprovados não equivalem a uma homologação ponta a ponta; a integração completa com MySQL permanece pendente nesta entrega local. Nenhum dado da Aiven foi usado pelos testes.

## Demonstração

- **Aplicação:** https://comanda-digital-blue.vercel.app/
- **Saúde da API:** https://dark-chicken-api.onrender.com/api/public/status
- **Swagger/OpenAPI:** https://dark-chicken-api.onrender.com/swagger-ui.html
- **Especificação JSON:** https://dark-chicken-api.onrender.com/v3/api-docs

O backend utiliza o plano gratuito do Render e pode levar alguns minutos para iniciar após inatividade. A interface contém nomes legados, como BomRango e Dark Kitchen App; todos pertencem a esta demonstração. Use somente dados fictícios nos testes. Não há credenciais administrativas públicas.

## Funcionalidades da publicação vigente (antes do Sprint 1)

| Área | Versão publicada |
| --- | --- |
| Cardápio | Consulta à API, filtros por categoria, fotos, preços e detalhes dos pratos. |
| Cadastro e login | Cadastro público exclusivo de clientes, validação dos dados, tratamento de e-mail duplicado e sessão JWT. |
| Controle de acesso | Rotas protegidas e autorização no backend para CLIENTE, ADMIN, GERENTE e COZINHEIRO. |
| Carrinho | Inclusão, alteração de quantidade e cálculo de total no frontend. |
| Pedidos no frontend | Protótipo com persistência em localStorage; não sincroniza entre usuários ou dispositivos. |
| Pedidos na API | Endpoints de criação, consulta, histórico e operação administrativa implementados separadamente; a interface ainda precisa ser conectada a eles. |
| Administração na API | Pratos, categorias, usuários, ingredientes, estoque, fichas técnicas, fornecedores, compras e dashboard. O fluxo completo de cada tela não foi homologado em produção. |
| Qualidade | Testes Angular/Vitest e testes Java de validação, tratamento de erros, autenticação, RBAC e regras de negócio. |

## Arquitetura e tecnologias

| Camada | Tecnologias e responsabilidade |
| --- | --- |
| Frontend | Angular 21, TypeScript, Reactive Forms, RxJS, Chart.js e ng2-charts. Hospedado na Vercel. |
| API | Java 17, Spring Boot 3.5, Spring Web, Spring Security e Bean Validation. Container Docker no Render. |
| Dados | MySQL 8 na Aiven, Spring Data JPA/Hibernate e 10 migrações Flyway. |
| Segurança | Tokens JWT, hash BCrypt e autorização por papéis. Segredos definidos no ambiente de execução. |
| Testes | Vitest no frontend; JUnit, Spring Boot Test e MockMvc no backend. |

A interface usa `/api` no ambiente de produção. O arquivo `angular_project/vercel.json` encaminha essa rota ao Render e mantém o fallback para `index.html` nas rotas da SPA. O diretório publicado é `dist/angular_project/browser`. A API conecta ao MySQL da Aiven com TLS.

## Estrutura

```text
angular_project/
  src/app/          Componentes, serviços, guards e testes
  src/environments Configuração de desenvolvimento e produção
  public/          Recursos estáticos
  vercel.json      Saída do build, proxy da API e rotas da SPA
backend/
  src/main/java/   Controllers, serviços, DTOs, entidades e segurança
  src/main/resources/db/migration/  Migrações Flyway
  src/test/        Testes unitários e de integração
  Dockerfile       Build Maven e execução Java em múltiplos estágios
```

## Execução local

Pré-requisitos: Node.js 24, npm, Java 17+, Maven 3.9+ e MySQL 8 em um banco dedicado de desenvolvimento.

### Backend

1. Crie um banco de desenvolvimento `comanda_digital` e um usuário com acesso apenas a ele.
2. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` e `CORS_ALLOWED_ORIGINS` no ambiente do processo. Os nomes estão em `backend/.env.example`; o Spring Boot não carrega um arquivo `.env` automaticamente.
3. Use uma chave JWT aleatória com pelo menos 32 caracteres. Não reutilize segredos de produção.
4. Execute:

```bash
cd backend
mvn spring-boot:run
```

A API inicia em `http://localhost:8080`. O Flyway aplica as migrações automaticamente. Para provisionar o administrador inicial, defina `INITIAL_ADMIN_PASSWORD` com uma senha forte de pelo menos 12 caracteres somente no primeiro provisionamento e remova a variável depois. Sem essa configuração, a conta padrão de demonstração fica inativa. A senha nunca deve ser adicionada ao código ou ao README.

### Frontend

Em outro terminal:

```bash
cd angular_project
npm ci
npm start
```

Abra `http://localhost:4200`. O ambiente de desenvolvimento consulta `http://localhost:8080/api`.

## Validação realizada em 29/10/2026

| Verificação | Resultado |
| --- | --- |
| Suíte completa do frontend | 20 testes aprovados em 8 arquivos. |
| Build Angular de produção | Aprovado; avisos de orçamento de tamanho de alguns bundles e estilos. |
| Auditoria npm de produção | Zero vulnerabilidades reportadas. |
| Testes unitários Java | 6 aprovados: DTOs e tratamento de exceções. |
| Suíte Java de integração | Não executada integralmente neste ambiente: o MySQL local não conseguiu criar socket. Não foi usado o banco de produção. |
| Integração publicada | Saúde da API, consulta ao cardápio, cadastro e login de cliente, conflito de e-mail e restrições de acesso verificados. |

Comandos para reproduzir:

```bash
cd angular_project
npm run build
npm test -- --watch=false
npm audit --omit=dev --audit-level=moderate
```

```bash
cd backend
mvn -Dtest=DtoValidationTest,GlobalExceptionHandlerTest test
# Com o MySQL descartável do compose.test.yml e a proteção de schema de teste:
mvn verify
```

**Os testes de integração fazem alterações no banco indicado pela configuração.** Execute-os somente em uma base descartável de testes. Nunca aponte a suíte para a Aiven de produção. A configuração em `src/test/resources` habilita dados de demonstração exclusivamente nesse contexto isolado.

## Limitações conhecidas e próximos passos

- Conectar carrinho, finalização e acompanhamento aos endpoints REST de pedidos. Hoje os pedidos da interface são locais e não chegam à cozinha em outro dispositivo.
- O cadastro de empresas é um formulário de protótipo sem persistência.
- Recuperação automática de senha não está implementada; a tela informa a limitação.
- Selecionar Pix, cartão ou dinheiro não realiza cobrança: não há gateway de pagamentos integrado.
- GPS, notificações e alguns indicadores visuais não constituem integrações de produção homologadas.
- A conta administrativa padrão permanece desativada até o provisionamento seguro. Painéis internos exigem uma conta autorizada.
- Homologar o fluxo completo de pedidos e perfis em um ambiente de testes separado.
- Consolidar identidade visual e reduzir os avisos de tamanho do build.

## Decisões técnicas demonstradas

- Separação entre apresentação, serviços de negócio, persistência e contratos de API.
- Validação no cliente e no servidor, sem permitir criação pública de perfis administrativos.
- Migrações Flyway preservadas: não se alteram scripts já aplicados para evitar divergência de checksums.
- Deploy do frontend e do backend independentes, com configuração por ambiente e conexão TLS ao banco gerenciado.
- Distinção explícita entre funcionalidades integradas, protótipos e itens ainda não homologados.

Este repositório foi preparado como portfólio acadêmico e demonstração de engenharia de software. Não representa um sistema de delivery pronto para operação comercial.
