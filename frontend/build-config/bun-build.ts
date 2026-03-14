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
  publicPath: '/assets/',
  naming: {
    chunk: '[name]-[hash].[ext]',
    entry: '[name]-[hash].[ext]',
    asset: '[name]-[hash].[ext]',
  },
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

// Reorganize output: move non-HTML assets to assets/ subdirectory,
// keep index.html at root (required by Spring Boot SPA setup)
const assetsDir = path.join(outDir, 'assets');
if (!fs.existsSync(assetsDir)) {
  fs.mkdirSync(assetsDir, { recursive: true });
}

const outputFiles = fs.readdirSync(outDir);
for (const file of outputFiles) {
  const filePath = path.join(outDir, file);
  if (fs.statSync(filePath).isDirectory()) continue;

  if (file.endsWith('.html')) {
    // Rename hashed HTML back to index.html
    fs.renameSync(filePath, path.join(outDir, 'index.html'));
  } else {
    // Move all other files to assets/ subdirectory
    fs.renameSync(filePath, path.join(assetsDir, file));
  }
}

// Copy remaining public assets
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

const finalFiles = fs.readdirSync(assetsDir);
console.log(
  `Build completed successfully. ${finalFiles.length} asset files + index.html written to ${outDir}`,
);
