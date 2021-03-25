describe('API Spec', () => {
  beforeEach(() => {
  });

  it('stored in sources should be up-to-date', () => {
    const { readFileSync } = require('fs');

    const yaml = require('js-yaml');
    const openApiSpec = yaml.load(readFileSync('../backend/src/test/resources/api-spec.yaml', 'utf8'));

    const swaggerToTs = require('openapi-typescript').default;
    const generatedSchema = swaggerToTs(openApiSpec);

    const vcsSchema = readFileSync('src/services/api-spec.ts', 'utf8');

    expect(vcsSchema).toEqual(generatedSchema);
  });
});
