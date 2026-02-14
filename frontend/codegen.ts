// eslint-disable-next-line import/no-extraneous-dependencies
import type { CodegenConfig } from '@graphql-codegen/cli';

const config: CodegenConfig = {
  schema: '../app/src/test/resources/api-schema.graphqls',
  documents: ['src/**/*.vue', 'src/**/*.ts'],
  ignoreNoDocuments: true,
  generates: {
    './src/services/api/gql/': {
      preset: 'client',
      config: {
        useTypeImports: true,
      },
      plugins: [],
    },
  },
};

export default config;
