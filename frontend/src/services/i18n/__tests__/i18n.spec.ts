import {
  afterEach, describe, expect, vi, test,
} from 'vitest';
import {
  formatMessage,
  parseMessage,
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
      value: ['en-GB', 'uk'],
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
      .toBe('en-GB');
  });

  test('should render template without parameters', async () => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('Test message'))
      .toBe('Test message');
  });

  test('should render named params', async () => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('{param}', { param: 'value' }))
      .toBe('value');
  });

  test('should render indexed params', async () => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('{0}', ['value']))
      .toBe('value');
  });

  test.each([
    [true, 'Yes'],
    [false, 'No'],
  ])('should support yesNo format (%b -> %s)', async (value, expectedInterpolation) => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('{0, yesNo}', [value]))
      .toBe(expectedInterpolation);
  });

  test('should support amount format', async () => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('{0, amount, withCurrency}', [{
      currency: 'AUD',
      amountInCents: 1234,
    }]))
      .toBe('A$12.34');
  });

  test.each([
    [0, '0%'],
    [1, '0.01%'],
    [2, '0.02%'],
    [10, '0.1%'],
    [11, '0.11%'],
    [100, '1%'],
    [101, '1.01%'],
    [110, '1.1%'],
    [123, '1.23%'],
    [1230, '12.3%'],
    [1234, '12.34%'],
  ])('should support bps format (%i -> %s)', async (bps, expectedValue) => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('{0, bps, percent}', [bps]))
      .toBe(expectedValue);
  });

  test('should support date format', async () => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('{0, date, medium}', [new Date('2021-10-21 14:23')]))
      .toBe('Oct 21, 2021');
  });

  test('should format API date-only strings as local calendar dates', async () => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('{0, date, medium}', ['3025-01-15']))
      .toBe('Jan 15, 3025');
    expect(formatMessage('{0, date, medium}', ['3025-12-31']))
      .toBe('Dec 31, 3025');
  });

  test('should support date-time format', async () => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('{0, dateTime, medium}', [new Date('3025-01-15T14:23:00Z')]))
      .toBe('Jan 15, 3025, 2:23 PM');
  });

  test.each([
    ['Australia/Melbourne', 'Mar 29, 1999, 9:01 AM', 'Mar 29, 1999'],
    ['America/New_York', 'Mar 28, 1999, 6:01 PM', 'Mar 28, 1999'],
  ])('should format timestamps in profile time zone %s', async (timeZone, expectedTimestamp, expectedDate) => {
    await setLocaleFromProfile('en', 'en', timeZone);

    expect(formatMessage('{0, dateTime, medium}', ['1999-03-28T23:01:00Z']))
      .toBe(expectedTimestamp);
    expect(formatMessage('{0, date, medium}', ['1999-03-28T23:01:00Z']))
      .toBe(expectedDate);
    expect(formatMessage('{0, date, medium}', ['1999-03-28']))
      .toBe('Mar 28, 1999');
  });

  test.each([
    ['en', 'en', 'Jan 15, 3025, 2:23 PM'],
    ['uk', 'uk', '15 січ. 3025, 2:23 пп'],
  ])('should format date-time translations with locale %s and language %s', async (locale, language, expected) => {
    const i18n = await import('@/services/i18n');
    await i18n.setLocaleFromProfile(locale, language, 'UTC');

    expect(i18n.$t.value.common.dateTime.medium(new Date('3025-01-15T14:23:00Z')))
      .toBe(expected);
  });

  test('should format wall times inside the browser daylight-saving gap without shifting them', async () => {
    vi.stubEnv('TZ', 'America/New_York');
    try {
      await setLocaleFromProfile('en', 'en', 'UTC');
      expect(formatMessage('{0, dateTime, medium}', ['1999-04-04T02:30:00Z']))
        .toBe('Apr 4, 1999, 2:30 AM');
    } finally {
      vi.unstubAllEnvs();
    }
  });

  test('should support $t', async () => {
    await setLocaleFromProfile('en', 'en', 'UTC');

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
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(formatMessage('{0, fileSize, pretty}', [value]))
      .toBe(expectedInterpolation);
  });

  test('should parse messages', async () => {
    await setLocaleFromProfile('en', 'en', 'UTC');

    expect(parseMessage('Test then {0} and then done.'))
      .toEqual([{
        type: 'content',
        value: 'Test then ',
      }, {
        type: 'arg',
        value: '0',
      }, {
        type: 'content',
        value: ' and then done.',
      }]);
  });

  afterEach(() => {
    vi.resetModules();
  });
});
