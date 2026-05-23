import {
  describe, test, expect,
} from 'vitest';
import 'whatwg-fetch';
import {
  apiDateString,
} from '@/services/api';

describe('apiDateString', () => {
  test('should convert date without zeros', () => {
    expect(apiDateString(new Date('2030-10-13T00:00:00')))
      .toBe('2030-10-13');
  });

  test('should convert date with zeros', () => {
    expect(apiDateString(new Date('2030-02-06T00:00:00')))
      .toBe('2030-02-06');
  });
});
