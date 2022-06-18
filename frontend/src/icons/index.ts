interface Index {
  [key: string]: string;
}

// when using 'component', storybook builder generates assets for svgs instead of embedding them in js
// it only happens for publishing, in dev mode it works as expected
// this might be a bug in vite-builder
// if ever resolved, we can remove build-svgs run script and move svgo.config.js into vite-plugins.ts
const rawIcons = import.meta.globEager('./svg/*.svg', { as: 'raw' });
const icons: Index = {};

for (const path in rawIcons) {
  if (Object.prototype.hasOwnProperty.call(rawIcons, path)) {
    const iconSvg = rawIcons[path] as unknown as string;
    const iconName = path.replace('./svg/', '')
      .replace('.svg', '');
    icons[iconName] = iconSvg;
  }
}

export function iconByName(iconName: string): string {
  const icon = icons[iconName];
  if (icon == null) {
    throw new Error(`${iconName} is not known`);
  }
  return icon;
}

export function iconNames(): string[] {
  return Object.getOwnPropertyNames(icons);
}
