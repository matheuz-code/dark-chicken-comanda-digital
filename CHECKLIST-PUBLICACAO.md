# Checklist antes de divulgar o repositório

- [x] Código e testes separados de arquivos pesados, caches e ferramentas locais.
- [x] Arquivos de ambiente reais excluídos; apenas exemplos incluídos.
- [x] Mock de cliente com nome/e-mail fictícios.
- [x] Recuperação de senha simulada substituída por aviso honesto.
- [x] Migração V10 bloqueia a conta seed padrão; startup permite senha inicial segura (requer validação com banco).
- [ ] Executar `npm ci`, `npm run build`, `npm test -- --watch=false`.
- [ ] Executar `mvn test` com banco de testes isolado.
- [ ] Criar repositório GitHub público.
- [ ] Confirmar que a API pública realmente responde.
- [ ] Conferir que o banco não contém dados pessoais reais.
- [ ] Configurar `INITIAL_ADMIN_PASSWORD` seguro antes do primeiro deploy e depois removê-lo.
- [ ] Validar acesso a rotas administrativas, cliente, cozinha e dados reais de demonstração.
- [ ] Capturar screenshots reais e adicionar ao README (após deploy validado).

**Não execute o teste backend contra o banco de produção.** Os testes de integração alteram o banco e dependem do seed original. Use um banco exclusivo para testes.
