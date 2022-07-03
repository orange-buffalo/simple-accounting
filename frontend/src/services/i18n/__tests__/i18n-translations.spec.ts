import {
  describe, expect, test,
} from 'vitest';
import { flatten } from 'flat';
import { I18nService } from '@/services/i18n';

describe('translation files', () => {
  test('should provide same keys for all languages', async () => {
    const bundles = await Promise.all(I18nService.getSupportedLanguages()
      .map((language) => import(`@/services/i18n/t9n/${language.languageCode}`)
        .then((module) => ({
          messages: module.default,
          language: language.languageCode,
        }))));

    const bundle = bundles[0];
    const bundleKeys = Object.getOwnPropertyNames(flatten(bundle.messages));
    for (let i = 1; i < bundles.length; i += 1) {
      const otherBundle = bundles[i];
      const otherBundleKeys = Object.getOwnPropertyNames(flatten(otherBundle.messages));
      expect(otherBundleKeys, `Inconsistent keys for ${bundle.language} / ${otherBundle.language}`)
        .to
        .eql(bundleKeys);
    }
  });
});
