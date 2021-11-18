module.exports = {
  root: true,
  env: { node: true },

  extends: [
    'plugin:vue/essential',
    '@vue/airbnb',
    '@vue/typescript',
  ],

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
    'no-use-before-define': ['error', { functions: false }],
    // the two following configs are a workaround for https://github.com/babel/babel-eslint/issues/530
    'template-curly-spacing': 'off',
    'import/no-unresolved': 'off',
    'import/prefer-default-export': 'off',
    indent: [
      'error', 2,
      { ignoredNodes: ['TemplateLiteral'] },
    ],
    'no-unused-vars': 'off',
    '@typescript-eslint/no-unused-vars': ['error'],
    'import/no-extraneous-dependencies': ['error', {
      devDependencies: [
        '**/tests/storybook/**/*.*',
        '**/*.spec.js',
        '**/*.spec.ts',
        'config/storybook/*.*'],
    }],
    'no-restricted-syntax': 'off',
    'vuejs-accessibility/click-events-have-key-events': 'off',
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

  parserOptions: {
    parser: '@typescript-eslint/parser',
  },

};
