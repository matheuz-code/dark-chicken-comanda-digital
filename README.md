# Dark Chicken — Comanda Digital (SRS)

Projeto acadêmico **fullstack** de gestão de pedidos para restaurante, desenvolvido a partir de um documento de requisitos (SRS). Inclui interface para cliente, operação de cozinha e administração do negócio.

## Funcionalidades implementadas no código

- Cardápio digital e criação/acompanhamento de pedidos.
- Administração de pratos, categorias e usuários.
- Controle de estoque, ingredientes, ficha técnica e movimentações.
- Gestão de fornecedores e compras.
- Dashboard gerencial e controle operacional de pedidos.
- Autenticação JWT e perfis com restrição de acesso.
- Documentação da API via Swagger/OpenAPI.

> **Escopo:** a recuperação automática de senhas **não está implementada**. A tela informa essa limitação; não simula envios de e-mail.

## Tecnologias

| Camada | Stack |
| --- | --- |
| Frontend | Angular 21, TypeScript, RxJS, Chart.js |
| Backend | Java 17+, Spring Boot 3.5, Spring Security, API REST |
| Dados | MySQL, Spring Data JPA, Flyway |
| Segurança | JWT, BCrypt, RBAC (perfis) |
| Qualidade | Vitest e testes de integração Spring Boot |
| Deploy previsto | Vercel (frontend), Railway (API + MySQL) |

## Estrutura do repositório

```
angular_project/  Interface Angular + testes
backend/          API Spring Boot + migrações + testes
```

## Execução local

**Pré-requisitos:** Node.js compatível com Angular 21; Java 17+; Maven; MySQL.

1. Crie o banco MySQL `comanda_digital` e configure as variáveis descritas em `backend/.env.example` (não faça commit de `.env`).
2. No **primeiro** início, defina `INITIAL_ADMIN_PASSWORD` com uma senha forte e exclusiva de pelo menos 12 caracteres para substituir a credencial de seed. Remova essa variável do ambiente após o provisionamento. Sem ela, o administrador inicial conhecido é desativado automaticamente no início da aplicação.
3. Inicie a API em `backend/` com `mvn spring-boot:run`; por padrão utiliza a porta `8080`.
4. Inicie a interface em `angular_project/` com `npm ci` e `npm start`; por padrão utiliza a porta `4200`.

Observação: o serviço necessita de `JWT_SECRET` longo e aleatório. **Nunca exponha senhas, tokens ou banco de dados de produção no repositório.**

## Verificações locais

```bash
cd angular_project
npm ci
npm run build
npm test -- --watch=false

# em outro terminal, com Java, Maven e MySQL de testes configurados:
cd backend
mvn test
```

> A presença de testes não equivale a uma execução bem-sucedida. Consulte as validações realizadas antes de afirmar que a versão foi aprovada nos testes.

## Hospedagem

Havia URLs de Vercel e Railway documentadas no código original. **A disponibilidade dessas instalações não foi confirmada nesta preparação.** Uma demonstração pública só deve ser divulgada após verificar frontend, API e autenticação, além de configurar a senha inicial de forma segura.

- Referência do frontend: https://comanda-digital-blue.vercel.app
- Referência de saúde da API: https://backend-production-0c6b6.up.railway.app/api/public/status
- Referência Swagger: https://backend-production-0c6b6.up.railway.app/swagger-ui.html

## Segurança para demonstrações

O seed SQL histórico não foi alterado para preservar os checksums das migrações Flyway já aplicadas. A **migração V10** desativa a conta com a senha conhecida antes de a API receber requisições. Em seguida, a classe `SeedAdminProtection` pode substituir a senha se `INITIAL_ADMIN_PASSWORD` estiver configurada; sem ela, a conta continua inativa. Uma conta com senha já personalizada não é modificada. Testes de integração usam um banco isolado e ativam o seed apenas nesse contexto.

Os exemplos e mocks do frontend usam dados fictícios (`example.com`). Para um deploy real, reveja CORS, proteção de dados, chaves JWT e configurações do banco. Não reutilize dados reais de clientes.

## Contexto

Projeto desenvolvido para aplicar modelagem orientada por requisitos, API REST, persistência relacional, testes, autorização por papéis e implementação de funcionalidades de gestão de um restaurante.
