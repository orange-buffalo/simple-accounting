import type { BunPlugin } from 'bun';
import * as fs from 'fs';
import * as path from 'path';

export const globImportPlugin: BunPlugin = {
  name: 'glob-import',
  setup(build) {
    build.onLoad({ filter: /icons[\\/]index\.ts$/ }, async (args) => {
      const dir = path.dirname(args.path);
      const svgDir = path.join(dir, 'svg');
      const files = fs.readdirSync(svgDir)
        .filter((f) => f.endsWith('.svg'))
        .sort();

      const imports = files.map(
        (f, i) => `import icon_${i} from './svg/${f}?component';`,
      ).join('\n');

      const entries = files.map((f, i) => {
        const name = f.replace('.svg', '');
        return `icons['${name}'] = icon_${i};`;
      }).join('\n');

      const contents = `import type { FunctionalComponent, SVGAttributes } from 'vue';

interface Index {
  [key: string]: FunctionalComponent<SVGAttributes>;
}

${imports}

const icons: Index = {};
${entries}

export function iconByName(iconName: string): FunctionalComponent<SVGAttributes> {
  const icon = icons[iconName];
  if (icon == null) {
    throw new Error(\`\${iconName} is not known\`);
  }
  return icon;
}

export function iconNames(): string[] {
  return Object.getOwnPropertyNames(icons);
}
`;
      return {
        contents,
        loader: 'ts',
      };
    });
  },
};
