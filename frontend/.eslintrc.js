module.exports = {
  root: true,
  env: { node: true },
  extends: [
    'plugin:vue/recommended',
    '@vue/airbnb'],
  rules: {
    'import/no-useless-path-segments': 'off',
    'import/extensions': 'off',
    'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'max-len': ['error', { code: 120 }],
    'vue/script-indent': ['error', 2, {
      baseIndent: 1,
    }],
    'vue/component-name-in-template-casing': ['error', 'PascalCase', {
      registeredComponentsOnly: false,
    }],
    'no-use-before-define': ['error', { 'functions': false }],
    // the two following configs are a workaround for https://github.com/babel/babel-eslint/issues/530
    'template-curly-spacing': 'off',
    'indent': [
      'error', 2,
      { ignoredNodes: ['TemplateLiteral'] },
    ],
  },
  overrides: [
    {
      files: [
        '*.vue',
      ],
      rules: {
        indent: 'off',
      },
    },
    {
      files: [
        '**/__tests__/*.{j,t}s?(x)',
        '**/tests/unit/**/*.spec.{j,t}s?(x)',
      ],
      env: {
        jest: true,
      },
    },
  ],
  parserOptions: { parser: 'babel-eslint' },
};
