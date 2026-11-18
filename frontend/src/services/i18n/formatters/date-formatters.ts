import { getCldr } from '@/services/i18n/cldr-data';
import { timestampToUtcCalendarDate } from '@/services/time-zone';

function createFormatter(mediumFormatter: (date: Date) => string, dateOnly: boolean) {
  return function dateFormatterImp(dateInput: unknown, locale: string, arg: unknown) {
    if (dateInput == null) {
      return null;
    }

    if (arg !== 'medium') {
      throw Error(`${arg} is not supported for date formatters`);
    }

    let date;
    const dateOnlyString = typeof dateInput === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(dateInput);
    const calendarOnly = dateOnly && (dateInput instanceof Date || dateOnlyString);
    if (typeof dateInput === 'string') {
      if (dateOnly && /^\d{4}-\d{2}-\d{2}$/.test(dateInput)) {
        const [year, month, day] = dateInput.split('-').map(Number);
        date = new Date(Date.UTC(year, month - 1, day));
      } else {
        date = new Date(dateInput);
      }
    } else {
      date = dateInput as Date;
    }

    if (calendarOnly && dateInput instanceof Date) {
      date = new Date(Date.UTC(dateInput.getFullYear(), dateInput.getMonth(), dateInput.getDate()));
    }
    return mediumFormatter(calendarOnly ? date : timestampToUtcCalendarDate(date));
  };
}

export function dateTimeFormatter() {
  const mediumFormatter = getCldr()
    .dateFormatter({ skeleton: 'yMMMdhm', timeZone: 'UTC' });
  return createFormatter(mediumFormatter, false);
}

export function dateFormatter() {
  const mediumFormatter = getCldr()
    .dateFormatter({ date: 'medium', timeZone: 'UTC' });
  return createFormatter(mediumFormatter, true);
}
