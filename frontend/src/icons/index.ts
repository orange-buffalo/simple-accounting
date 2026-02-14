import type { FunctionalComponent, SVGAttributes } from 'vue';

interface Index {
  [key: string]: FunctionalComponent<SVGAttributes>;
}

const iconComponents = import.meta.glob<FunctionalComponent<SVGAttributes>>('./svg/*.svg', {
  eager: true,
  query: '?component',
});
const icons: Index = {};

for (const path in iconComponents) {
  if (Object.hasOwn(iconComponents, path)) {
    const iconComponent = iconComponents[path];
    const iconName = path.replace('./svg/', '').replace('.svg', '');
    icons[iconName] = iconComponent;
  }
}

export function iconByName(iconName: string): FunctionalComponent<SVGAttributes> {
  const icon = icons[iconName];
  if (icon == null) {
    throw new Error(`${iconName} is not known`);
  }
  return icon;
}

export function iconNames(): string[] {
  return Object.getOwnPropertyNames(icons);
}
