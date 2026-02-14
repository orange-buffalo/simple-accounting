import { getCldr } from '@/services/i18n/cldr-data';

function createFormatter(mediumFormatter: (date: Date) => string) {
  return function dateFormatterImp(dateInput: unknown, locale: string, arg: unknown) {
    if (dateInput == null) {
      return null;
    }

    if (arg !== 'medium') {
      throw Error(`${arg} is not supported for date formatters`);
    }

    let date: Date;
    if (typeof dateInput === 'string') {
      date = new Date(dateInput);
    } else {
      date = dateInput as Date;
    }

    return mediumFormatter(date);
  };
}

export function dateTimeFormatter() {
  const mediumFormatter = getCldr().dateFormatter({ skeleton: 'yMMMdhm' });
  return createFormatter(mediumFormatter);
}

export function dateFormatter() {
  const mediumFormatter = getCldr().dateFormatter({ date: 'medium' });
  return createFormatter(mediumFormatter);
}
