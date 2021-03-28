import { generateTypesForDocument } from 'openapi-client-axios-typegen';
import { readFileSync } from 'fs';

describe('API Spec', () => {
  it('stored in sources should be up-to-date', () => {
    const yaml = require('js-yaml');
    const currentApiSpec = JSON.stringify(
      yaml.load(readFileSync('../backend/src/test/resources/api-spec.yaml', 'utf8')),
    );

    const vcsApiSpec = readFileSync('src/services/api/api-spec.json', 'utf8');
    expect(vcsApiSpec).toEqual(currentApiSpec);
  });
});

describe('Generated API Client', () => {
  it('should be up-to-date', async () => {
    const opts = {
      transformOperationName: (operation: string) => operation,
    };
    const [imports, schemaTypes, operationTypings] = await generateTypesForDocument(
      '../backend/src/test/resources/api-spec.yaml', opts,
    );
    const generatedClientDefinition = `${imports}
          \n${schemaTypes.replace(/\s+$/gm, '')}
          \n${operationTypings.replace(/\s+$/gm, '')}\n`
      .replace('declare namespace', 'export namespace')
      .replace('  namespace', '  export namespace');
    const vcsClientDefinition = readFileSync('src/services/api/api-client-definition.ts', 'utf8');
    expect(vcsClientDefinition).toEqual(generatedClientDefinition);
  });
});
