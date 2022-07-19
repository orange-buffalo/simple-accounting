import {
  afterEach, describe, expect, vi, test,
} from 'vitest';
import {
  formatMessage,
  getSupportedLanguages,
  getSupportedLocales,
  getCurrentLanguage,
  getCurrentLocale,
  setLocaleFromProfile,
  setLocaleFromBrowser,
  $t,
} from '@/services/i18n';

describe('i18n', () => {
  test('should provide supported languages', () => {
    expect(getSupportedLanguages())
      .toContainEqual({
        languageCode: 'en',
        displayName: 'English',
      });
  });

  test('should fail on supported locales if not initialized', () => {
    expect(() => getSupportedLocales())
      .toThrow('i18n has not been initialized');
    expect(() => getCurrentLanguage())
      .toThrow('i18n has not been initialized');
    expect(() => getCurrentLocale())
      .toThrow('i18n has not been initialized');
  });

  test('should setup up after initialization from browser', async () => {
    Object.defineProperty(global.navigator, 'languages', {
      value: ['fr', 'en'],
      configurable: true,
    });

    await setLocaleFromBrowser();

    expect(getSupportedLocales())
      .toContainEqual({
        locale: 'en',
        displayName: 'English',
      });
    expect(getCurrentLanguage())
      .toBe('en');
    expect(getCurrentLocale())
      .toBe('fr');
  });

  test('should render template without parameters', async () => {
    await setLocaleFromProfile('en', 'en');

    expect(formatMessage('Test message'))
      .toBe('Test message');
  });

  test('should render named params', async () => {
    await setLocaleFromProfile('en', 'en');

    expect(formatMessage('{param}', { param: 'value' }))
      .toBe('value');
  });

  test('should render indexed params', async () => {
    await setLocaleFromProfile('en', 'en');

    expect(formatMessage('{0}', ['value']))
      .toBe('value');
  });

  test.each([
    [true, 'Yes'],
    [false, 'No'],
  ])('should support yesNo format (%b -> %s)', async (value, expectedInterpolation) => {
    await setLocaleFromProfile('en', 'en');

    expect(formatMessage('{0, yesNo}', [value]))
      .toBe(expectedInterpolation);
  });

  test('should support amount format', async () => {
    await setLocaleFromProfile('en', 'en');

    expect(formatMessage('{0, amount, withCurrency}', [{
      currency: 'AUD',
      amount: 1234,
    }]))
      .toBe('A$12.34');
  });

  test('should support bps format', async () => {
    await setLocaleFromProfile('en', 'en');

    expect(formatMessage('{0, bps, percent}', [123]))
      .toBe('1.23%');
  });

  test('should support date format', async () => {
    await setLocaleFromProfile('en', 'en');

    expect(formatMessage('{0, date, medium}', [new Date('2021-10-21 14:23')]))
      .toBe('Oct 21, 2021');
  });

  test('should support date-time format', async () => {
    await setLocaleFromProfile('en', 'en');

    expect(formatMessage('{0, saDateTime, medium}', [new Date('2021-10-21 14:23')]))
      .toBe('Oct 21, 2021, 2:23 PM');
  });

  test('should support $t', async () => {
    await setLocaleFromProfile('en', 'en');

    expect($t.value.common.yesNo.yes())
      .toBe('Yes');
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
    await setLocaleFromProfile('en', 'en');

    expect(formatMessage('{0, fileSize, pretty}', [value]))
      .toBe(expectedInterpolation);
  });

  afterEach(() => {
    vi.resetModules();
  });
});
