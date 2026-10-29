function escapeWindowsParentheses(testConfig) {
  if (!Array.isArray(testConfig?.include)) return;

  testConfig.include = testConfig.include.map((pattern) =>
    pattern.replace(/(^|[^\\])([()])/g, '$1\\$2')
  );
}

export default {
  plugins: [
    {
      name: 'comanda-digital:escape-windows-test-paths',
      enforce: 'post',
      config(config) {
        escapeWindowsParentheses(config.test);
      },
      configResolved(config) {
        escapeWindowsParentheses(config.test);
      },
    },
  ],
};
