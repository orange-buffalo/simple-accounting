import type { BunPlugin } from 'bun';
import { optimize } from 'svgo';
import { compile } from '@vue/compiler-dom';
import * as pathModule from 'path';

const srcDir = pathModule.resolve(process.cwd(), 'src');

export const svgComponentPlugin: BunPlugin = {
  name: 'svg-component',
  setup(build) {
    build.onResolve({ filter: /\.svg\?component$/ }, (args) => {
      const svgPath = args.path.replace('?component', '');
      let resolvedPath: string;
      if (svgPath.startsWith('@/')) {
        resolvedPath = pathModule.resolve(srcDir, svgPath.substring(2));
      } else if (svgPath.startsWith('.')) {
        resolvedPath = pathModule.resolve(pathModule.dirname(args.importer), svgPath);
      } else {
        resolvedPath = svgPath;
      }
      return {
        path: resolvedPath,
        namespace: 'svg-component',
      };
    });

    build.onLoad({ filter: /.*/, namespace: 'svg-component' }, async (args) => {
      const svgContent = await Bun.file(args.path).text();

      const optimized = optimize(svgContent, {
        plugins: [
          {
            name: 'preset-default',
            params: {
              overrides: {
                removeViewBox: false,
              },
            },
          },
          'removeDimensions',
          'inlineStyles',
          {
            name: 'removeStyleElement',
            fn: () => ({
              element: {
                enter: (node: any, parentNode: any) => {
                  if (node.name === 'style' && parentNode.type === 'element') {
                    parentNode.children = parentNode.children.filter((c: any) => c !== node);
                  }
                },
              },
            }),
          },
        ],
      });

      const { code } = compile(optimized.data, {
        mode: 'module',
        onWarn() {},
        onError() {},
      });

      // The compile output exports a named `render` function.
      // We need to wrap it as a default export component for use as a Vue component.
      const wrappedCode = code + '\nexport default { render };\n';

      return {
        contents: wrappedCode,
        loader: 'js',
      };
    });
  },
};
