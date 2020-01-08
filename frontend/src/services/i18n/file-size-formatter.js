export default function fileSizeFormatter({ globalize }) {
  const maxOneDigitNumberFormat = globalize.numberFormatter({
    maximumFractionDigits: 1,
  });

  const byteFormatter = globalize.unitFormatter('digital-byte', {
    form: 'short',
    numberFormatter: maxOneDigitNumberFormat,
  });

  const kilobyteFormatter = globalize.unitFormatter('digital-kilobyte', {
    form: 'short',
    numberFormatter: maxOneDigitNumberFormat,
  });

  const megabyteFormatter = globalize.unitFormatter('digital-megabyte', {
    form: 'short',
    numberFormatter: maxOneDigitNumberFormat,
  });

  const KB = 1024;
  const MB = 1024 * 1024;

  return function fileSizeFormatterImpl(sizeInBytes, locale, arg) {
    if (arg !== 'pretty') {
      throw Error(`${arg} is not supported for file size formatter`);
    }

    if (sizeInBytes < KB) {
      return byteFormatter(sizeInBytes);
    }

    if (sizeInBytes < MB) {
      return kilobyteFormatter(sizeInBytes / KB);
    }

    return megabyteFormatter(sizeInBytes / MB);
  };
}
