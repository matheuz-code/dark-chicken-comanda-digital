# Sprint 1 — integração incremental para revisão

Base: main `09813cfe8c88d8036e3cfc3295889f6e34811303`. Branch: `fix/dark-chicken-recuperacao-sprint1`.

Os 30 hashes anteriores do manifesto do pacote foram comparados com a base oficial e coincidiram integralmente. O pacote foi aplicado sem conflitos. Não foram substituídas a home, telas originais, autenticação ou migrações históricas. Foram preservados os testes anteriores de listagem/detalhes do catálogo junto aos casos novos.

## Comportamento proposto na branch

- Cardápio: leituras com timeout por tentativa de 10 s, até 12 tentativas e intervalo de 5 s, apenas para falhas transitórias (0/502/503/504 ou timeout). A saída da página cancela a assinatura. Respostas inválidas e 401/403 não recebem retries automáticos.
- Rascunho do carrinho em sessionStorage. Pedidos antigos em localStorage não são apagados nem importados como registros da API.
- Checkout envia IDs, quantidades, observações, endereço e forma de pagamento à API; utiliza ID, preços e total devolvidos pelo servidor. Nenhum POST/PATCH é repetido automaticamente. Pagamento permanece simulado, explicitamente identificado.
- Histórico, acompanhamento e cozinha usam a API. A mudança de status aguarda confirmação; falha não produz sucesso local. Polling da cozinha/acompanhamento a cada 15 s, enquanto a página está visível; atualização de listas é coalescida.
- Troca de sessão limpa os pedidos exibidos e descarta respostas pendentes da sessão anterior. Logout também limpa o rascunho da sessão; login preserva o carrinho anônimo para permitir o checkout.
- Detalhe de pedido de cliente é restrito ao proprietário. A cozinha confirma por ADMIN; GERENTE continua usando a gestão de pedidos existente. COZINHEIRO prepara pedidos já confirmados. Não há novo papel MOTOboy nem ampliação de RBAC.
- Estoque baixa em CONFIRMADO, com proteção existente contra duplicação. EM_PREPARO mantém fallback para pedidos confirmados antes da revisão. Cancelamento/estorno continuam no serviço original, sem migração de dados.
- `/api/public/ready` verifica DataSource e responde 200/READY ou 503/UNAVAILABLE sem detalhes JDBC. `/api/public/status` continua sendo liveness, não comprovação de banco. Nenhuma alteração do health check Render foi aplicada.

## Contratos JSON conferidos

| Fluxo | Rota / campos |
|---|---|
| Cadastro/login | Rotas originais `/api/auth/register` e `/api/auth/login`; sessão JWT e usuário com nome, telefone, endereço e perfil |
| Catálogo | GET `/api/cardapio`; página `conteudo`, prato `id`, `nome`, `precoVenda`, `categoria` |
| Criar pedido CLIENTE | POST `/api/pedidos`; `itens[{pratoId,quantidade,observacoes}]`, `enderecoEntrega`, `formaPagamento` em PIX/CARTAO/DINHEIRO; sem preço/total enviados |
| Listar do cliente | GET `/api/pedidos/meus`; página `conteudo` |
| Detalhar do cliente | GET `/api/pedidos/{id}`; nova rota de leitura com verificação de proprietário |
| Gestão/cozinha | GET `/api/admin/pedidos` e `/{id}`; PATCH `/{id}/status`; autorização existente |
| Resposta de pedido | `id`, `status`, `formaPagamento`, `valorTotal`, `criadoEm`, `enderecoEntrega`, `itens[{pratoId,nomePrato,fotoUrl,quantidade,precoUnitario,subtotal,observacoes}]` |
| Detalhe administrativo | Os campos anteriores mais `cliente`, `canal`, `timeline`, entre outros, mantidos pelo backend |

O adaptador não confunde `criadoEm` com `createdAt`. Recusa IDs/valores/itens/datas inválidos e status fora do enum. Preserva o canal real de pedidos administrativos. Datas continuam no formato ISO LocalDateTime original, sem offset explícito; a padronização de fuso entre ambientes permanece uma limitação conhecida, sem mudança silenciosa do contrato nesta revisão.

## Testes locais executados

Os resultados finais e a quantidade de casos estão no relatório de entrega. A distinção é obrigatória:

1. Angular/Vitest: HTTP simulado, componentes/guards/serviços e estado local. Não comprova o MySQL.
2. Java unitário: JUnit/Mockito e MockMvc standalone, sem banco. Todos os fontes Java, inclusive testes de integração, compilados com Java 17.
3. Integração MySQL: **não homologada localmente**. O binário oficial MySQL 8.4 inicializou o datadir descartável, mas o ambiente bloqueou o socket UNIX (`Operation not permitted`). Não foi utilizado H2 e não houve conexão de teste à Aiven.
4. Aplicação publicada: permanece no código anterior. A validação de infraestrutura de 31/10 comprovou catálogo/status/filtros/busca/detalhes; não valida o fluxo novo desta branch, que ainda não foi implantada.

O Maven convencional também encontrou limitação de DNS no ambiente local. A compilação e a execução JUnit direta utilizam as dependências das versões do projeto; não devem ser apresentadas como uma suíte Maven de integração aprovada.

## Integração MySQL reproduzível e protegida

Pré-requisitos: Docker Compose local, Java 17 e Maven. Nenhuma assinatura ou serviço de nuvem é necessário. O container é descartável, vinculado somente ao loopback, com dados fictícios e armazenamento temporário.

```bash
cd backend
docker compose -p dark-chicken-sprint1-test -f compose.test.yml up -d --wait
mvn verify
# Somente o ambiente de testes criado acima:
docker compose -p dark-chicken-sprint1-test -f compose.test.yml stop
```

Credenciais explícitas em `compose.test.yml` e `src/test/resources` são somente fixtures locais. Nunca reutilizá-las fora desse ambiente. Não exportar variáveis da Aiven no terminal de testes.

O inicializador registrado exclusivamente em `src/test/resources/META-INF/spring.factories` recusa qualquer URL que não seja MySQL em loopback e schema **dark_chicken_sprint1_test**. Também recusa `comanda_digital` local. A configuração e o inicializador de testes não são empacotados no JAR de produção.

A suíte existente cobre cadastro/login, RBAC, CRUD administrativo, criação de pedido com preços do banco, histórico, fluxo administrativo e estoque/estorno. Foram preparados novos asserts para o detalhe do proprietário (itens/total/data), 404 para outro cliente, 401 anônimo e 403 de perfil inadequado. Os testes de estoque foram ajustados para a baixa na confirmação sem segunda baixa no preparo. Esses testes precisam executar com MySQL para serem homologados.

A proposta de workflow `.github/workflows/sprint1-tests.yml` usa somente runners padrão `ubuntu-latest`, sem artifacts/cache de upload e sem segredos de produção; não contém etapa de deploy. Os jobs têm guarda para não executar em repositório privado, evitando consumo cobrável. Enquanto a branch não for publicada e o workflow não executar, CI é apenas **preparada**, não aprovada. Referência de custo: https://docs.github.com/en/billing/concepts/product-billing/github-actions (runners padrão em repositórios públicos são gratuitos; runners maiores não são utilizados).

## Limites antes de merge/deploy

- Executar a suíte completa com MySQL isolado e a jornada Angular → API → banco → nova consulta em sessões diferentes.
- Conferir cancelamento, estorno, concorrência e estoque insuficiente transacionalmente.
- Histórico usa primeira página de até 50 pedidos e chamadas de detalhe por item (N+1). Indicadores são dos pedidos carregados, não totais globais/diários nem ganhos de entregador.
- POST não tem chave de idempotência. Resposta perdida exige consultar histórico antes de repetir.
- Pagamento não cobra, cadastro de empresas e recuperação de senha mantêm limitações documentadas; logística/GPS não foram implementados nesta revisão.
- Cold start e eventual desligamento da Aiven Free continuam possíveis; não há promessa de disponibilidade contínua nem pings para evitar repouso.
- O build preserva os orçamentos existentes; avisos de bundle/CSS não foram ocultados.
- Revisão de segurança cobriu os 9 commits da base e os arquivos da branch por padrões/contexto; não constitui garantia absoluta de ausência de vulnerabilidades. Não foram detectadas credenciais operacionais ou dumps confidenciais nessa revisão. Credenciais de seed permanecem protegidas pelo mecanismo original e as migrações históricas não foram reescritas.
- Nenhum merge, deploy, alteração de visibilidade, credencial ou banco de produção é autorizado por este documento.
