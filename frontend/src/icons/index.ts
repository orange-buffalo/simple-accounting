import type { FunctionalComponent, SVGAttributes } from 'vue';

/**
 * Icons are stored as `<name>-<variant>.svg`:
 * - `simple` icons are used on the pages, where they are rendered small (13-20px);
 * - `detailed` icons are used where they are rendered large (navigation menu, dashboard cards,
 *   document panels). They are optional - when an icon has no detailed variant, the simple one is used.
 */
export type SaIconVariant = 'simple' | 'detailed';

interface Index {
  [key: string]: FunctionalComponent<SVGAttributes>;
}

const iconComponents = import.meta.glob<FunctionalComponent<SVGAttributes>>('./svg/*.svg', {
  eager: true,
  query: '?component',
});

const icons: Record<SaIconVariant, Index> = {
  simple: {},
  detailed: {},
};

for (const path in iconComponents) {
  if (Object.prototype.hasOwnProperty.call(iconComponents, path)) {
    const fileName = path.replace('./svg/', '')
      .replace('.svg', '');
    const separatorIndex = fileName.lastIndexOf('-');
    const iconName = fileName.substring(0, separatorIndex);
    const variant = fileName.substring(separatorIndex + 1) as SaIconVariant;
    if (icons[variant] === undefined) {
      throw new Error(`${fileName} does not follow the <name>-<variant> convention`);
    }
    icons[variant][iconName] = iconComponents[path];
  }
}

export function iconByName(iconName: string, variant: SaIconVariant = 'simple'): FunctionalComponent<SVGAttributes> {
  const icon = (variant === 'detailed' ? icons.detailed[iconName] : undefined) ?? icons.simple[iconName];
  if (icon == null) {
    throw new Error(`${iconName} is not known`);
  }
  return icon;
}

export function iconNames(): string[] {
  return Object.getOwnPropertyNames(icons.simple);
}

export function detailedIconNames(): string[] {
  return Object.getOwnPropertyNames(icons.detailed);
}
