export type Translations = typeof import('./en').default;
type Language = string;
type TranslationsDeferred = Record<Language, () => Promise<Translations>>;
const translationDeferred: TranslationsDeferred = {};

const asyncModules = import.meta.glob<Translations>(['./*.ts', '!./formatter.ts'], { import: 'default' });
for (const path in asyncModules) {
  if (Object.hasOwn(asyncModules, path)) {
    const language: Language = path.replace('./', '').replace('.ts', '');
    translationDeferred[language] = asyncModules[path];
  }
}

export default translationDeferred;
