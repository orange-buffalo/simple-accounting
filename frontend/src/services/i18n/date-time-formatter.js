export default function dateTimeFormatter({ globalize }) {
  const mediumFormatter = globalize.dateFormatter({ skeleton: 'yMMMdhm' });

  return function dateTimeFormatterImp(dateTimeInput, locale, arg) {
    if (dateTimeInput == null) {
      return null;
    }

    if (arg !== 'medium') {
      throw Error(`${arg} is not supported for date time formatter`);
    }

    let dateTime;
    if (typeof dateTimeInput === 'string') {
      dateTime = new Date(dateTimeInput);
    } else {
      dateTime = dateTimeInput;
    }

    return mediumFormatter(dateTime);
  };
}
