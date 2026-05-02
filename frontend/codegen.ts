// eslint-disable-next-line import/no-extraneous-dependencies
import { CodegenConfig } from '@graphql-codegen/cli';

const config: CodegenConfig = {
  schema: '../app/src/test/resources/api-schema.graphqls',
  documents: ['src/**/*.vue', 'src/**/*.ts'],
  ignoreNoDocuments: true,
  generates: {
    './src/services/api/gql/schema-types.ts': {
      plugins: [
        {
          add: {
            content: '/* eslint-disable */',
          },
        },
        'typescript',
      ],
      config: {
        useTypeImports: true,
        scalars: {
          LocalDate: 'string',
          Long: 'number',
          DateTime: 'string',
        },
      },
    },
    './src/services/api/gql/': {
      preset: 'client',
      config: {
        useTypeImports: true,
        scalars: {
          LocalDate: 'string',
          Long: 'number',
          DateTime: 'string',
        },
      },
      plugins: [],
    },
  },
};

export default config;
