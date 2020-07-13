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
    announcement: 'New here? We are launching public access soon.',
    userName: {
      placeholder: 'Login',
    },
    password: {
      placeholder: 'Password',
    },
    rememberMe: {
      label: 'Remember me for 30 days',
    },
    login: 'Login',
    loginError: {
      underAttack: 'Looks like your account is under attack!',
      generalFailure: 'Login attempt failed. Please make sure login and password is correct',
      accountLocked: 'Account is temporary locked. It will be unlocked in {0, duration}',
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
    creatingLinkMessage: 'Starting the download, please wait...',
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
    fromInvoice: {
      title: 'Payment for {0}',
    },
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

  incomesOverview: {
    header: 'Incomes',
    filters: {
      announcement: 'Filters coming soon',
      input: {
        placeholder: 'Search incomes',
      },
    },
    create: 'Add new',
  },

  editInvoice: {
    recordedOn: 'Recorded on {0, saDateTime, medium}',
    cancelledOn: 'Cancelled on {0, date, medium}',
    cancelInvoice: {
      button: 'Cancel Invoice',
      confirm: {
        message: 'This will permanently cancel this invoice. Continue?',
        yes: 'Yes',
        no: 'No',
      },
    },
    pageHeader: {
      edit: 'Edit Invoice',
      create: 'Create New Invoice',
    },
    generalInformation: {
      header: 'General Information',
      customer: {
        label: 'Customer',
        placeholder: 'Select a customer',
      },
      title: {
        label: 'Description / Title',
        placeholder: 'Provide a short summary',
      },
      currency: {
        label: 'Currency',
      },
      amount: {
        label: 'Amount',
      },
      dateIssued: {
        label: 'Date Issued',
        placeholder: 'Date invoice is issued',
      },
      dueDate: {
        label: 'Due Date',
        placeholder: 'Date invoice is due',
      },
      alreadySent: {
        label: 'Already Sent',
      },
      dateSent: {
        label: 'Date Sent',
        placeholder: 'Date invoice is sent',
      },
      alreadyPaid: {
        label: 'Already Paid',
      },
      datePaid: {
        label: 'Date Paid',
        placeholder: 'Date invoice is paid',
      },
      generalTax: {
        label: 'Included General Tax',
        placeholder: 'Select a tax',
      },
    },
    additionalNotes: {
      header: 'Additional notes',
      notes: {
        label: 'Notes',
        placeholder: 'Any additional information to be stored for this invoice record',
      },
    },
    attachments: {
      header: 'Attachments',
    },
    cancel: 'Cancel',
    save: 'Save',
    validations: {
      customer: 'Please select a customer',
      currency: 'Please select a currency',
      title: 'Please provide the title',
      amount: 'Please provide invoice amount',
      dateIssued: 'Please provide the date when invoice is issued',
      dueDate: 'Please provide the date when invoice is due',
      dateSent: 'Please provide the date when invoice is sent',
      datePaid: 'Please provide the date when invoice is paid',
    },
  },

  invoicesOverview: {
    header: 'Invoices',
    filters: {
      announcement: 'Filters coming soon',
      input: {
        placeholder: 'Search invoices',
      },
    },
    create: 'Add new',
  },

  invoicesOverviewPanel: {
    customer: {
      tooltip: 'Customer',
      label: 'Customer',
    },
    datePaid: {
      tooltip: 'Date paid',
      label: 'Date Paid',
    },
    notes: {
      tooltip: 'Additional notes provided',
      header: 'Additional Notes',
    },
    attachments: {
      tooltip: 'Attachments provided',
      header: 'Attachments',
    },
    generalTax: {
      tooltip: 'General Tax applied',
      label: 'Applicable General Tax',
    },
    generalTaxRate: {
      label: 'Applicable General Tax Rate',
      value: '{0, bps, percent}',
    },
    foreignCurrency: {
      tooltip: 'In foreign currency',
    },
    edit: 'Edit',
    markAsSent: 'Sent today',
    markAsPaid: 'Paid today',
    generalInformation: {
      header: 'General Information',
    },
    status: {
      label: 'Status',
      finalized: 'Finalized',
      draft: 'Draft',
      cancelled: 'Cancelled',
      sent: 'Sent',
      overdue: 'Overdue',
    },
    currency: {
      label: 'Invoice Currency',
    },
    amount: {
      label: 'Invoice Amount',
    },
    dateIssued: {
      label: 'Date Issued',
    },
    dueDate: {
      label: 'Due Date',
    },
    dateSent: {
      label: 'Date Sent',
    },
    dateCancelled: {
      label: 'Date Cancelled',
    },
  },

  saCurrencyInput: {
    groups: {
      recent: 'Recently Used Currencies',
      all: 'All Currencies',
    },
    currencyLabel: '{code} - {displayName}',
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
    messagebox: {
      confirm: 'OK',
    },
  },

  useDocumentsUpload: {
    documentsUploadFailure: 'Some of the documents have not been uploaded. Please retry or remove them.',
  },

  saGoogleDriveIntegrationSetup: {
    successful: {
      status: 'Google Drive integration is active',
      details: 'All documents are stored in {folderLink} folder',
    },
    unknown: {
      status: 'Verifying integration status...',
      details: 'Please hold on while we are checking the status',
    },
    authorizationRequired: {
      status: 'Authorization required',
      details: {
        message: 'Please authorize the application to store documents in your Google Drive.{action}',
        startAction: 'Start authorization now',
      },
    },
    authorizationInProgress: {
      status: 'Authorization in progress...',
      details: {
        line1: 'Please continue the authorization in the popup browser window.',
        line2: 'Once finished, we will automatically update the status here.',
      },
    },
    authorizationFailed: {
      status: 'Application authorization failed',
      details: {
        message: 'We could not complete the authorization, sorry :( {action}',
        retryAction: 'Try again',
      },
    },
  },

  saFailedDocumentsStorageMessage: {
    title: 'Documents storage is not active',
    message: 'Please navigate to your {0} and complete the configuration.',
    profileLink: 'profile settings',
  },

  errorHandler: {
    fatalApiError: 'We encountered a server error. Please try again.',
  },

  saBasicErrorMessage: {
    defaultMessage: 'An error happened. Please try again later.',
  },

  saEntitySelect: {
    loading: {
      text: 'Loading...',
    },
    noData: {
      text: 'No data found',
    },
    moreElements: {
      text: '{0} more items..',
    },
  },
};
