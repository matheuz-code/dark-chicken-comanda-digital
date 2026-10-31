# Dark Chicken — Comanda Digital

Projeto acadêmico fullstack para uma dark kitchen, com Angular 21, Java 17 e Spring Boot. O trabalho aplica modelagem de requisitos, API REST, autenticação JWT, autorização por perfis, persistência relacional e migrações versionadas.

**Estado atual da publicação:** MVP de portfólio. O cardápio, a autenticação e os pedidos usam a API Spring Boot e MySQL. Use somente dados fictícios: o checkout é demonstrativo e não processa cobrança.

## Sprint 1

O Sprint 1 conecta checkout, histórico, acompanhamento e cozinha aos endpoints Spring existentes, preservando as telas e o schema. Inclui detalhe restrito ao proprietário, baixa de estoque em `CONFIRMADO`, cancelamento com estorno de estoque quando aplicável e idempotência persistente para reenvios do `POST /api/pedidos`.

Consulte [contratos, validação, ambiente MySQL isolado e limitações](docs/SPRINT1.md). Os testes de integração usam somente MySQL descartável; nenhum dado da Aiven é usado em testes.

## Demonstração

- **Aplicação:** https://comanda-digital-blue.vercel.app/
- **Saúde da API:** https://dark-chicken-api.onrender.com/api/public/status
- **Swagger/OpenAPI:** https://dark-chicken-api.onrender.com/swagger-ui.html
- **Especificação JSON:** https://dark-chicken-api.onrender.com/v3/api-docs

O backend utiliza o plano gratuito do Render e pode levar alguns minutos para iniciar após inatividade. A interface contém nomes legados, como BomRango e Dark Kitchen App; todos pertencem a esta demonstração. Use somente dados fictícios nos testes. Não há credenciais administrativas públicas.

## Funcionalidades do MVP

| Área | Versão publicada |
| --- | --- |
| Cardápio | Consulta à API, filtros por categoria, fotos, preços e detalhes dos pratos. |
| Cadastro e login | Cadastro público exclusivo de clientes, validação dos dados, tratamento de e-mail duplicado e sessão JWT. |
| Controle de acesso | Rotas protegidas e autorização no backend para CLIENTE, ADMIN, GERENTE e COZINHEIRO. |
| Carrinho | Inclusão, alteração de quantidade e cálculo de total no frontend. |
| Pedidos | Checkout cria pedidos persistidos no MySQL, com chave de idempotência associada ao cliente. Histórico e acompanhamento consultam a API; o proprietário é validado no backend. |
| Cozinha e operação | Perfis internos consultam e alteram o status dos pedidos conforme suas permissões. A baixa de estoque ocorre em `CONFIRMADO`; cancelamentos estornam apenas a movimentação aplicável. |
| Pagamento | **Demonstrativo.** Pix, cartão e dinheiro registram somente a forma escolhida; nenhum valor é cobrado e novos pedidos ficam `PENDENTE`. KPIs financeiros e pratos mais vendidos consideram apenas pagamentos realmente `PAGO`. |
| Administração | Pratos, categorias, usuários, ingredientes, estoque, fichas técnicas, fornecedores, compras e dashboard. Alguns fluxos administrativos seguem como demonstração acadêmica e devem ser homologados antes de uso comercial. |
| Qualidade | Testes Angular/Vitest e testes Java de validação, autorização, idempotência, pedidos, estoque, cancelamento, estorno e integração MySQL. |

## Arquitetura e tecnologias

| Camada | Tecnologias e responsabilidade |
| --- | --- |
| Frontend | Angular 21, TypeScript, Reactive Forms, RxJS, Chart.js e ng2-charts. Hospedado na Vercel. |
| API | Java 17, Spring Boot 3.5, Spring Web, Spring Security e Bean Validation. Container Docker no Render. |
| Dados | MySQL 8 na Aiven, Spring Data JPA/Hibernate e 11 migrações Flyway. |
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

## Validação

| Verificação | Resultado |
| --- | --- |
| Suíte completa do frontend | Vitest executado no CI. |
| Build Angular de produção | Aprovado; avisos de orçamento de tamanho de alguns bundles e estilos. |
| Auditoria npm de produção | Zero vulnerabilidades reportadas. |
| Testes Java | JUnit, MockMvc e integração MySQL executados no CI com banco descartável. |
| Segurança de dados | Nenhum teste de integração deve apontar para a Aiven de produção. |

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

- Pagamento é demonstrativo: não há gateway, cobrança, conciliação ou confirmação financeira automática.
- Recuperação automática de senha não está implementada; a tela informa a limitação.
- GPS, notificações, rastreamento de entrega e alguns indicadores visuais permanecem em desenvolvimento.
- O cadastro de empresas é um formulário de protótipo sem persistência.
- A conta administrativa padrão permanece desativada até o provisionamento seguro. Painéis internos exigem uma conta autorizada.
- Homologar o fluxo completo de pedidos e perfis em ambiente operacional separado, com pagamento real somente se vier a ser integrado e auditado.
- Consolidar identidade visual e reduzir os avisos de tamanho do build.

## Decisões técnicas demonstradas

- Separação entre apresentação, serviços de negócio, persistência e contratos de API.
- Validação no cliente e no servidor, sem permitir criação pública de perfis administrativos.
- Migrações Flyway preservadas: não se alteram scripts já aplicados para evitar divergência de checksums.
- Deploy do frontend e do backend independentes, com configuração por ambiente e conexão TLS ao banco gerenciado.
- Distinção explícita entre funcionalidades integradas, protótipos e itens ainda não homologados.

Este repositório foi preparado como portfólio acadêmico e demonstração de engenharia de software. Não representa um sistema de delivery pronto para operação comercial.
