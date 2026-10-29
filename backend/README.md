# Comanda Digital API

Backend Spring Boot da aplicação Comanda Digital.

## Pré-requisitos

- JDK 17 ou superior
- Maven 3.6.3 ou superior
- MySQL 8 ou superior

## Banco local

Crie o banco e um usuário dedicado no MySQL:

```sql
CREATE DATABASE comanda_digital
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'comanda_app'@'localhost' IDENTIFIED BY 'troque_esta_senha';
GRANT ALL PRIVILEGES ON comanda_digital.* TO 'comanda_app'@'localhost';
FLUSH PRIVILEGES;
```

Defina `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS` e
`JWT_SECRET` no ambiente. A chave JWT deve ter pelo menos 32 caracteres e nunca
deve ser versionada. Consulte `.env.example`.

## Executar

Na primeira execução, a configuração local pode ser automatizada:

```powershell
cd angularproject\backend
powershell -ExecutionPolicy Bypass -File .\scripts\setup-local.ps1
```

O script baixa distribuições oficiais portáteis do JDK 17, Maven e MySQL para
pastas locais ignoradas pelo Git. Depois, para iniciar o MySQL e o backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

Se Java, Maven e MySQL já estiverem instalados globalmente, também é possível usar:

```powershell
mvn spring-boot:run
```

Com a aplicação iniciada:

- Status: http://localhost:8080/api/public/status
- Swagger: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

O Hibernate apenas valida o schema. A criação e evolução das tabelas serão feitas exclusivamente pelo Flyway.

## Preparação para Railway

O `Dockerfile` na raiz desta pasta gera a imagem de produção com Java 17. Para
publicar, crie um serviço MySQL chamado `MySQL` no mesmo projeto e importe no
serviço do backend as referências de `railway.env.example`. Gere um
`JWT_SECRET` exclusivo e nunca publique o valor no repositório.

Use `/api/public/status` como healthcheck. O backend já respeita a variável
`PORT` fornecida pela plataforma. Depois do deploy do frontend, substitua
`CORS_ALLOWED_ORIGINS` pela URL pública do site.
