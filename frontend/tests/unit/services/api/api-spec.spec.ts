import { generateTypesForDocument } from 'openapi-client-axios-typegen';
import { readFileSync, writeFileSync } from 'fs';

const BACKEND_SPEC_PATH = '../backend/src/test/resources/api-spec.yaml';
const FRONTEND_SPEC_PATH = 'src/services/api/api-spec.json';
const CLIENT_DEFINITION_PATH = 'src/services/api/api-client-definition.ts';

describe('API Spec', () => {
  it('stored in sources should be up-to-date', () => {
    const yaml = require('js-yaml');
    const currentApiSpec = JSON.stringify(
      yaml.load(readFileSync(BACKEND_SPEC_PATH, 'utf8')),
      null,
      2,
    );

    // support simpler local workflow
    if (process.env.REPLACE_COMMITTED_SPECS) {
      writeFileSync(FRONTEND_SPEC_PATH, currentApiSpec);
    }

    const vcsApiSpec = readFileSync(FRONTEND_SPEC_PATH, 'utf8');
    expect(vcsApiSpec)
      .toEqual(currentApiSpec);
  });
});

describe('Generated API Client', () => {
  it('should be up-to-date', async () => {
    const opts = {
      transformOperationName: (operation: string) => operation,
    };
    const [imports, schemaTypes, operationTypings] = await generateTypesForDocument(
      BACKEND_SPEC_PATH, opts,
    );
    const generatedClientDefinition = `${imports}
          \n${schemaTypes.replace(/\s+$/gm, '')}
          \n${operationTypings.replace(/\s+$/gm, '')}\n`
      .replace('declare namespace', 'export namespace')
      .replace('  namespace', '  export namespace');

    // support simpler local workflow
    if (process.env.REPLACE_COMMITTED_SPECS) {
      writeFileSync(CLIENT_DEFINITION_PATH, generatedClientDefinition);
    }

    const vcsClientDefinition = readFileSync(CLIENT_DEFINITION_PATH, 'utf8');

    expect(vcsClientDefinition)
      .toEqual(generatedClientDefinition);
  });
});
