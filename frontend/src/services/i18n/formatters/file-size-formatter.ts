import { getCldr } from '@/services/i18n/cldr-data';

export function fileSizeFormatter() {
  const byteFormatter = getCldr().unitFormatter('digital-byte', {
    form: 'short',
    // @ts-ignore
    numberFormatter: getCldr().numberFormatter({
      maximumFractionDigits: 1,
    }),
  });

  const kilobyteFormatter = getCldr().unitFormatter('digital-kilobyte', {
    form: 'short',
    // @ts-ignore
    numberFormatter: getCldr().numberFormatter({
      maximumFractionDigits: 1,
    }),
  });

  const megabyteFormatter = getCldr().unitFormatter('digital-megabyte', {
    form: 'short',
    // @ts-ignore
    numberFormatter: getCldr().numberFormatter({
      maximumFractionDigits: 1,
    }),
  });

  const KB = 1024;
  const MB = 1024 * 1024;

  return function fileSizeFormatterImpl(value: unknown, locale: string, arg: string | null) {
    if (arg !== 'pretty') {
      throw Error(`${arg} is not supported for file size formatter`);
    }
    const sizeInBytes = value as number;

    if (sizeInBytes < KB) {
      return byteFormatter(sizeInBytes);
    }

    if (sizeInBytes < MB) {
      return kilobyteFormatter(sizeInBytes / KB);
    }

    return megabyteFormatter(sizeInBytes / MB);
  };
}
