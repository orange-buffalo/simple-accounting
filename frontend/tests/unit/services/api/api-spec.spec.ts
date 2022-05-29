import { generateTypesForDocument } from 'openapi-client-axios-typegen';
import { readFileSync, writeFileSync } from 'fs';
import  {describe, it, expect} from 'vitest';

const BACKEND_SPEC_PATH = '../backend/src/test/resources/api-spec.yaml';
const FRONTEND_SPEC_PATH = 'src/services/api/api-spec.json';
const CLIENT_DEFINITION_PATH = 'src/services/api/api-client-definition.ts';

function filterResponses(obj: any): void {
  for (const [key, value] of Object.entries(obj)) {
    if (key === 'responses') {
      // we are only interested in 200 response for API Client
      // remove other responses from spec to have simple response types
      const response = (value as any)['200'];
      if (response) {
        // eslint-disable-next-line no-param-reassign
        obj[key] = {
          200: response,
        };
      }
    } else if (typeof value === 'object') {
      filterResponses(value);
    }
  }
}

describe('API Spec', () => {
  it('stored in sources should be up-to-date', async () => {
    const yaml = require('js-yaml');
    const currentApiSpec = yaml.load(readFileSync(BACKEND_SPEC_PATH, 'utf8'));
    filterResponses(currentApiSpec);

    const currentApiSpecString = JSON.stringify(currentApiSpec, null, 2);

    // support simpler local workflow
    if (process.env.REPLACE_COMMITTED_SPECS) {
      writeFileSync(FRONTEND_SPEC_PATH, currentApiSpecString);
    }

    const vcsApiSpec = readFileSync(FRONTEND_SPEC_PATH, 'utf8');
    expect(vcsApiSpec)
      .toEqual(currentApiSpecString);
  });
});

describe('Generated API Client', () => {
  it('should be up-to-date', async () => {
    const opts = {
      transformOperationName: (operation: string) => operation,
    };
    const [imports, schemaTypes, operationTypings] = await generateTypesForDocument(FRONTEND_SPEC_PATH, opts);
    const generatedClientDefinition = `${imports}
          \n${schemaTypes.replace(/\s+$/gm, '')}
          \n${operationTypings.replace(/\s+$/gm, '')}\n`
      .replace(/declare namespace/g, 'export namespace')
      .replace(/ {2}namespace/g, '  export namespace')
      // workaround for generator bug
      .replace(/\$\$200/g, '$200');

    // support simpler local workflow
    if (process.env.REPLACE_COMMITTED_SPECS) {
      writeFileSync(CLIENT_DEFINITION_PATH, generatedClientDefinition);
    }

    const vcsClientDefinition = readFileSync(CLIENT_DEFINITION_PATH, 'utf8');

    expect(vcsClientDefinition)
      .toEqual(generatedClientDefinition);
  });
});
