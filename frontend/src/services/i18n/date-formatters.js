function createFormatter({ mediumFormatter }) {
  return function dateFormatterImp(dateInput, locale, arg) {
    if (dateInput == null) {
      return null;
    }

    if (arg !== 'medium') {
      throw Error(`${arg} is not supported for date formatters`);
    }

    let date;
    if (typeof dateInput === 'string') {
      date = new Date(dateInput);
    } else {
      date = dateInput;
    }

    return mediumFormatter(date);
  };
}

export function dateTimeFormatter({ globalize }) {
  const mediumFormatter = globalize.dateFormatter({ skeleton: 'yMMMdhm' });
  return createFormatter({ mediumFormatter });
}

export function dateFormatter({ globalize }) {
  const mediumFormatter = globalize.dateFormatter({ date: 'medium' });
  return createFormatter({ mediumFormatter });
}
