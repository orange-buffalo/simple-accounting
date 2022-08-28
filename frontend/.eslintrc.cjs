/* eslint-env node */
require('@rushstack/eslint-patch/modern-module-resolution');

module.exports = {
  root: true,
  extends: [
    'plugin:vue/vue3-essential',
    'eslint:recommended',
    '@vue/eslint-config-typescript/recommended',
    '@vue/airbnb',
    'plugin:storybook/recommended'],
  env: {
    'vue/setup-compiler-macros': true,
  },
  rules: {
    'import/no-useless-path-segments': 'off',
    'import/extensions': 'off',
    'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'vue/max-len': ['error', {
      code: 120,
    }],
    'vue/script-indent': ['error', 2, {
      baseIndent: 1,
    }],
    'vue/component-name-in-template-casing': ['error', 'PascalCase', {
      registeredComponentsOnly: false,
    }],
    '@typescript-eslint/ban-ts-comment': 'off',
    'no-use-before-define': ['error', {
      functions: false,
    }],
    // the two following configs are a workaround for https://github.com/babel/babel-eslint/issues/530
    'template-curly-spacing': 'off',
    'import/no-unresolved': 'off',
    'import/prefer-default-export': 'off',
    indent: 'off',
    'no-unused-vars': 'off',
    'consistent-return': 'off',
    '@typescript-eslint/no-unused-vars': ['error'],
    'import/no-extraneous-dependencies': ['error', {
      devDependencies: [
        '**/*.stories.ts',
        '**/*.spec.ts',
        'vite.config.ts',
        '.eslintrc.cjs',
        'build-config/**/*.*',
        'src/__storybook__/**'],
    }],
    'import/no-import-module-exports': 'off',
    'no-restricted-syntax': 'off',
    'vuejs-accessibility/click-events-have-key-events': 'off',
    'vue/multi-word-component-names': 'off',
    'vue/require-default-prop': 'off',
  },
  globals: {
    RequestInit: 'readonly',
  },
};
