import { describe, expect, test } from 'vitest';
import { datePickerFormat, elementPlusLocale, setDatePickerLocale } from '../date-picker-localization';

describe('date picker localization', () => {
  test('uses the regional date order', () => {
    setDatePickerLocale('en-US');
    expect(datePickerFormat.value).toBe('MM/DD/YYYY');

    setDatePickerLocale('en-GB');
    expect(datePickerFormat.value).toBe('DD/MM/YYYY');

    setDatePickerLocale('uk');
    expect(datePickerFormat.value).toBe('DD.MM.YYYY');
    expect(elementPlusLocale.value.name).toBe('uk');
  });

  test('uses the formatting locale for calendar labels', () => {
    setDatePickerLocale('en-AU');
    expect(elementPlusLocale.value.name).toBe('en');
    expect(datePickerFormat.value).toBe('DD/MM/YYYY');
  });
});
