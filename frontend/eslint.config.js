import js from '@eslint/js';
import typescript from '@typescript-eslint/eslint-plugin';
import typescriptParser from '@typescript-eslint/parser';
import importPlugin from 'eslint-plugin-import';
import vue from 'eslint-plugin-vue';
import globals from 'globals';
import vueParser from 'vue-eslint-parser';

export default [
  // Include recommended configurations
  js.configs.recommended,
  ...vue.configs['flat/essential'],

  // Base configuration for all files
  {
    files: ['**/*.{js,mjs,cjs,ts,vue}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      parser: vueParser,
      parserOptions: {
        parser: typescriptParser,
        extraFileExtensions: ['.vue'],
      },
      globals: {
        ...globals.browser,
        ...globals.es2022,
        RequestInit: 'readonly',
        defineModel: 'readonly',
      },
    },
    plugins: {
      vue,
      '@typescript-eslint': typescript,
      import: importPlugin,
    },
    rules: {
      // Vue rules
      'vue/max-len': [
        'error',
        {
          code: 120,
          template: 200,
        },
      ],
      'vue/script-indent': [
        'error',
        2,
        {
          baseIndent: 1,
        },
      ],
      'vue/component-name-in-template-casing': [
        'error',
        'PascalCase',
        {
          registeredComponentsOnly: false,
        },
      ],
      'vue/multi-word-component-names': 'off',
      'vue/require-default-prop': 'off',
      'vue/no-setup-props-destructure': 'off',

      // TypeScript rules
      '@typescript-eslint/ban-ts-comment': 'off',
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          caughtErrorsIgnorePattern: '^_',
        },
      ],

      // Import rules
      'import/no-useless-path-segments': 'off',
      'import/extensions': 'off',
      'import/no-unresolved': 'off',
      'import/prefer-default-export': 'off',
      'import/no-extraneous-dependencies': [
        'error',
        {
          devDependencies: ['**/*.spec.ts', 'vite.config.ts', 'eslint.config.js', 'build-config/**/*.*'],
        },
      ],
      'import/no-import-module-exports': 'off',

      // General rules
      'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'off',
      'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
      'no-use-before-define': [
        'error',
        {
          functions: false,
        },
      ],
      'template-curly-spacing': 'off',
      indent: 'off',
      'no-unused-vars': 'off',
      'no-useless-assignment': 'off',
      'consistent-return': 'off',
      'no-restricted-syntax': 'off',
      'max-classes-per-file': 'off',
    },
  },

  // Node.js configuration for config files
  {
    files: ['**/build-config/**/*.{js,ts}', 'vite.config.ts', 'eslint.config.js'],
    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
  },

  // TypeScript-specific configuration
  {
    files: ['**/*.{ts,tsx,vue}'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: typescriptParser,
      },
    },
    rules: {
      // Allow unused parameters that start with underscore
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          caughtErrorsIgnorePattern: '^_',
        },
      ],
    },
  },

  // Test files configuration
  {
    files: ['**/*.{test,spec}.{js,ts,tsx}', '**/__tests__/**/*.{js,ts,tsx}'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.vitest,
      },
    },
  },

  // Ignore patterns (replaces .eslintignore)
  {
    ignores: ['src/services/api/generated/**', 'components.d.ts', 'dist/*', 'build/*'],
  },
];
