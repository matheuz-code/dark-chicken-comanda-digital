-- Protect previously deployed databases before the web server starts accepting requests.
-- The default is INATIVO. Integration tests on a separate database explicitly use ATIVO.
-- Do not change historical Flyway migrations V1-V9 (their checksums may be deployed).
UPDATE usuario
SET status = '${seedAdminStatus}'
WHERE email = 'admin@email.com'
  AND senha_hash = '$2a$12$CMrugK2ki0r7ckAFXnkGbeF8Lt2/Pxp6aeEUityQF9JbtRrqPLMA2';
