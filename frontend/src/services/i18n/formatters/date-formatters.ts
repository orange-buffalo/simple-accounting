import { getCldr } from '@/services/i18n/cldr-data';

function createFormatter(mediumFormatter: (date: Date) => string, dateOnly: boolean) {
  return function dateFormatterImp(dateInput: unknown, locale: string, arg: unknown) {
    if (dateInput == null) {
      return null;
    }

    if (arg !== 'medium') {
      throw Error(`${arg} is not supported for date formatters`);
    }

    let date;
    if (typeof dateInput === 'string') {
      if (dateOnly && /^\d{4}-\d{2}-\d{2}$/.test(dateInput)) {
        const [year, month, day] = dateInput.split('-').map(Number);
        date = new Date(year, month - 1, day);
      } else {
        date = new Date(dateInput);
      }
    } else {
      date = dateInput as Date;
    }

    return mediumFormatter(date);
  };
}

export function dateTimeFormatter() {
  const mediumFormatter = getCldr()
    .dateFormatter({ skeleton: 'yMMMdhm' });
  return createFormatter(mediumFormatter, false);
}

export function dateFormatter() {
  const mediumFormatter = getCldr()
    .dateFormatter({ date: 'medium' });
  return createFormatter(mediumFormatter, true);
}
