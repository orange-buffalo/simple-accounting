import {
  afterEach,
  beforeEach, describe, expect, vi, test,
} from 'vitest';
import { flatten } from 'flat';
import { I18nService } from '@/services/i18n';

describe('i18n', () => {
  let i18n: I18nService;

  test('should provide supported languages', () => {
    expect(I18nService.getSupportedLanguages())
      .toContainEqual({
        languageCode: 'en',
        displayName: 'English',
      });
  });

  test('should fail on supported locales if not initialized', () => {
    expect(() => i18n.getSupportedLocales())
      .toThrow('i18n has not been initialized');
    expect(() => i18n.getCurrentLanguage())
      .toThrow('i18n has not been initialized');
    expect(() => i18n.getCurrentLocale())
      .toThrow('i18n has not been initialized');
  });

  test('should setup up after initialization from browser', async () => {
    Object.defineProperty(global.navigator, 'languages', {
      value: ['fr', 'en'],
      configurable: true,
    });

    await i18n.setLocaleFromBrowser();

    expect(i18n.getSupportedLocales())
      .toContainEqual({
        locale: 'en',
        displayName: 'English',
      });
    expect(i18n.getCurrentLanguage())
      .toBe('en');
    expect(i18n.getCurrentLocale())
      .toBe('fr');
  });

  test('should fallback to message key if not found in message file', async () => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('unknown.message'))
      .toBe('unknown.message');
  });

  test('should render messages with nested key', async () => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('test.message'))
      .toBe('Test message');
  });

  test('should render named params', async () => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('namedParam', { param: 'value' }))
      .toBe('value');
  });

  test('should render indexed params', async () => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('indexedParam', ['value']))
      .toBe('value');
  });

  test('should support yesNo format', async () => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('yesNoFormatter', [true]))
      .toBe('common.yesNo.yes');
  });

  test('should support amount format', async () => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('amountFormatter', [{
      currency: 'AUD',
      amount: 1234,
    }]))
      .toBe('A$12.34');
  });

  test('should support bps format', async () => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('bpsFormatter', [123]))
      .toBe('1.23%');
  });

  test('should support date format', async () => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('dateFormatter', [new Date('2021-10-21 14:23')]))
      .toBe('Oct 21, 2021');
  });

  test('should support date-time format', async () => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('dateTimeFormatter', [new Date('2021-10-21 14:23')]))
      .toBe('Oct 21, 2021, 2:23 PM');
  });

  test.each([
    [0, '0 byte'],
    [1, '1 byte'],
    [2, '2 byte'],
    [1000, '1,000 byte'],
    [1024, '1 kB'],
    [1150, '1.1 kB'],
    [1050, '1 kB'],
    [1024 * 99, '99 kB'],
    [1024 * 1000, '1,000 kB'],
    [1024 * 1024, '1 MB'],
    [1024 * 1125, '1.1 MB'],
    [1024 * 1025, '1 MB'],
    [1024 * 1024 * 99, '99 MB'],
    [1024 * 1024 * 1000, '1,000 MB'],
    [1024 * 1024 * 2000, '2,000 MB'],
  ])('should support file size format (%i -> %s)', async (value, expectedInterpolation) => {
    await i18n.setLocaleFromProfile('en', 'en');

    expect(i18n.t('fileSizeFormatter', [value]))
      .toBe(expectedInterpolation);
  });

  beforeEach(async () => {
    vi.mock('../t9n/en', () => ({
      default: {
        test: {
          message: 'Test message',
        },
        common: {
          percent: '{0, number, :: percent scale/100 .00}',
        },
        namedParam: '{param}',
        indexedParam: '{0}',
        yesNoFormatter: '{0, yesNo}',
        amountFormatter: '{0, amount, withCurrency}',
        bpsFormatter: '{0, bps, percent}',
        dateFormatter: '{0, date, medium}',
        dateTimeFormatter: '{0, saDateTime, medium}',
        fileSizeFormatter: '{0, fileSize, pretty}',
      },
    }));
    i18n = (await import('@/services/i18n')).i18n;
  });

  afterEach(() => {
    vi.resetModules();
  });
});

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
        .to.eql(bundleKeys);
    }
  });
});
