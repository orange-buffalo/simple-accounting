import { generate } from '@graphql-codegen/cli';
import { readFileSync, readdirSync } from 'fs';
import { join } from 'path';

import codegenConfig from '../codegen.ts';

const gqlDir = 'src/services/api/gql';
const iterations = 100;

function readGqlFiles() {
  return Object.fromEntries(
    readdirSync(gqlDir)
      .sort()
      .map((name) => [name, readFileSync(join(gqlDir, name), 'utf8')]),
  );
}

async function runCodegen() {
  return generate({ ...codegenConfig, silent: true }, false);
}

function toContentMap(generationResult) {
  return Object.fromEntries(
    generationResult.map((f) => [f.filename.split('/').pop(), f.content ?? '']),
  );
}

const committed = readGqlFiles();

const baselineResult = await runCodegen();
const baseline = toContentMap(baselineResult);

const committedFileNames = Object.keys(committed).sort().join(',');
const baselineFileNames = Object.keys(baseline).sort().join(',');
if (committedFileNames !== baselineFileNames) {
  throw new Error(
    `Generated file list changed. Committed: [${committedFileNames}], Generated: [${baselineFileNames}].` +
      ` Run 'bun run graphql-codegen' and commit the result.`,
  );
}

for (const [name, content] of Object.entries(committed)) {
  if (baseline[name] !== content) {
    throw new Error(
      `Generated file '${name}' differs from committed version. Run 'bun run graphql-codegen' and commit the result.`,
    );
  }
}

for (let i = 2; i <= iterations; i++) {
  const current = toContentMap(await runCodegen());
  for (const [name, content] of Object.entries(baseline)) {
    if (current[name] !== content) {
      throw new Error(
        `Generator is not stable: file '${name}' changed between iterations 1 and ${i}.`,
      );
    }
  }
}

console.log(`✓ GQL types are up to date and stable (verified ${iterations} iterations).`);
