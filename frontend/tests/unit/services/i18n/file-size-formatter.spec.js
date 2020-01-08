describe('file size formatter', () => {
  let i18n;

  function assertFormatting(valueInBytes, expectedText) {
    expect(i18n.t('fileSize', [valueInBytes]))
      .toBe(expectedText);
  }

  beforeAll(() => {
    jest.mock('@/i18n/t9n/en.js', () => ({
      fileSize: '{0, fileSize, pretty}',
    }));

    i18n = require('@/services/i18n').default;

    return i18n.setLocaleFromProfile({
      language: 'en',
      locale: 'en_AU',
    });
  });

  it('should support 0 byte', () => assertFormatting(0, '0 byte'));
  it('should support 1 byte', () => assertFormatting(1, '1 byte'));
  it('should support 2 bytes', () => assertFormatting(2, '2 byte'));
  it('should support 1000 bytes', () => assertFormatting(1000, '1,000 byte'));
  it('should support 1 kB', () => assertFormatting(1024, '1 kB'));
  it('should round kilobyte to one digit after decimal separator', () => assertFormatting(1150, '1.1 kB'));
  it('should trim trailing zeros after rounding kilobyte', () => assertFormatting(1050, '1 kB'));
  it('should support 99 kilobyte', () => assertFormatting(1024 * 99, '99 kB'));
  it('should support 1000 kilobyte', () => assertFormatting(1024 * 1000, '1,000 kB'));
  it('should support 1 megabyte', () => assertFormatting(1024 * 1024, '1 MB'));
  it('should round megabyte to one digit after decimal separator', () => assertFormatting(1024 * 1125, '1.1 MB'));
  it('should trim trailing zeros after rounding megabyte', () => assertFormatting(1024 * 1025, '1 MB'));
  it('should support 99 megabyte', () => assertFormatting(1024 * 1024 * 99, '99 MB'));
  it('should support 1000 megabyte', () => assertFormatting(1024 * 1024 * 1000, '1,000 MB'));
  it('should support 2000 megabyte', () => assertFormatting(1024 * 1024 * 2000, '2,000 MB'));
});
