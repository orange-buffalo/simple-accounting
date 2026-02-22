import { generate } from '@graphql-codegen/cli';
import { readFileSync, readdirSync } from 'fs';
import { join } from 'path';

import codegenConfig from '../codegen.ts';

const gqlDir = 'src/services/api/gql';

function readGqlFiles() {
  return Object.fromEntries(
    readdirSync(gqlDir)
      .sort()
      .map((name) => [name, readFileSync(join(gqlDir, name), 'utf8')]),
  );
}

const committed = readGqlFiles();

const generationResult = await generate({ ...codegenConfig, silent: true }, false);
const generated = Object.fromEntries(
  generationResult.map((f) => [f.filename.split('/').pop(), f.content ?? '']),
);

const committedFileNames = Object.keys(committed).sort().join(',');
const generatedFileNames = Object.keys(generated).sort().join(',');
if (committedFileNames !== generatedFileNames) {
  throw new Error(
    `Generated file list changed. Committed: [${committedFileNames}], Generated: [${generatedFileNames}].` +
      ` Run 'bun run graphql-codegen' and commit the result.`,
  );
}

for (const [name, content] of Object.entries(committed)) {
  if (generated[name] !== content) {
    throw new Error(
      `Generated file '${name}' differs from committed version. Run 'bun run graphql-codegen' and commit the result.`,
    );
  }
}

console.log('✓ GQL types are up to date.');
