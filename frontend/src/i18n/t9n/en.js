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
      yes: 'Yes',
      no: 'No',
    },
    percent: '{0, number, percent}',
  },

  loginPage: {
    userName: {
      placeholder: 'Login',
    },
  },

  navigationMenu: {
    dashboard: 'Dashboard',
    expenses: 'Expenses',
    incomes: 'Incomes',
    invoices: 'Invoices',
    taxPayments: 'Income Tax Payments',
    reporting: 'Reporting',
    settings: {
      header: 'Settings',
      customers: 'Customers',
      categories: 'Categories',
      generalTaxes: 'General Taxes',
      workspaces: 'Workspaces',
    },
    user: {
      header: 'User',
      profile: 'My Profile',
      logout: 'Logout',
    },
  },

  saDocumentDownloadLink: {
    label: 'Download',
  },

  saDocumentUpload: {
    fileSelector: {
      message: 'Drop file here or click to upload',
      hint: 'Files up to {0, fileSize, pretty} are allowed',
    },
    uploadStatusMessage: {
      error: 'Upload failed, please try again',
      uploading: 'Uploading...',
      scheduled: 'New document to be uploaded',
    },
  },

  saDocument: {
    size: {
      label: '({0, fileSize, pretty})',
    },
  },

  dashboard: {
    header: 'Dashboard',
  },

  editInvoice: {
    recordedOn: 'Recorded on {0, saDateTime, medium}',
    cancelledOn: 'Cancelled on {0, date, medium}',
  },

  editExpense: {
    pageHeader: {
      edit: 'Edit Expense',
      create: 'Record New Expense',
    },
    generalInformation: {
      header: 'General Information',
      category: {
        label: 'Category',
        placeholder: 'Select a category',
      },
      title: {
        label: 'Description / Title',
        placeholder: 'Provide a short summary',
      },
      currency: {
        label: 'Currency',
      },
      originalAmount: {
        label: 'Original Amount',
      },
      datePaid: {
        label: 'Date Paid',
        placeholder: 'Date expense is paid',
      },
      convertedAmountInDefaultCurrency: {
        label: 'Amount in {0}',
      },
      useDifferentExchangeRateForIncomeTaxPurposes: {
        label: 'Using different exchange rate for taxation purposes',
      },
      incomeTaxableAmountInDefaultCurrency: {
        label: 'Amount in {0} for taxation purposes',
      },
      generalTax: {
        label: 'Included General Tax',
        placeholder: 'Select a tax',
      },
      partialForBusiness: {
        label: 'Partial Business Purpose',
      },
      percentOnBusiness: {
        label: '% related to business activities',
      },
    },
    additionalInformation: {
      header: 'Additional Information',
      notes: {
        label: 'Notes',
        placeholder: 'Any additional information to be stored for this expense record',
      },
    },
    attachments: {
      header: 'Attachments',
    },
    cancel: 'Cancel',
    save: 'Save',
    validations: {
      currency: 'Please select a currency',
      title: 'Please provide the title',
      datePaid: 'Please provide the date when expense is paid',
      originalAmount: 'Please provide expense amount',
    },
    documentsUploadFailure: 'Some of the documents have not been uploaded. Please retry or remove them.',
  },

  expensesOverviewPanel: {
    datePaid: {
      tooltip: 'Date Paid',
      label: 'Date Paid',
    },
    notes: {
      tooltip: 'Additional notes provided',
      header: 'Additional Notes',
    },
    generalTax: {
      tooltip: 'General Tax applied',
      label: 'Applicable General Tax',
    },
    generalTaxRate: {
      label: 'Applicable General Tax Rate',
      value: '{0, bps, percent}',
    },
    generalTaxAmount: {
      label: 'Applicable General Tax Amount',
      notProvided: 'Not yet available',
    },
    attachments: {
      tooltip: 'Attachments provided',
      header: 'Attachments',
    },
    foreignCurrency: {
      tooltip: 'In foreign currency',
      header: 'Currency Conversion',
    },
    partialBusinessPurpose: {
      tooltip: 'Partial business purpose',
      label: 'Partial Business Purpose',
      value: '{0, number, percent} related to business activities',
    },
    copy: 'Copy',
    edit: 'Edit',
    summary: {
      header: 'Summary',
    },
    status: {
      label: 'Status',
      short: {
        finalized: 'Finalized',
        pending: 'Pending',
      },
      full: {
        finalized: 'Finalized',
        pendingConversion: 'Conversion to {0} pending',
        waitingExchangeRate: 'Waiting for exchange rate',
      },
    },
    category: {
      label: 'Category',
    },
    incomeTaxableAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Amount in {0} for taxation purposes',
        notProvided: 'Not yet available',
      },
      adjustedAmountInDefaultCurrency: {
        label: 'Amount for Taxation Purposes',
        notProvided: 'Not yet provided',
      },
    },
    convertedAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Amount in {0}',
        notProvided: 'Not yet available',
      },
    },
    generalInformation: {
      header: 'General Information',
    },
    originalCurrency: {
      label: 'Original Currency',
    },
    originalAmount: {
      label: 'Original Amount',
    },
    differentExchangeRate: {
      label: 'Using different exchange rate for taxation purposes?',
      value: '{0, yesNo}',
    },
  },

  expensesOverview: {
    header: 'Expenses',
    filters: {
      announcement: 'Filters coming soon',
      input: {
        placeholder: 'Search expenses',
      },
    },
    create: 'Add new',
  },

  editIncomeTaxPayment: {
    header: {
      edit: 'Edit Income Tax Payment',
      create: 'Record New Income Tax Payment',
    },
    generalInformation: {
      header: 'General Information',

      title: {
        label: 'Description / Title',
        placeholder: 'Provide a short summary',
      },
      amount: {
        label: 'Amount',
      },
      datePaid: {
        label: 'Date Paid',
        placeholder: 'Date tax is paid',
      },
      reportingDate: {
        label: 'Reporting Date',
        placeholder: 'Date to include this payment into reporting by',
      },
    },
    additionalInformation: {
      header: 'Additional notes',
      notes: {
        label: 'Notes',
        placeholder: 'Any additional information to be stored for this tax payment record',
      },
    },
    attachments: {
      header: 'Attachments',
    },
    cancel: 'Cancel',
    save: 'Save',
    validations: {
      title: 'Please provide the title',
      datePaid: 'Please provide the date when tax payment is done',
      amount: 'Please provide tax payment amount',
    },
    uploadFailure: 'Some of the documents have not been uploaded. Please retry or remove them.',
  },

  incomeTaxPaymentsOverviewPanel: {
    datePaid: {
      label: 'Date paid',
    },
    notes: {
      tooltip: 'Additional notes provided',
      header: 'Additional Notes',
    },
    attachments: {
      tooltip: 'Attachments provided',
      header: 'Attachments',
    },
    edit: 'Edit',
    summary: {
      header: 'Summary',
    },
    reportingDate: {
      label: 'Reporting Date',
    },
  },

  incomeTaxPaymentsOverview: {
    header: 'Income Tax Payments',
    filters: {
      announcement: 'Filters coming soon',
    },
    create: 'Add new',
  },

  myProfile: {
    languagePreferences: {
      header: 'Language Preferences',
      language: {
        label: 'Interface Language',
        placeholder: 'Please select interface language',
      },
      locale: {
        label: 'Language to display dates, amounts, etc',
        placeholder: 'Please select formatting language',
      },
    },
  },

  editIncome: {
    pageHeader: {
      edit: 'Edit Income',
      create: 'Record New Income',
    },
    generalInformation: {
      header: 'General Information',
      category: {
        label: 'Category',
        placeholder: 'Select a category',
      },
      title: {
        label: 'Description / Title',
        placeholder: 'Provide a short summary',
      },
      currency: {
        label: 'Currency',
      },
      originalAmount: {
        label: 'Amount',
      },
      dateReceived: {
        label: 'Date Received',
        placeholder: 'Date income is received',
      },
      convertedAmountInDefaultCurrency: {
        label: 'Amount in {0}',
      },
      useDifferentExchangeRateForIncomeTaxPurposes: {
        label: 'Using different exchange rate for taxation purposes',
      },
      incomeTaxableAmountInDefaultCurrency: {
        label: 'Amount in {0} for taxation purposes',
      },
      generalTax: {
        label: 'Included General Tax',
        placeholder: 'Select a tax',
      },
    },
    additionalInformation: {
      header: 'Additional Information',
      linkedInvoice: {
        label: 'Linked Invoice',
      },
      notes: {
        label: 'Notes',
        placeholder: 'Any additional information to be stored for this income record',
      },
    },
    attachments: {
      header: 'Attachments',
    },
    cancel: 'Cancel',
    save: 'Save',
    validations: {
      currency: 'Please select a currency',
      title: 'Please provide the title',
      dateReceived: 'Please provide the date when income is received',
      originalAmount: 'Please provide income amount',
    },
    documentsUploadFailure: 'Some of the documents have not been uploaded. Please retry or remove them.',
  },

  incomesOverviewPanel: {
    dateReceived: {
      tooltip: 'Date received',
      label: 'Date Received',
    },
    notes: {
      tooltip: 'Additional notes provided',
      header: 'Additional Notes',
    },
    generalTax: {
      tooltip: 'General Tax applied',
      label: 'Applicable General Tax',
    },
    generalTaxRate: {
      label: 'Applicable General Tax Rate',
      value: '{0, bps, percent}',
    },
    generalTaxAmount: {
      label: 'Applicable General Tax Amount',
      notProvided: 'Not yet available',
    },
    attachments: {
      tooltip: 'Attachments provided',
      header: 'Attachments',
    },
    foreignCurrency: {
      tooltip: 'In foreign currency',
      header: 'Currency Conversion',
    },
    linkedInvoice: {
      label: 'Associated Invoice',
      tooltip: 'Invoice associated',
    },
    edit: 'Edit',
    summary: {
      header: 'Summary',
    },
    status: {
      label: 'Status',
      short: {
        finalized: 'Finalized',
        pending: 'Pending',
      },
      full: {
        finalized: 'Finalized',
        pendingConversion: 'Conversion to {0} pending',
        waitingExchangeRate: 'Waiting for exchange rate',
      },
    },
    category: {
      label: 'Category',
    },
    incomeTaxableAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Amount in {0} for taxation purposes',
        notProvided: 'Not yet available',
      },
      adjustedAmountInDefaultCurrency: {
        label: 'Amount for Taxation Purposes',
        notProvided: 'Not yet provided',
      },
    },
    convertedAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Amount in {0}',
        notProvided: 'Not yet available',
      },
    },
    generalInformation: {
      header: 'General Information',
    },
    originalCurrency: {
      label: 'Original Currency',
    },
    originalAmount: {
      label: 'Original Amount',
    },
    differentExchangeRate: {
      label: 'Using different exchange rate for taxation purposes?',
      value: '{0, yesNo}',
    },
  },

  el: {
    datepicker: {
      now: 'Now',
      today: 'Today',
      cancel: 'Cancel',
      clear: 'Clear',
      confirm: 'OK',
      selectDate: 'Select date',
      selectTime: 'Select time',
      startDate: 'Start Date',
      startTime: 'Start Time',
      endDate: 'End Date',
      endTime: 'End Time',
      prevYear: 'Previous Year',
      nextYear: 'Next Year',
      prevMonth: 'Previous Month',
      nextMonth: 'Next Month',
      year: '',
      month1: 'January',
      month2: 'February',
      month3: 'March',
      month4: 'April',
      month5: 'May',
      month6: 'June',
      month7: 'July',
      month8: 'August',
      month9: 'September',
      month10: 'October',
      month11: 'November',
      month12: 'December',
      week: 'week',
      weeks: {
        sun: 'Sun',
        mon: 'Mon',
        tue: 'Tue',
        wed: 'Wed',
        thu: 'Thu',
        fri: 'Fri',
        sat: 'Sat',
      },
      months: {
        jan: 'Jan',
        feb: 'Feb',
        mar: 'Mar',
        apr: 'Apr',
        may: 'May',
        jun: 'Jun',
        jul: 'Jul',
        aug: 'Aug',
        sep: 'Sep',
        oct: 'Oct',
        nov: 'Nov',
        dec: 'Dec',
      },
    },
    select: {
      loading: 'Loading',
      noMatch: 'No matching data',
      noData: 'No data',
      placeholder: 'Select',
    },
    pagination: {
      goto: 'Go to',
      pagesize: '/page',
      total: 'Total {total}',
      pageClassifier: '',
    },
    table: {
      emptyText: 'No Data',
      confirmFilter: 'Confirm',
      resetFilter: 'Reset',
      clearFilter: 'All',
      sumText: 'Sum',
    },
  },
};
