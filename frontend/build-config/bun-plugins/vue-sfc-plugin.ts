import type { BunPlugin } from 'bun';
import * as sfc from '@vue/compiler-sfc';
import * as sass from 'sass';
import * as path from 'path';
import * as crypto from 'crypto';
import { createSassImporter } from './scss-plugin';

// Register TypeScript for type resolution in SFC macros
sfc.registerTS(() => require('typescript'));

function hashId(filename: string): string {
  return crypto.createHash('md5').update(filename).digest('hex').substring(0, 8);
}

const cssCache = new Map<string, string>();

export function getCssForVuePath(vuePath: string): string | undefined {
  return cssCache.get(vuePath);
}

export const vueSfcPlugin: BunPlugin = {
  name: 'vue-sfc',
  setup(build) {
    build.onResolve({ filter: /\.vue\.bun-vue-css$/ }, (args) => {
      return {
        path: args.path.replace('.bun-vue-css', ''),
        namespace: 'vue-css',
      };
    });

    build.onLoad({ filter: /.*/, namespace: 'vue-css' }, (args) => {
      const css = cssCache.get(args.path) || '';
      return {
        contents: css,
        loader: 'css',
      };
    });

    build.onLoad({ filter: /\.vue$/ }, async (args) => {
      const source = await Bun.file(args.path).text();
      const filename = args.path;
      const shortName = path.relative(process.cwd(), filename);
      const id = hashId(shortName);
      const scopeId = `data-v-${id}`;

      const { descriptor, errors } = sfc.parse(source, { filename: shortName });

      if (errors.length > 0) {
        throw new Error(`Vue SFC parse errors in ${shortName}: ${errors.map((e) => e.message).join('\n')}`);
      }

      const hasScoped = descriptor.styles.some((s) => s.scoped);

      // Compile script
      let scriptCode = '';
      let scriptBindings: Record<string, sfc.BindingTypes> | undefined;
      if (descriptor.script || descriptor.scriptSetup) {
        const scriptResult = sfc.compileScript(descriptor, {
          id,
          isProd: true,
          sourceMap: false,
          fs: {
            fileExists(file: string) {
              return require('fs').existsSync(file);
            },
            readFile(file: string) {
              return require('fs').readFileSync(file, 'utf-8');
            },
          },
        });
        scriptCode = scriptResult.content;
        scriptBindings = scriptResult.bindings;
      }

      // Compile template
      let templateCode = '';
      if (descriptor.template) {
        const templateResult = sfc.compileTemplate({
          source: descriptor.template.content,
          filename: shortName,
          id,
          scoped: hasScoped,
          compilerOptions: {
            scopeId: hasScoped ? scopeId : undefined,
            bindingMetadata: scriptBindings,
          },
          isProd: true,
        });
        if (templateResult.errors.length > 0) {
          throw new Error(
            `Vue template compile errors in ${shortName}: ${templateResult.errors.map((e) => (typeof e === 'string' ? e : e.message)).join('\n')}`,
          );
        }
        templateCode = templateResult.code;
      }

      // Compile styles
      let combinedCss = '';
      for (const style of descriptor.styles) {
        let css = style.content;
        if (style.lang === 'scss' || style.lang === 'sass') {
          const compiled = sass.compileString(css, {
            syntax: style.lang === 'sass' ? 'indented' : 'scss',
            loadPaths: [path.dirname(filename), path.resolve(process.cwd(), 'src'), path.resolve(process.cwd(), 'node_modules')],
            importers: [createSassImporter()],
            quietDeps: true,
          });
          css = compiled.css;
        }
        if (style.scoped) {
          const styleResult = sfc.compileStyle({
            source: css,
            filename: shortName,
            id: scopeId,
            scoped: true,
            isProd: true,
          });
          if (styleResult.errors.length > 0) {
            throw new Error(
              `Vue style compile errors in ${shortName}: ${styleResult.errors.map((e) => e.message).join('\n')}`,
            );
          }
          css = styleResult.code;
        }
        combinedCss += css + '\n';
      }

      // Store CSS for extraction
      let cssImport = '';
      if (combinedCss.trim()) {
        cssCache.set(filename, combinedCss);
        cssImport = `import '${filename}.bun-vue-css';\n`;
      }

      // Process the script code: replace the default export with a variable
      // and attach the render function
      let output = scriptCode;

      // Handle the import from 'vue' that the template render function needs
      // The template code uses functions from vue like createElementVNode, etc.
      if (templateCode) {
        // The template compile output has its own imports from 'vue'
        // and exports a render function
        output += '\n' + templateCode + '\n';

        // Replace `export default` with a variable assignment
        // Handle both `export default /*@__PURE__*/_defineComponent({` and `export default {` patterns
        output = output.replace(
          /export default\s*(?:\/\*@__PURE__\*\/\s*)?(_defineComponent\()?/,
          'const __sfc_component__ = $1',
        );

        output += `\n__sfc_component__.render = render;\n`;

        if (hasScoped) {
          output += `__sfc_component__.__scopeId = '${scopeId}';\n`;
        }
        output += `__sfc_component__.__file = '${shortName}';\n`;
        output += `export default __sfc_component__;\n`;
      }

      return {
        contents: cssImport + output,
        loader: 'ts',
      };
    });
  },
};
