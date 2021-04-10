import { apiDateString } from '@/services/api';

describe('apiDateString', () => {
  it('should convert date without zeros', () => {
    expect(apiDateString(new Date('2030-10-13T00:00:00')))
      .toBe('2030-10-13');
  });

  it('should convert date with zeros', () => {
    expect(apiDateString(new Date('2030-02-06T00:00:00')))
      .toBe('2030-02-06');
  });
});
