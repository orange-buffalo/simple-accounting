import { flatten } from 'flat';
import { describe, expect, test } from 'vitest';
import { getSupportedLanguages } from '@/services/i18n';

describe('translation files', () => {
  test('should provide same keys and shape for all languages', async () => {
    const bundles = await Promise.all(
      getSupportedLanguages().map((language) =>
        import(`@/services/i18n/t9n/${language.languageCode}.ts`).then((module) => ({
          messages: module.default,
          language: language.languageCode,
        })),
      ),
    );

    const zeroBundle = bundles[0];
    const zeroBundleMessages: Record<string, unknown> = flatten(zeroBundle.messages);
    const zeroBundleKeys = Object.getOwnPropertyNames(zeroBundleMessages);
    for (let i = 1; i < bundles.length; i += 1) {
      const otherBundle = bundles[i];
      const otherBundleMessages: Record<string, unknown> = flatten(otherBundle.messages);
      const otherBundleKeys = Object.getOwnPropertyNames(otherBundleMessages);
      expect(otherBundleKeys, `Inconsistent keys for ${zeroBundle.language} / ${otherBundle.language}`).to.eql(
        zeroBundleKeys,
      );

      for (const bundleKey of zeroBundleKeys) {
        const zeroBundleValue = zeroBundleMessages[bundleKey];
        expect(zeroBundleValue).to.be.a(
          'function',
          `All values in message bundle ${zeroBundle.language} should be functions`,
        );
        const argumentsLength = (zeroBundleValue as Function).length;

        const otherBundleValue = otherBundleMessages[bundleKey];
        expect(otherBundleValue)
          .to.be.a('function', `All values in message bundle ${otherBundle.language} should be functions`)
          .to.have.property('length', argumentsLength, `${otherBundle.language} / ${bundleKey} should be consistent`);
      }
    }
  });
});
