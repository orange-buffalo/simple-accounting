import type { BunPlugin } from 'bun';
import * as sass from 'sass';
import * as path from 'path';

const srcDir = path.resolve(process.cwd(), 'src');

function createSassImporter(): sass.FileImporter<'sync'> {
  return {
    findFileUrl(url: string) {
      if (url.startsWith('@/')) {
        const resolved = path.resolve(srcDir, url.substring(2));
        return new URL(`file://${resolved}`);
      }
      return null;
    },
  };
}

export const scssPlugin: BunPlugin = {
  name: 'scss-loader',
  setup(build) {
    build.onLoad({ filter: /\.scss$/ }, async (args) => {
      const result = sass.compile(args.path, {
        loadPaths: [
          path.dirname(args.path),
          srcDir,
          path.resolve(process.cwd(), 'node_modules'),
        ],
        importers: [createSassImporter()],
        quietDeps: true,
      });
      return {
        contents: result.css,
        loader: 'css',
      };
    });
  },
};

export { createSassImporter };
