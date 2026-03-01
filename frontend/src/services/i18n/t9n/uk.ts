/* eslint-disable vue/max-len */
import { format } from '@/services/i18n/t9n/formatter';

export default {
  common: {
    date: {
      medium: (date?: Date | string) => format('{0, date, medium}', [date]),
    },
    dateTime: {
      medium: (dateTime?: Date | string) => format('{0, saDateTime, medium}', [dateTime]),
    },
    amount: {
      withCurrency: (amountInCents: number, currency: string) => format('{0, amount, withCurrency}', [{
        currency,
        amountInCents,
      }]),
    },
    yesNo: {
      yes: () => 'Так',
      no: () => 'Ні',
    },
    percent: (value: number) => format('{0, number, :: percent scale/100}', [value]),
    cancel: () => 'Скасувати',
    save: () => 'Зберегти',
  },

  loginPage: {
    announcement: () => 'Вперше тут? Ми запускаємо публічний доступ скоро.',
    userName: {
      placeholder: () => 'Логін',
    },
    password: {
      placeholder: () => 'Пароль',
    },
    rememberMe: {
      label: () => 'Запам\'ятати мене на 30 днів',
    },
    login: () => 'Ввійти',
    loginError: {
      underAttack: () => 'Схоже, що ваш акаунт знаходиться під атакою!',
      generalFailure: () => 'Спроба входу не вдалася. Будь ласка, переконайтеся, що логін і пароль правильний',
      accountLocked: (lockDurationInSec: number) => format('Обліковий запис тимчасово заблоковано. Він буде розблокований через {0, duration}', [lockDurationInSec]),
      userNotActivated: () => 'Ваш обліковий запис ще не активовано. Будь ласка, використовуйте токен, який вам надали адміністратори. Зв\'яжіться з ними, якщо вам потрібно скинути токен',
    },
  },

  navigationMenu: {
    dashboard: () => 'Кокпіт',
    expenses: () => 'Витрати',
    incomes: () => 'Доходи',
    invoices: () => 'Рахунки-фактури',
    taxPayments: () => 'Податок на прибуток',
    reporting: () => 'Репорти',
    settings: {
      header: () => 'Параметри',
      customers: () => 'Клієнти',
      categories: () => 'Категорії',
      generalTaxes: () => 'Загальні податки',
      workspaces: () => 'Проекти',
    },
    user: {
      header: () => 'Користувач',
      profile: () => 'Мій Профіль',
      logout: () => 'Вихід',
    },
    admin: {
      users: () => 'Користувачі',
    },
  },

  saDocumentDownloadLink: {
    label: () => 'Завантажити',
    creatingLinkMessage: () => 'Починаємо збереження файлу...',
  },

  saDocumentUpload: {
    fileSelector: {
      message: () => 'Перетягніть сюди файл або натисніть, щоб завантажити',
      hint: (fileSizeInBytes: number) => format('Підтримуються файли до {0, fileSize, pretty}', [fileSizeInBytes]),
    },
    uploadStatusMessage: {
      error: () => 'Завантаження не вдалося, будь ласка спробуйте знову',
      uploading: () => 'Завантаження...',
      scheduled: () => 'Новий документ буде завантажено',
    },
  },

  saDocument: {
    size: {
      label: (fileSizeInBytes: number) => format('({0, fileSize, pretty})', [fileSizeInBytes]),
    },
  },

  dashboard: {
    header: () => 'Кокпіт',
    dateRange: {
      separator: () => 'До',
      startPlaceholder: () => 'Дата початку',
      endPlaceholder: () => 'Дата закінчення',
    },
    cards: {
      expenses: {
        totalHeader: (count: number) => format('Усього {0, number} витрат', [count]),
        pendingHeader: (count: number) => format('В очікуванні ще {0, number}', [count]),
      },
      incomes: {
        totalHeader: (count: number) => format('Усього {0, number} доходів', [count]),
        pendingHeader: (count: number) => format('В очікуванні ще {0, number}', [count]),
      },
      profit: {
        taxableAmount: () => 'Оподатковувана Сума',
        currencyExchangeDifference: () => 'Різниця валютного курсу',
        incomeTaxPayments: () => 'Виплати Податку на Прибуток',
        estimatedTax: () => 'Розрахунковий Податок',
        estimatedTaxPlaceholder: () => 'незабаром..',
        profit: () => 'Прибуток',
      },
      invoice: {
        to: () => 'Кому',
        issueDate: () => 'Дата Випуску',
        dateSent: () => 'Дата Відправки',
        dueDate: () => 'Дата Сплати',
        status: {
          overdue: () => 'Прострочено',
          pending: () => 'В очікуванні',
        },
      },
    },
  },

  editExpense: {
    pageHeader: {
      edit: () => 'Редагувати Рахунок',
      create: () => 'Запис Новий Рахунок',
    },
    generalInformation: {
      header: () => 'Загальна Інформація',
      category: {
        label: () => 'Категорія',
        placeholder: () => 'Виберіть категорію',
      },
      title: {
        label: () => 'Опис / Назва',
        placeholder: () => 'Дайте коротке резюме',
      },
      currency: {
        label: () => 'Валюта',
      },
      originalAmount: {
        label: () => 'Оригінальна Сума',
      },
      datePaid: {
        label: () => 'Дата Оплати',
        placeholder: () => 'Дата, коли рахунок оплачений',
      },
      convertedAmountInDefaultCurrency: {
        label: (currency: string) => format('Сума в {0}', [currency]),
      },
      useDifferentExchangeRateForIncomeTaxPurposes: {
        label: () => 'Використовати інший обмінний курс для цілей оподаткування',
      },
      incomeTaxableAmountInDefaultCurrency: {
        label: (currency: string) => format('Сума в {0} для цілей оподаткування', [currency]),
      },
      generalTax: {
        label: () => 'Включений Загальний Податок',
        placeholder: () => 'Виберіть податок',
      },
      partialForBusiness: {
        label: () => 'Часткова витрати для бізнесу',
      },
      percentOnBusiness: {
        label: () => '% суми, пов\'язаний із здійсненням підприємницької діяльності',
      },
    },
    additionalInformation: {
      header: () => 'Додаткова Інформація',
      notes: {
        label: () => 'Примітки',
        placeholder: () => 'Будь-яка надайте додаткову інформацію, що буде збережена для цього рахунку',
      },
    },
    attachments: {
      header: () => 'Вкладення',
    },
    cancel: () => 'Скасувати',
    save: () => 'Зберегти',
    validations: {
      currency: () => 'Будь ласка, виберіть валюту',
      title: () => 'Будь ласка, вкажіть назву',
      datePaid: () => 'Прошу повідомити дату, коли рахунок оплачується',
      originalAmount: () => 'Будь ласка, надайте сума витрат',
    },
  },

  expensesOverviewPanel: {
    datePaid: {
      tooltip: () => 'Дата Заплатили',
      label: () => 'Дата Заплатили',
    },
    notes: {
      tooltip: () => 'Додатково надаються',
      header: () => 'Додаткові Примітки',
    },
    generalTax: {
      tooltip: () => 'Загальний податок',
      label: () => 'Застосовні Загальні Податкові',
    },
    generalTaxRate: {
      label: () => 'Застосовується Загальна Податкова Ставка',
      value: (bps: number) => format('{0, bps, percent}', [bps]),
    },
    generalTaxAmount: {
      label: () => 'Застосовні Загальні Сума Податку ',
      notProvided: () => 'Поки не доступний',
    },
    attachments: {
      tooltip: () => 'Вкладень',
      header: () => 'Вкладення',
    },
    foreignCurrency: {
      tooltip: () => 'В іноземній валюті',
      header: () => 'Конвертація Валюти ',
    },
    partialBusinessPurpose: {
      tooltip: () => 'Часткове ділової мети',
      label: () => 'Часткове Ділової Мети',
      value: (value: number) => format('{0, number, percent}, пов\'язаних з підприємницькою діяльністю', [value]),
    },
    copy: () => 'Копія',
    edit: () => 'Редагувати',
    summary: {
      header: () => 'Резюме',
    },
    status: {
      label: () => 'Статус',
      short: {
        finalized: () => 'Завершено',
        pending: () => 'В очікуванні',
      },
      full: {
        finalized: () => 'Завершено',
        pendingConversion: (currency: string) => format('Чекаємо на конверсію до {0}', [currency]),
        waitingExchangeRate: () => 'Чекаючи курсом',
      },
    },
    category: {
      label: () => 'Категорія',
    },
    incomeTaxableAmounts: {
      originalAmountInDefaultCurrency: {
        label: (currency: string) => format('Сума {0} для цілей оподаткування', [currency]),
        notProvided: () => 'Поки не доступний',
      },
      adjustedAmountInDefaultCurrency: {
        label: () => 'Сума для цілей оподаткування ',
        notProvided: () => 'Поки не передбачено',
      },
    },
    convertedAmounts: {
      originalAmountInDefaultCurrency: {
        label: (currency: string) => format('Сума в {0}', [currency]),
        notProvided: () => 'Поки не доступний',
      },
    },
    generalInformation: {
      header: () => 'Загальна Інформація',
    },
    originalCurrency: {
      label: () => 'Вихідною Валюті',
    },
    originalAmount: {
      label: () => 'Оригінальна Сума',
    },
    differentExchangeRate: {
      label: () => 'Використовуючи різні обмінного курсу для цілей оподаткування?',
      value: (value: boolean) => format('{0, yesNo}', [value]),
    },
  },

  expensesOverview: {
    header: () => 'Витрати',
    filters: {
      announcement: () => 'Фільтри будуть незабаром',
      input: {
        placeholder: () => 'Витрати на пошук ',
      },
    },
    create: () => 'Додати новий',
  },

  editIncomeTaxPayment: {
    header: {
      edit: () => 'Редагувати Сплату Податку На Доходи ',
      create: () => 'Запис Новий Розрахунок Податку На Прибуток',
    },
    generalInformation: {
      header: () => 'Загальна Інформація',
      title: {
        label: () => 'Опис / Назва',
        placeholder: () => 'Дайте коротке резюме',
      },
      amount: {
        label: () => 'Сума',
      },
      datePaid: {
        label: () => 'Дата Заплатили',
        placeholder: () => 'Дата сплати податку',
      },
      reportingDate: {
        label: () => 'Звітну Дату',
        placeholder: () => 'Дата включити цей платіж звітності',
      },
    },
    additionalInformation: {
      header: () => 'Додаткові примітки',
      notes: {
        label: () => 'Примітки',
        placeholder: () => 'Будь-яка додаткова інформація буде зберігатися протягом даного податкового платежу ',
      },
    },
    attachments: {
      header: () => 'Вкладення',
    },
    cancel: () => 'Скасувати',
    save: () => 'Зберегти',
    validations: {
      title: () => 'Будь ласка, вкажіть назву',
      datePaid: () => 'Прошу повідомити дату, коли сплата податку здійснюється',
      amount: () => 'Будь ласка, вкажіть суму податкового платежу ',
    },
  },

  incomeTaxPaymentsOverviewPanel: {
    datePaid: {
      label: () => 'Дата заплатили',
    },
    notes: {
      tooltip: () => 'Додатково надаються',
      header: () => 'Додаткові Примітки',
    },
    attachments: {
      tooltip: () => 'Вкладень',
      header: () => 'Вкладення',
    },
    edit: () => 'Редагувати',
    summary: {
      header: () => 'Резюме',
    },
    reportingDate: {
      label: () => 'Звітну Дату',
    },
  },

  incomeTaxPaymentsOverview: {
    header: () => 'Прибутковий Податок ',
    filters: {
      announcement: () => 'Фільтри будуть незабаром',
    },
    create: () => 'Додати новий',
  },

  myProfile: {
    pageHeader: () => 'Мій Профіль',
    documentsStorage: {
      header: () => 'Сховище документів',
      googleDrive: () => 'Google Drive',
    },
    languagePreferences: {
      header: () => 'Мовні Уподобання',
      language: {
        label: () => 'Мова Інтерфейсу ',
        placeholder: () => 'Будь ласка, оберіть мову інтерфейсу ',
      },
      locale: {
        label: () => 'Мову для відображення дати, Сума і т. д.',
        placeholder: () => 'Будь ласка, виберіть мову форматування ',
      },
      feedback: {
        success: () => 'Мовні налаштування були збережені',
      },
    },
    changePassword: {
      header: () => 'Зміна Пароля',
      currentPassword: {
        label: () => 'Поточний Пароль',
        placeholder: () => 'Будь ласка, введіть ваш поточний пароль',
      },
      newPassword: {
        label: () => 'Новий Пароль',
        placeholder: () => 'Будь ласка, введіть новий пароль',
      },
      newPasswordConfirmation: {
        label: () => 'Підтвердження Нового Пароля',
        placeholder: () => 'Будь ласка, підтвердьте новий пароль',
      },
      submit: {
        label: () => 'Змінити Пароль',
      },
      validations: {
        confirmationDoesNotMatch: () => 'Підтвердження не збігається з новим паролем',
        currentPasswordMismatch: () => 'Поточний пароль не відповідає вашому обліковому запису',
      },
      feedback: {
        success: () => 'Ваш пароль був успішно змінений',
      },
    },
  },

  editIncome: {
    pageHeader: {
      edit: () => 'Зміна Доходу',
      create: () => 'Новий Запис Дохід',
    },
    generalInformation: {
      header: () => 'Загальна Інформація',
      category: {
        label: () => 'Категорія',
        placeholder: () => 'Виберіть категорію',
      },
      title: {
        label: () => 'Опис / Назва',
        placeholder: () => 'Дайте коротке резюме',
      },
      currency: {
        label: () => 'Валюта',
      },
      originalAmount: {
        label: () => 'Сума',
      },
      dateReceived: {
        label: () => 'Дата Одержання',
        placeholder: () => 'Дата отримання доходу',
      },
      convertedAmountInDefaultCurrency: {
        label: (currency: string) => format('Сума в {0}', [currency]),
      },
      useDifferentExchangeRateForIncomeTaxPurposes: {
        label: () => 'Використовуючи різні обмінного курсу для цілей оподаткування ',
      },
      incomeTaxableAmountInDefaultCurrency: {
        label: (currency: string) => format('Сума в {0} для цілей оподаткування', [currency]),
      },
      generalTax: {
        label: () => 'Включений Загальний Податок',
        placeholder: () => 'Виберіть податкову',
      },
    },
    additionalInformation: {
      header: () => 'Додаткова Інформація',
      linkedInvoice: {
        label: () => 'Пов\'язано Накладної',
      },
      notes: {
        label: () => 'Примітки',
        placeholder: () => 'Будь-яка додаткова інформація бути збережені для запису доходів ',
      },
    },
    attachments: {
      header: () => 'Вкладення',
    },
    cancel: () => 'Скасувати',
    save: () => 'Зберегти',
    validations: {
      currency: () => 'Будь ласка, виберіть валюту',
      title: () => 'Будь ласка, вкажіть назву',
      dateReceived: () => 'Прошу повідомити дату, коли дохід отриманий',
      originalAmount: () => 'Будь ласка, вкажіть суму доходу ',
    },
    fromInvoice: {
      title: (invoiceTitle: string) => format('Платіж за {0}', [invoiceTitle]),
    },
  },

  incomesOverviewPanel: {
    dateReceived: {
      tooltip: () => 'Дата одержання',
      label: () => 'Дата Одержання',
    },
    notes: {
      tooltip: () => 'Додатково надаються',
      header: () => 'Додаткові Примітки',
    },
    generalTax: {
      tooltip: () => 'Загальний податок',
      label: () => 'Застосовні Загальні Податкові',
    },
    generalTaxRate: {
      label: () => 'Застосовується Загальна Податкова Ставка',
      value: (bps: number) => format('{0, bps, percent}', [bps]),
    },
    generalTaxAmount: {
      label: () => 'Застосовні Загальні Сума Податку ',
      notProvided: () => 'Поки не доступний',
    },
    attachments: {
      tooltip: () => 'Вкладень',
      header: () => 'Вкладення',
    },
    foreignCurrency: {
      tooltip: () => 'В іноземній валюті',
      header: () => 'Конвертація Валюти ',
    },
    linkedInvoice: {
      label: () => 'Пов\'язані Рахунку-Фактури',
      tooltip: () => 'Рахунки-фактури, пов\'язані',
    },
    edit: () => 'Редагувати',
    summary: {
      header: () => 'Резюме',
    },
    status: {
      label: () => 'Статус',
      short: {
        finalized: () => 'Завершено',
        pending: () => 'В очікуванні',
      },
      full: {
        finalized: () => 'Завершено',
        pendingConversion: (currency: string) => format('Чекаємо на конверсію до {0}', [currency]),
        waitingExchangeRate: () => 'Чекаємо на обмінний курс',
      },
    },
    category: {
      label: () => 'Категорія',
    },
    incomeTaxableAmounts: {
      originalAmountInDefaultCurrency: {
        label: (currency: string) => format('Сума в {0} для цілей оподаткування', [currency]),
        notProvided: () => 'Поки не доступний',
      },
      adjustedAmountInDefaultCurrency: {
        label: () => 'Сума для цілей оподаткування ',
        notProvided: () => 'Поки не передбачено',
      },
    },
    convertedAmounts: {
      originalAmountInDefaultCurrency: {
        label: (currency: string) => format('Сума в {0}', [currency]),
        notProvided: () => 'Поки не доступний',
      },
    },
    generalInformation: {
      header: () => 'Загальна Інформація',
    },
    originalCurrency: {
      label: () => 'Вихідною Валюті',
    },
    originalAmount: {
      label: () => 'Оригінальна Сума',
    },
    differentExchangeRate: {
      label: () => 'Використовуючи різні обмінного курсу для цілей оподаткування?',
      value: (value: boolean) => format('{0, yesNo}', [value]),
    },
  },

  incomesOverview: {
    header: () => 'Доходи',
    filters: {
      announcement: () => 'Фільтри будуть незабаром',
      input: {
        placeholder: () => 'Пошук доходи ',
      },
    },
    create: () => 'Додати новий',
  },

  editInvoice: {
    cancelInvoice: {
      button: () => 'Скасувати Рахунок',
      confirm: {
        message: () => 'Це дозволить назавжди скасувати цей рахунок. Продовжувати?',
        yes: () => 'Так',
        no: () => 'Немає',
      },
      status: () => 'Цей разунок було відмінено',
    },
    pageHeader: {
      edit: () => 'Редагування Накладної',
      create: () => 'Створити Новий Рахунок',
    },
    generalInformation: {
      header: () => 'Загальна Інформація',
      customer: {
        label: () => 'Клієнт',
        placeholder: () => 'Виберіть клієнта',
      },
      title: {
        label: () => 'Опис / Назва',
        placeholder: () => 'Дайте коротке резюме',
      },
      currency: {
        label: () => 'Валюта',
      },
      amount: {
        label: () => 'Сума',
      },
      dateIssued: {
        label: () => 'Дата Випуску',
        placeholder: () => 'Дата виставлення рахунків-фактур',
      },
      dueDate: {
        label: () => 'Дата',
        placeholder: () => 'Дата накладної',
      },
      alreadySent: {
        label: () => 'Вже Відправила',
      },
      dateSent: {
        label: () => 'Дата Відправки',
        placeholder: () => 'Дата рахунку направляються',
      },
      alreadyPaid: {
        label: () => 'Вже Заплатили',
      },
      datePaid: {
        label: () => 'Дата Заплатили',
        placeholder: () => 'Дату оплати.',
      },
      generalTax: {
        label: () => 'Включений Загальний Податок',
        placeholder: () => 'Виберіть податкову',
      },
    },
    additionalNotes: {
      header: () => 'Додаткові примітки',
      notes: {
        label: () => 'Примітки',
        placeholder: () => 'Будь-яка додаткова інформація бути збережені для запису рахунку ',
      },
    },
    attachments: {
      header: () => 'Вкладення',
    },
    cancel: () => 'Скасувати',
    save: () => 'Зберегти',
    validations: {
      customer: () => 'Будь ласка, виберіть замовника',
      currency: () => 'Будь ласка, виберіть валюту',
      title: () => 'Будь ласка, вкажіть назву',
      amount: () => 'Прохання представити суму рахунку ',
      dateIssued: () => 'Прошу повідомити дату, коли рахунок-фактура видається',
      dueDate: () => 'Прошу повідомити дату, коли рахунок-фактура у зв\'язку',
      dateSent: () => 'Прошу повідомити дату, коли рахунок-фактура відправляється',
      datePaid: () => 'Прошу повідомити дату, коли рахунок-фактура сплачений',
    },
  },

  invoicesOverview: {
    header: () => 'Рахунки-фактури',
    filters: {
      announcement: () => 'Фільтри будуть незабаром',
      input: {
        placeholder: () => 'Пошук рахунків-фактур',
      },
    },
    create: () => 'Додати новий',
  },

  invoicesOverviewPanel: {
    customer: {
      tooltip: () => 'Клієнт',
      label: () => 'Клієнт',
    },
    datePaid: {
      tooltip: () => 'Дата заплатили',
      label: () => 'Дата Заплатили',
    },
    notes: {
      tooltip: () => 'Додатково надаються',
      header: () => 'Додаткові Примітки',
    },
    attachments: {
      tooltip: () => 'Вкладень',
      header: () => 'Вкладення',
    },
    generalTax: {
      tooltip: () => 'Загальний податок',
      label: () => 'Застосовні Загальні Податкові',
    },
    generalTaxRate: {
      label: () => 'Застосовується Загальна Податкова Ставка',
      value: (bps: number) => format('{0, bps, percent}', [bps]),
    },
    foreignCurrency: {
      tooltip: () => 'В іноземній валюті',
    },
    edit: () => 'Редагувати',
    markAsSent: () => 'Надіслано сьогодні',
    markAsPaid: () => 'Оплата Отримана',
    generalInformation: {
      header: () => 'Загальна Інформація',
    },
    status: {
      label: () => 'Статус',
      finalized: () => 'Завершено',
      draft: () => 'Проект',
      cancelled: () => 'Скасовано',
      sent: () => 'Відправив',
      overdue: () => 'Прострочені',
    },
    currency: {
      label: () => 'Валюти Рахунку ',
    },
    amount: {
      label: () => 'Сума Рахунку ',
    },
    dateIssued: {
      label: () => 'Дата Випуску',
    },
    dueDate: {
      label: () => 'Дата',
    },
    dateSent: {
      label: () => 'Дата Відправки',
    },
  },

  saCurrencyInput: {
    groups: {
      recent: () => 'Нещодавно Використовуваних Валют',
      all: () => 'Всі Валюти',
    },
    currencyLabel: ({
      code,
      displayName,
    }: { code: string, displayName: string }) => `${code} - ${displayName}`,
  },

  el: {
    datepicker: {
      now: () => 'Зараз',
      today: () => 'Сьогодні',
      cancel: () => 'Скасувати',
      clear: () => 'Зрозуміло',
      confirm: () => 'ОК',
      selectDate: () => 'Виберіть дату',
      selectTime: () => 'Виберіть час',
      startDate: () => 'Дата Початку',
      startTime: () => 'Час Початку',
      endDate: () => 'Дата Закінчення',
      endTime: () => 'Час Закінчення',
      prevYear: () => 'Попередній Рік',
      nextYear: () => 'В Наступному Році',
      prevMonth: () => 'Попередній Місяць',
      nextMonth: () => 'В Наступному Місяці',
      year: () => '',
      month1: () => 'Січня',
      month2: () => 'Лютий',
      month3: () => 'Березня',
      month4: () => 'Квітня',
      month5: () => 'Може',
      month6: () => 'Червня',
      month7: () => 'Липня',
      month8: () => 'Серпня',
      month9: () => 'Вересня',
      month10: () => 'Жовтня',
      month11: () => 'Листопада',
      month12: () => 'Грудня',
      week: () => 'тиждень',
      weeks: {
        sun: () => 'Сонце',
        mon: () => 'Пн',
        tue: () => 'Вт',
        wed: () => 'СР',
        thu: () => 'Тьху',
        fri: () => 'Пт',
        sat: () => 'Сидів',
      },
      months: {
        jan: () => 'Січня',
        feb: () => 'Лютого',
        mar: () => 'Березня',
        apr: () => 'Квітня',
        may: () => 'Може',
        jun: () => 'Червня',
        jul: () => 'Липня',
        aug: () => 'Серпня',
        sep: () => 'Вересня',
        oct: () => 'Жовтня',
        nov: () => 'Листопада',
        dec: () => 'Грудня',
      },
    },
    select: {
      loading: () => 'Завантаження',
      noMatch: () => 'Без відповідних даних',
      noData: () => 'Немає даних',
      placeholder: () => 'Виберіть',
    },
    pagination: {
      goto: () => 'Перейти до',
      pagesize: () => '/стор.',
      total: () => 'Загального {total}',
      pageClassifier: () => '',
    },
    table: {
      emptyText: () => 'Немає Даних',
      confirmFilter: () => 'Підтвердити',
      resetFilter: () => 'Скидання',
      clearFilter: () => 'Всі',
      sumText: () => 'Сума',
    },
    messagebox: {
      confirm: () => 'ОК',
    },
  },

  useDocumentsUpload: {
    documentsUploadFailure: () => 'Деякі документи не були завантажені. Повторіть спробу або видалити їх.',
  },

  saGoogleDriveIntegrationSetup: {
    successful: {
      status: () => 'Інтеграція з Google Drive активна',
      details: () => 'Усі документи зберігаються до папки {folderLink}',
    },
    unknown: {
      status: () => 'Перевіряємо стан інтеграції...',
      details: () => 'Будь ласка, зачекайте доки ми перевіримо стан',
    },
    authorizationRequired: {
      status: () => 'Потрібна авторизація',
      details: {
        message: () => 'Будь ласка, надайте доступ для збереження документів у Ваш Google Drive.{action}',
        startAction: () => 'Дати доступ зараз',
      },
    },
    authorizationInProgress: {
      status: () => 'Авторизація в процесі...',
      details: {
        line1: () => 'Будь ласка, продовжіть авторизацію у вікні, що з\'явилося.',
        line2: () => 'Коли авторизацію буде завершено, ми автоматично оновимо статус тут.',
      },
    },
    authorizationFailed: {
      status: () => 'Авторизація не вдалася',
      details: {
        message: () => 'На жаль, ми не змогли отримати потрібний доступ :( {action}',
        retryAction: () => 'Спробувати ще раз',
      },
    },
  },

  saFailedDocumentsStorageMessage: {
    title: () => 'Налаштування збереження документів не активні',
    message: () => 'Будь ласка, перейдіть до Ваших {default} та виправте конфігурацію.',
    profileLink: () => 'налаштуваннь профілю',
  },

  errorHandler: {
    fatalErrorMessage: () => 'Ми зіткнулися з технічною помилкою. Будь ласка, розгляньте можливість повідомити про проблему та оновіть сторінку, щоб спробувати знову.',
  },

  saBasicErrorMessage: {
    defaultMessage: () => 'Сталася помилка. Будь ласка, спробуйте ще пізніше.',
  },

  saEntitySelect: {
    loading: {
      text: () => 'Завантаження...',
    },
    noData: {
      text: () => 'Результатів не знайдено',
    },
    moreElements: {
      text: (count: number) => format('Іще {0} елементів..', [count]),
    },
  },

  saInvoiceSelect: {
    placeholder: () => 'Оберіть або знайдіть рахунок',
  },

  usersOverview: {
    header: () => 'Користувачі',
    filters: {
      announcement: () => 'Фільтри будуть незабаром',
      input: {
        placeholder: () => 'Пошук користувачів',
      },
    },
    create: () => 'Створити користувача',
  },

  adminOverviewPanel: {
    userTypeAdmin: () => 'Адміністратор',
    userTypeRegular: () => 'Користувач',
    userActivated: () => 'Активований',
    userNotActivated: () => 'Не активований',
    edit: () => 'Редагувати',
  },

  formValidationMessages: {
    sizeMinMax: (min: number, max: number) => format('Довжина цього значення не повинна бути коротшою за {min, number} та не довшою за {max, number} символів', {
      min,
      max,
    }),
    sizeMax: (max: number) => format('Довжина цього значення не повинна перевищувати {max, number} символів', {
      max,
    }),
    notBlank: () => 'Це поле є обов\'язковим і не повинно бути порожнім',
  },

  accountActivationPage: {
    loading: () => 'Ми перевіряємо ваш токен...',
    badToken: () => 'Наданий токен недійсний або його термін дії закінчився. Будь ласка, запросіть новий.',
    instructions: () => 'Будь ласка, введіть ваш новий пароль. Після цього вам потрібно буде увійти, використовуючи ваше ім\'я користувача та новий пароль.',
    form: {
      password: {
        label: () => 'Новий пароль',
        placeholder: () => 'Будь ласка, введіть ваш новий пароль',
      },
      passwordConfirmation: {
        label: () => 'Підтвердження нового паролю',
        placeholder: () => 'Будь ласка, підтвердіть ваш новий пароль',
        notMatchingError: () => 'Паролі не співпадають',
      },
      submit: () => 'Активувати обліковий запис',
    },
    success: {
      message: () => 'Обліковий запис активовано. Тепер ви можете увійти, використовуючи свої облікові дані.',
      button: () => 'Увійти зараз',
    },
  },

  editUser: {
    pageHeader: {
      edit: () => 'Редагувати користувача',
      create: () => 'Створити нового користувача',
    },
    form: {
      userName: {
        label: () => 'Ім\'я користувача',
        errors: {
          userAlreadyExists: (userName: string) => format('Користувач з іменем "{0}" вже існує', [userName]),
        },
      },
      role: {
        label: () => 'Роль користувача',
        options: {
          user: () => 'Користувач',
          admin: () => 'Адміністратор',
        },
      },
      activationStatus: {
        label: () => 'Статус активації',
        activated: () => 'Активовано',
        loading: () => 'Завантаження...',
        notActivated: () => 'Обліковий запис користувача ще не активовано. Будь ласка, поділіться посиланням нижче з користувачем, щоб він міг налаштувати свій пароль.',
        copied: () => 'Посилання скопійовано до буфера обміну',
      },
    },
    successNotification: (userName: string) => format('Користувач {0} був успішно збережений', [userName]),
  },

  saForm: {
    inputValidationFailed: () => 'Деякі поля не були правильно заповнені. Будь ласка, перевірте форму та спробуйте знову.',
  },

  accountSetup: {
    welcomeMessage: () => 'Ласкаво просимо до simple-accounting! Ми зараз створимо новий робочий простір для вас - місце, де зберігаються дані вашого бізнесу. Якщо у вас є декілька бізнесів, кожен з них може бути представлений як окремий робочий простір. Ви зможете налаштувати інші робочі простори, якщо це буде потрібно, після початкової конфігурації.',
    workspaceNameLabel: () => 'Назва робочого простору',
    workspaceNamePlaceholder: () => 'Вкажіть назву для вашого робочого простору',
    defaultCurrencyLabel: () => 'Основна (за замовчуванням) валюта',
    defaultCurrencyPlaceholder: () => 'Вкажіть основну валюту цього робочого простору',
    submitButton: () => 'Завершити налаштування',
  },

  reporting: {
    header: () => 'Звітність',
    wizard: {
      steps: {
        selectReport: {
          title: () => 'Оберіть звіт',
          description: {
            select: () => 'Будь ласка, оберіть звіт',
            selected: () => 'Податковий Звіт',
            unknown: () => 'Невідомий Звіт o_O',
          },
        },
        selectDates: {
          title: () => 'Оберіть дати звітності',
          description: {
            select: () => 'Будь ласка, оберіть період звітності',
            selected: (fromDate: string, toDate: string) => format('{0} до {1}', [fromDate, toDate]),
          },
        },
        viewReport: {
          title: () => 'Переглянути звіт',
          description: {
            loading: () => 'Завантаження..',
            ready: () => 'Готово',
          },
        },
      },
      reports: {
        generalTax: {
          title: () => 'Звіт Загального Податку',
          description: () => 'Зібрані та сплачені загальні податки',
        },
      },
      dateRange: {
        separator: () => 'До',
        startPlaceholder: () => 'Дата початку',
        endPlaceholder: () => 'Дата закінчення',
      },
      buttons: {
        select: () => 'Обрати',
        next: () => 'Далі',
      },
    },
  },

  saPageableItems: {
    emptyResults: () => 'Тут немає результатів',
  },

  customersOverview: {
    header: () => 'Клієнти',
    filters: {
      announcement: () => 'Фільтри незабаром',
    },
    create: () => 'Додати новий',
    edit: () => 'Редагувати',
  },

  generalTaxReport: {
    emptyData: () => 'Немає даних для відображення',
    columns: {
      tax: () => 'Податок',
      taxableIncome: () => 'Оподатковуваний Дохід',
      taxCollected: () => 'Зібрано Податку',
      taxPaid: () => 'Сплачено Податку',
      balance: () => 'Баланс',
    },
    sections: {
      collected: () => 'Зібрано',
      paid: () => 'Сплачено',
      total: () => 'Усього',
    },
    tableColumns: {
      tax: () => 'Податок',
      numberOfItems: () => 'Кількість Елементів',
      itemsAmount: () => 'Сума Елементів',
      taxAmount: () => 'Сума Податку',
    },
  },

  loginByLinkPage: {
    loading: () => 'Ми перевіряємо ваш токен доступу...',
    error: () => 'Токен доступу недійсний. Будь ласка, запросіть нове посилання.',
    success: () => 'Доступ надано. Перенаправлення до вашого робочого простору...',
  },

  infra: {
    sessionExpired: () => 'Ваша сесія минула. Будь ласка, увійдіть знову.',
  },
};
