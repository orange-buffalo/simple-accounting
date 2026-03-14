import type { BunPlugin } from 'bun';
import * as path from 'path';

export const rawImportPlugin: BunPlugin = {
  name: 'raw-import',
  setup(build) {
    build.onResolve({ filter: /\?raw$/ }, (args) => {
      const filePath = args.path.replace('?raw', '');
      let resolvedPath: string;

      if (filePath.startsWith('@/')) {
        resolvedPath = path.resolve(process.cwd(), 'src', filePath.substring(2));
      } else if (filePath.startsWith('.')) {
        resolvedPath = path.resolve(path.dirname(args.importer), filePath);
      } else {
        resolvedPath = filePath;
      }

      return {
        path: resolvedPath,
        namespace: 'raw-import',
      };
    });

    build.onLoad({ filter: /.*/, namespace: 'raw-import' }, async (args) => {
      const content = await Bun.file(args.path).text();
      return {
        contents: `export default ${JSON.stringify(content)};`,
        loader: 'js',
      };
    });
  },
};
