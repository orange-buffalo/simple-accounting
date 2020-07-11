export default {
  common: {
    date: {
      medium: '{0, date, medium}',
    },
    dateTime: {
      medium: '{0, saDateTime, medium}',
    },
    amount: {
      withCurrency: '{0, amount, withCurrency}',
    },
    yesNo: {
      yes: 'Так',
      no: 'Ні',
    },
    percent: '{0, number, percent}',
  },

  loginPage: {
    announcement: 'Вперше тут? Ми запускаємо публічний доступ скоро.',
    userName: {
      placeholder: 'Логін',
    },
    password: {
      placeholder: 'Пароль',
    },
    rememberMe: {
      label: 'Запам\'ятати мене на 30 днів',
    },
    login: 'Ввійти',
    loginError: {
      underAttack: 'Схоже, що ваш акаунт знаходиться під атакою!',
      generalFailure: 'Спроба входу не вдалася. Будь ласка, переконайтеся, що логін і пароль правильний',
      accountLocked: 'Обліковий запис тимчасово заблоковано. Він буде розблокований через {0, duration}',
    },
  },

  navigationMenu: {
    dashboard: 'Кокпіт',
    expenses: 'Витрати',
    incomes: 'Доходи',
    invoices: 'Рахунки-фактури',
    taxPayments: 'Податок на прибуток',
    reporting: 'Репорти',
    settings: {
      header: 'Параметри',
      customers: 'Клієнти',
      categories: 'Категорії',
      generalTaxes: 'Загальні податки',
      workspaces: 'Проекти',
    },
    user: {
      header: 'Користувач',
      profile: 'Мій Профіль',
      logout: 'Вихід',
    },
  },

  saDocumentDownloadLink: {
    label: 'Завантажити',
    creatingLinkMessage: 'Починаємо збереження файлу...',
  },

  saDocumentUpload: {
    fileSelector: {
      message: 'Перетягніть сюди файл або натисніть, щоб завантажити',
      hint: 'Підтримуються файли до {0, fileSize, pretty}',
    },
    uploadStatusMessage: {
      error: 'Завантаження не вдалося, будь ласка спробуйте знову',
      uploading: 'Завантаження...',
      scheduled: 'Новий документ буде завантажено',
    },
  },

  saDocument: {
    size: {
      label: '({0, fileSize, pretty})',
    },
  },

  dashboard: {
    header: 'Кокпіт',
  },

  editExpense: {
    pageHeader: {
      edit: 'Редагувати Рахунок',
      create: 'Запис Новий Рахунок',
    },
    generalInformation: {
      header: 'Загальна Інформація',
      category: {
        label: 'Категорія',
        placeholder: 'Виберіть категорію',
      },
      title: {
        label: 'Опис / Назва',
        placeholder: 'Дайте коротке резюме',
      },
      currency: {
        label: 'Валюта',
      },
      originalAmount: {
        label: 'Оригінальна Сума',
      },
      datePaid: {
        label: 'Дата Оплати',
        placeholder: 'Дата, коли рахунок оплачений',
      },
      convertedAmountInDefaultCurrency: {
        label: 'Сума в {0}',
      },
      useDifferentExchangeRateForIncomeTaxPurposes: {
        label: 'Використовати інший обмінний курс для цілей оподаткування',
      },
      incomeTaxableAmountInDefaultCurrency: {
        label: 'Сума в {0} для цілей оподаткування',
      },
      generalTax: {
        label: 'Включений Загальний Податок',
        placeholder: 'Виберіть податок',
      },
      partialForBusiness: {
        label: 'Часткова витрати для бізнесу',
      },
      percentOnBusiness: {
        label: '% суми, пов\'язаний із здійсненням підприємницької діяльності',
      },
    },
    additionalInformation: {
      header: 'Додаткова Інформація',
      notes: {
        label: 'Примітки',
        placeholder: 'Будь-яка надайте додаткову інформацію, що буде збережена для цього рахунку',
      },
    },
    attachments: {
      header: 'Вкладення',
    },
    cancel: 'Скасувати',
    save: 'Зберегти',
    validations: {
      currency: 'Будь ласка, виберіть валюту',
      title: 'Будь ласка, вкажіть назву',
      datePaid: 'Прошу повідомити дату, коли рахунок оплачується',
      originalAmount: 'Будь ласка, надайте сума витрат',
    },
  },

  expensesOverviewPanel: {
    datePaid: {
      tooltip: 'Дата Заплатили',
      label: 'Дата Заплатили',
    },
    notes: {
      tooltip: 'Додатково надаються',
      header: 'Додаткові Примітки',
    },
    generalTax: {
      tooltip: 'Загальний податок',
      label: 'Застосовні Загальні Податкові',
    },
    generalTaxRate: {
      label: 'Застосовується Загальна Податкова Ставка',
      value: '{0, bps, percent}',
    },
    generalTaxAmount: {
      label: 'Застосовні Загальні Сума Податку ',
      notProvided: 'Поки не доступний',
    },
    attachments: {
      tooltip: 'Вкладень',
      header: 'Вкладення',
    },
    foreignCurrency: {
      tooltip: 'В іноземній валюті',
      header: 'Конвертація Валюти ',
    },
    partialBusinessPurpose: {
      tooltip: 'Часткове ділової мети',
      label: 'Часткове Ділової Мети',
      value: '{0, number, percent}, пов\'язаних з підприємницькою діяльністю',
    },
    copy: 'Копія',
    edit: 'Редагувати',
    summary: {
      header: 'Резюме',
    },
    status: {
      label: 'Статус',
      short: {
        finalized: 'Завершено',
        pending: 'В очікуванні',
      },
      full: {
        finalized: 'Завершено',
        pendingConversion: 'Перетворення {0} до',
        waitingExchangeRate: 'Чекаючи курсом',
      },
    },
    category: {
      label: 'Категорія',
    },
    incomeTaxableAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Сума {0} для цілей оподаткування ',
        notProvided: 'Поки не доступний',
      },
      adjustedAmountInDefaultCurrency: {
        label: 'Сума для цілей оподаткування ',
        notProvided: 'Поки не передбачено',
      },
    },
    convertedAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Сума {0}',
        notProvided: 'Поки не доступний',
      },
    },
    generalInformation: {
      header: 'Загальна Інформація',
    },
    originalCurrency: {
      label: 'Вихідною Валюті',
    },
    originalAmount: {
      label: 'Оригінальна Сума',
    },
    differentExchangeRate: {
      label: 'Використовуючи різні обмінного курсу для цілей оподаткування?',
      value: '{0, yesNo}',
    },
  },

  expensesOverview: {
    header: 'Витрати',
    filters: {
      announcement: 'Фільтри найближчим часом ',
      input: {
        placeholder: 'Витрати на пошук ',
      },
    },
    create: 'Додати новий',
  },

  editIncomeTaxPayment: {
    header: {
      edit: 'Редагувати Сплату Податку На Доходи ',
      create: 'Запис Новий Розрахунок Податку На Прибуток',
    },
    generalInformation: {
      header: 'Загальна Інформація',
      title: {
        label: 'Опис / Назва',
        placeholder: 'Дайте коротке резюме',
      },
      amount: {
        label: 'Сума',
      },
      datePaid: {
        label: 'Дата Заплатили',
        placeholder: 'Дата сплати податку',
      },
      reportingDate: {
        label: 'Звітну Дату',
        placeholder: 'Дата включити цей платіж звітності',
      },
    },
    additionalInformation: {
      header: 'Додаткові примітки',
      notes: {
        label: 'Примітки',
        placeholder: 'Будь-яка додаткова інформація буде зберігатися протягом даного податкового платежу ',
      },
    },
    attachments: {
      header: 'Вкладення',
    },
    cancel: 'Скасувати',
    save: 'Зберегти',
    validations: {
      title: 'Будь ласка, вкажіть назву',
      datePaid: 'Прошу повідомити дату, коли сплата податку здійснюється',
      amount: 'Будь ласка, вкажіть суму податкового платежу ',
    },
  },

  incomeTaxPaymentsOverviewPanel: {
    datePaid: {
      label: 'Дата заплатили',
    },
    notes: {
      tooltip: 'Додатково надаються',
      header: 'Додаткові Примітки',
    },
    attachments: {
      tooltip: 'Вкладень',
      header: 'Вкладення',
    },
    edit: 'Редагувати',
    summary: {
      header: 'Резюме',
    },
    reportingDate: {
      label: 'Звітну Дату',
    },
  },

  incomeTaxPaymentsOverview: {
    header: 'Прибутковий Податок ',
    filters: {
      announcement: 'Фільтри найближчим часом ',
    },
    create: 'Додати новий',
  },

  myProfile: {
    languagePreferences: {
      header: 'Мовні Уподобання',
      language: {
        label: 'Мова Інтерфейсу ',
        placeholder: 'Будь ласка, оберіть мову інтерфейсу ',
      },
      locale: {
        label: 'Мову для відображення дати, Сума і т. д.',
        placeholder: 'Будь ласка, виберіть мову форматування ',
      },
    },
  },

  editIncome: {
    pageHeader: {
      edit: 'Зміна Доходу',
      create: 'Новий Запис Дохід',
    },
    generalInformation: {
      header: 'Загальна Інформація',
      category: {
        label: 'Категорія',
        placeholder: 'Виберіть категорію',
      },
      title: {
        label: 'Опис / Назва',
        placeholder: 'Дайте коротке резюме',
      },
      currency: {
        label: 'Валюта',
      },
      originalAmount: {
        label: 'Сума',
      },
      dateReceived: {
        label: 'Дата Одержання',
        placeholder: 'Дата отримання доходу',
      },
      convertedAmountInDefaultCurrency: {
        label: 'Сума {0}',
      },
      useDifferentExchangeRateForIncomeTaxPurposes: {
        label: 'Використовуючи різні обмінного курсу для цілей оподаткування ',
      },
      incomeTaxableAmountInDefaultCurrency: {
        label: 'Сума {0} для цілей оподаткування ',
      },
      generalTax: {
        label: 'Включений Загальний Податок',
        placeholder: 'Виберіть податкову',
      },
    },
    additionalInformation: {
      header: 'Додаткова Інформація',
      linkedInvoice: {
        label: 'Пов\'язано Накладної',
      },
      notes: {
        label: 'Примітки',
        placeholder: 'Будь-яка додаткова інформація бути збережені для запису доходів ',
      },
    },
    attachments: {
      header: 'Вкладення',
    },
    cancel: 'Скасувати',
    save: 'Зберегти',
    validations: {
      currency: 'Будь ласка, виберіть валюту',
      title: 'Будь ласка, вкажіть назву',
      dateReceived: 'Прошу повідомити дату, коли дохід отриманий',
      originalAmount: 'Будь ласка, вкажіть суму доходу ',
    },
    fromInvoice: {
      title: 'Платіж за {0}',
    },
  },

  incomesOverviewPanel: {
    dateReceived: {
      tooltip: 'Дата одержання',
      label: 'Дата Одержання',
    },
    notes: {
      tooltip: 'Додатково надаються',
      header: 'Додаткові Примітки',
    },
    generalTax: {
      tooltip: 'Загальний податок',
      label: 'Застосовні Загальні Податкові',
    },
    generalTaxRate: {
      label: 'Застосовується Загальна Податкова Ставка',
      value: '{0, БПС, відсотків}',
    },
    generalTaxAmount: {
      label: 'Застосовні Загальні Сума Податку ',
      notProvided: 'Поки не доступний',
    },
    attachments: {
      tooltip: 'Вкладень',
      header: 'Вкладення',
    },
    foreignCurrency: {
      tooltip: 'В іноземній валюті',
      header: 'Конвертація Валюти ',
    },
    linkedInvoice: {
      label: 'Пов\'язані Рахунку-Фактури',
      tooltip: 'Рахунки-фактури, пов\'язані',
    },
    edit: 'Редагувати',
    summary: {
      header: 'Резюме',
    },
    status: {
      label: 'Статус',
      short: {
        finalized: 'Завершено',
        pending: 'В очікуванні',
      },
      full: {
        finalized: 'Завершено',
        pendingConversion: 'Перетворення {0} до',
        waitingExchangeRate: 'Чекаючи курсом',
      },
    },
    category: {
      label: 'Категорія',
    },
    incomeTaxableAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Сума {0} для цілей оподаткування ',
        notProvided: 'Поки не доступний',
      },
      adjustedAmountInDefaultCurrency: {
        label: 'Сума для цілей оподаткування ',
        notProvided: 'Поки не передбачено',
      },
    },
    convertedAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Сума {0}',
        notProvided: 'Поки не доступний',
      },
    },
    generalInformation: {
      header: 'Загальна Інформація',
    },
    originalCurrency: {
      label: 'Вихідною Валюті',
    },
    originalAmount: {
      label: 'Оригінальна Сума',
    },
    differentExchangeRate: {
      label: 'Використовуючи різні обмінного курсу для цілей оподаткування?',
      value: '{0, yesNo}',
    },
  },

  incomesOverview: {
    header: 'Доходи',
    filters: {
      announcement: 'Фільтри найближчим часом ',
      input: {
        placeholder: 'Пошук доходи ',
      },
    },
    create: 'Додати новий',
  },

  editInvoice: {
    recordedOn: 'Записаних на {0, saDateTime, medium}',
    cancelledOn: 'Скасовано на {0, date, medium}',
    cancelInvoice: {
      button: 'Скасувати Рахунок',
      confirm: {
        message: 'Це дозволить назавжди скасувати цей рахунок. Продовжувати?',
        yes: 'Так',
        no: 'Немає',
      },
    },
    pageHeader: {
      edit: 'Редагування Накладної',
      create: 'Створити Новий Рахунок',
    },
    generalInformation: {
      header: 'Загальна Інформація',
      customer: {
        label: 'Клієнт',
        placeholder: 'Виберіть клієнта',
      },
      title: {
        label: 'Опис / Назва',
        placeholder: 'Дайте коротке резюме',
      },
      currency: {
        label: 'Валюта',
      },
      amount: {
        label: 'Сума',
      },
      dateIssued: {
        label: 'Дата Випуску',
        placeholder: 'Дата виставлення рахунків-фактур',
      },
      dueDate: {
        label: 'Дата',
        placeholder: 'Дата накладної',
      },
      alreadySent: {
        label: 'Вже Відправила',
      },
      dateSent: {
        label: 'Дата Відправки',
        placeholder: 'Дата рахунку направляються',
      },
      alreadyPaid: {
        label: 'Вже Заплатили',
      },
      datePaid: {
        label: 'Дата Заплатили',
        placeholder: 'Дату оплати.',
      },
      generalTax: {
        label: 'Включений Загальний Податок',
        placeholder: 'Виберіть податкову',
      },
    },
    additionalNotes: {
      header: 'Додаткові примітки',
      notes: {
        label: 'Примітки',
        placeholder: 'Будь-яка додаткова інформація бути збережені для запису рахунку ',
      },
    },
    attachments: {
      header: 'Вкладення',
    },
    cancel: 'Скасувати',
    save: 'Зберегти',
    validations: {
      customer: 'Будь ласка, виберіть замовника',
      currency: 'Будь ласка, виберіть валюту',
      title: 'Будь ласка, вкажіть назву',
      amount: 'Прохання представити суму рахунку ',
      dateIssued: 'Прошу повідомити дату, коли рахунок-фактура видається',
      dueDate: 'Прошу повідомити дату, коли рахунок-фактура у зв\'язку',
      dateSent: 'Прошу повідомити дату, коли рахунок-фактура відправляється',
      datePaid: 'Прошу повідомити дату, коли рахунок-фактура сплачений',
    },
  },

  invoicesOverview: {
    header: 'Рахунки-фактури',
    filters: {
      announcement: 'Фільтри найближчим часом ',
      input: {
        placeholder: 'Пошук рахунків-фактур',
      },
    },
    create: 'Додати новий',
  },

  invoicesOverviewPanel: {
    customer: {
      tooltip: 'Клієнт',
      label: 'Клієнт',
    },
    datePaid: {
      tooltip: 'Дата заплатили',
      label: 'Дата Заплатили',
    },
    notes: {
      tooltip: 'Додатково надаються',
      header: 'Додаткові Примітки',
    },
    attachments: {
      tooltip: 'Вкладень',
      header: 'Вкладення',
    },
    generalTax: {
      tooltip: 'Загальний податок',
      label: 'Застосовні Загальні Податкові',
    },
    generalTaxRate: {
      label: 'Застосовується Загальна Податкова Ставка',
      value: '{0, bps, percent}',
    },
    foreignCurrency: {
      tooltip: 'В іноземній валюті',
    },
    edit: 'Редагувати',
    markAsSent: 'Відправила сьогодні',
    markAsPaid: 'Сьогодні',
    generalInformation: {
      header: 'Загальна Інформація',
    },
    status: {
      label: 'Статус',
      finalized: 'Завершено',
      draft: 'Проект',
      cancelled: 'Скасовано',
      sent: 'Відправив',
      overdue: 'Прострочені',
    },
    currency: {
      label: 'Валюти Рахунку ',
    },
    amount: {
      label: 'Сума Рахунку ',
    },
    dateIssued: {
      label: 'Дата Випуску',
    },
    dueDate: {
      label: 'Дата',
    },
    dateSent: {
      label: 'Дата Відправки',
    },
    dateCancelled: {
      label: 'Дата Скасування',
    },
  },

  saCurrencyInput: {
    groups: {
      recent: 'Нещодавно Використовуваних Валют',
      all: 'Всі Валюти',
    },
    currencyLabel: '{code} - {displayName}',
  },

  el: {
    datepicker: {
      now: 'Зараз',
      today: 'Сьогодні',
      cancel: 'Скасувати',
      clear: 'Зрозуміло',
      confirm: 'ОК',
      selectDate: 'Виберіть дату',
      selectTime: 'Виберіть час',
      startDate: 'Дата Початку',
      startTime: 'Час Початку',
      endDate: 'Дата Закінчення',
      endTime: 'Час Закінчення',
      prevYear: 'Попередній Рік',
      nextYear: 'В Наступному Році',
      prevMonth: 'Попередній Місяць',
      nextMonth: 'В Наступному Місяці',
      year: '',
      month1: 'Січня',
      month2: 'Лютий',
      month3: 'Березня',
      month4: 'Квітня',
      month5: 'Може',
      month6: 'Червня',
      month7: 'Липня',
      month8: 'Серпня',
      month9: 'Вересня',
      month10: 'Жовтня',
      month11: 'Листопада',
      month12: 'Грудня',
      week: 'тиждень',
      weeks: {
        sun: 'Сонце',
        mon: 'Пн',
        tue: 'Вт',
        wed: 'СР',
        thu: 'Тьху',
        fri: 'Пт',
        sat: 'Сидів',
      },
      months: {
        jan: 'Січня',
        feb: 'Лютого',
        mar: 'Березня',
        apr: 'Квітня',
        may: 'Може',
        jun: 'Червня',
        jul: 'Липня',
        aug: 'Серпня',
        sep: 'Вересня',
        oct: 'Жовтня',
        nov: 'Листопада',
        dec: 'Грудня',
      },
    },
    select: {
      loading: 'Завантаження',
      noMatch: 'Без відповідних даних',
      noData: 'Немає даних',
      placeholder: 'Виберіть',
    },
    pagination: {
      goto: 'Перейти до',
      pagesize: '/стор.',
      total: 'Загального {total}',
      pageClassifier: '',
    },
    table: {
      emptyText: 'Немає Даних',
      confirmFilter: 'Підтвердити',
      resetFilter: 'Скидання',
      clearFilter: 'Всі',
      sumText: 'Сума',
    },
    messagebox: {
      confirm: 'ОК',
    },
  },

  useDocumentsUpload: {
    documentsUploadFailure: 'Деякі документи не були завантажені. Повторіть спробу або видалити їх.',
  },

  saGoogleDriveIntegrationSetup: {
    successful: {
      status: 'Інтеграція з Google Drive активна',
      details: 'Усі документи зберігаються до папки {folderLink}',
    },
    unknown: {
      status: 'Перевіряємо стан інтеграції...',
      details: 'Будь ласка, зачекайте доки ми перевіримо стан',
    },
    authorizationRequired: {
      status: 'Потрібна авторизація',
      details: {
        message: 'Будь ласка, надайте доступ для збереження документів у Ваш Google Drive.{action}',
        startAction: 'Дати доступ зараз',
      },
    },
    authorizationInProgress: {
      status: 'Авторизація в процесі...',
      details: {
        line1: 'Будь ласка, продовжіть авторизацію у вікні, що з\'явилося.',
        line2: 'Коли авторизацію буде завершено, ми автоматично оновимо статус тут.',
      },
    },
    authorizationFailed: {
      status: 'Авторизація не вдалася',
      details: {
        message: 'На жаль, ми не змогли отримати потрібний доступ :( {action}',
        retryAction: 'Спробувати ще раз',
      },
    },
  },

  saFailedDocumentsStorageMessage: {
    title: 'Налаштування збереження документів не активні',
    message: 'Будь ласка, перейдіть до Ваших {0} та виправте конфігурацію.',
    profileLink: 'налаштуваннь профілю',
  },

  errorHandler: {
    fatalApiError: 'Сталася помилка на сервері. Будь-ласка, спробуйте ще раз.',
  },

  saBasicErrorMessage: {
    defaultMessage: 'Сталася помилка. Будь ласка, спробуйте ще пізніше.',
  },
};
