import deepKeys from 'deep-keys';
import i18n from '@/services/i18n';

describe('i18n', () => {
  it('should provide at least one language', () => {
    expect(i18n.getSupportedLanguages().length)
      .toBeGreaterThanOrEqual(1);
  });

  it('should provide same keys for all languages', async () => {
    const bundles = await Promise.all(i18n.getSupportedLanguages()
      .map((language) => import(`@/i18n/t9n/${language.languageCode}`)
        .then((module) => ({
          messages: module.default,
          language: language.languageCode,
        }))));

    const bundle = bundles[0];
    const bundleKeys = deepKeys(bundle.messages);
    for (let i = 1; i < bundles.length; i += 1) {
      const otherBundle = bundles[i];
      const otherBundleKeys = deepKeys(otherBundle.messages);
      expect(otherBundleKeys, `Inconsistent keys for ${bundle.language} / ${otherBundle.language}`)
        .toIncludeSameMembers(bundleKeys);
    }
  });
});
