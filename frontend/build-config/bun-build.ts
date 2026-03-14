import * as fs from 'fs';
import * as path from 'path';
import { vueSfcPlugin } from './bun-plugins/vue-sfc-plugin';
import { scssPlugin } from './bun-plugins/scss-plugin';
import { svgComponentPlugin } from './bun-plugins/svg-component-plugin';
import { globImportPlugin } from './bun-plugins/glob-import-plugin';
import { rawImportPlugin } from './bun-plugins/raw-import-plugin';

const outDir = './dist/META-INF/resources';

// Clean output directory
if (fs.existsSync('./dist')) {
  fs.rmSync('./dist', { recursive: true });
}

console.log('Building frontend with Bun bundler...');

const result = await Bun.build({
  entrypoints: ['./index.html'],
  outdir: outDir,
  target: 'browser',
  sourcemap: 'inline',
  splitting: true,
  publicPath: '/',
  plugins: [
    rawImportPlugin,
    globImportPlugin,
    svgComponentPlugin,
    vueSfcPlugin,
    scssPlugin,
  ],
  define: {
    'process.env.NODE_ENV': '"production"',
  },
});

if (!result.success) {
  console.error('Build failed:');
  for (const log of result.logs) {
    console.error(log);
  }
  process.exit(1);
}

// Copy remaining public assets (Bun processes referenced ones, but we copy extras)
const publicDir = './public';
if (fs.existsSync(publicDir)) {
  const publicFiles = fs.readdirSync(publicDir);
  for (const file of publicFiles) {
    const dest = path.join(outDir, file);
    if (!fs.existsSync(dest)) {
      fs.copyFileSync(path.join(publicDir, file), dest);
    }
  }
}

const outputFiles = result.outputs.map((o) => `  ${path.relative(outDir, o.path)} (${(o.size / 1024).toFixed(1)} KB)`);
console.log(`Build completed successfully. ${result.outputs.length} files written to ${outDir}:`);
for (const line of outputFiles.slice(0, 10)) {
  console.log(line);
}
if (outputFiles.length > 10) {
  console.log(`  ... and ${outputFiles.length - 10} more files`);
}
