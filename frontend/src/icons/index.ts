import type { DefineComponent } from 'vue';

interface Index {
  [key: string]: DefineComponent;
}

const iconModules = import.meta.globEager('./svg/*.svg');
const icons: Index = {};
for (const path in iconModules) {
  if (Object.prototype.hasOwnProperty.call(iconModules, path)) {
    const iconComponent = iconModules[path].default;
    const iconName = path.replace('./svg/', '')
      .replace('.svg', '');
    icons[iconName] = iconComponent;
  }
}

export function iconByName(iconName: string): DefineComponent {
  const icon = icons[iconName];
  if (icon == null) {
    throw new Error(`${iconName} is not known`);
  }
  return icon;
}

export function iconNames(): string[] {
  return Object.getOwnPropertyNames(icons);
}
