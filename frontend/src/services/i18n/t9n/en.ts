/* eslint-disable vue/max-len */
import { format } from './formatter';

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
      yes: () => 'Yes',
      no: () => 'No',
    },
    percent: (value: number) => format('{0, number, :: percent scale/100}', [value]),
    cancel: () => 'Cancel',
    save: () => 'Save',
  },

  loginPage: {
    announcement: () => 'New here? We are launching public access soon.',
    userName: {
      placeholder: () => 'Login',
    },
    password: {
      placeholder: () => 'Password',
    },
    rememberMe: {
      label: () => 'Remember me for 30 days',
    },
    login: () => 'Login',
    loginError: {
      underAttack: () => 'Looks like your account is under attack!',
      generalFailure: () => 'Login attempt failed. Please make sure login and password is correct',
      accountLocked: (lockDurationInSec: number) => format('Account is temporary locked. It will be unlocked in {0, duration}', [lockDurationInSec]),
      userNotActivated: () => 'Your account is not yet activated. Please use the token shared with you by the administrators. Contact them if you need to reset the token',
    },
  },

  navigationMenu: {
    dashboard: () => 'Dashboard',
    expenses: () => 'Expenses',
    incomes: () => 'Incomes',
    invoices: () => 'Invoices',
    taxPayments: () => 'Income Tax Payments',
    reporting: () => 'Reporting',
    settings: {
      header: () => 'Settings',
      customers: () => 'Customers',
      categories: () => 'Categories',
      generalTaxes: () => 'General Taxes',
      workspaces: () => 'Workspaces',
    },
    user: {
      header: () => 'User',
      profile: () => 'My Profile',
      logout: () => 'Logout',
    },
    admin: {
      users: () => 'Users',
    },
  },

  saDocumentDownloadLink: {
    label: () => 'Download',
    creatingLinkMessage: () => 'Starting the download, please wait...',
  },

  saDocumentUpload: {
    fileSelector: {
      message: () => 'Drop file here or click to upload',
      hint: (fileSizeInBytes: number) => format('Files up to {0, fileSize, pretty} are allowed', [fileSizeInBytes]),
    },
    uploadStatusMessage: {
      error: () => 'Upload failed, please try again',
      uploading: () => 'Uploading...',
      scheduled: () => 'New document to be uploaded',
    },
  },

  saDocument: {
    size: {
      label: (fileSizeInBytes: number) => format('({0, fileSize, pretty})', [fileSizeInBytes]),
    },
  },

  dashboard: {
    header: () => 'Dashboard',
    dateRange: {
      separator: () => 'To',
      startPlaceholder: () => 'Start date',
      endPlaceholder: () => 'End date',
    },
    cards: {
      expenses: {
        totalHeader: (count: number) => format('Total of {0, number} expenses', [count]),
        pendingHeader: (count: number) => format('Pending {0, number} more', [count]),
      },
      incomes: {
        totalHeader: (count: number) => format('Total of {0, number} incomes', [count]),
        pendingHeader: (count: number) => format('Pending {0, number} more', [count]),
      },
      profit: {
        taxableAmount: () => 'Taxable Amount',
        currencyExchangeDifference: () => 'Currency exchange rate difference',
        incomeTaxPayments: () => 'Income Tax Payments',
        estimatedTax: () => 'Estimated Tax',
        estimatedTaxPlaceholder: () => 'coming soon..',
        profit: () => 'Profit',
      },
      invoice: {
        to: () => 'To',
        issueDate: () => 'Issue Date',
        dateSent: () => 'Date Sent',
        dueDate: () => 'Due Date',
        status: {
          overdue: () => 'Overdue',
          pending: () => 'Pending',
        },
      },
    },
  },

  editExpense: {
    pageHeader: {
      edit: () => 'Edit Expense',
      create: () => 'Record New Expense',
    },
    generalInformation: {
      header: () => 'General Information',
      category: {
        label: () => 'Category',
        placeholder: () => 'Select a category',
      },
      title: {
        label: () => 'Description / Title',
        placeholder: () => 'Provide a short summary',
      },
      currency: {
        label: () => 'Currency',
      },
      originalAmount: {
        label: () => 'Original Amount',
      },
      datePaid: {
        label: () => 'Date Paid',
        placeholder: () => 'Date expense is paid',
      },
      convertedAmountInDefaultCurrency: {
        label: (currency: string) => format('Amount in {0}', [currency]),
      },
      useDifferentExchangeRateForIncomeTaxPurposes: {
        label: () => 'Using different exchange rate for taxation purposes',
      },
      incomeTaxableAmountInDefaultCurrency: {
        label: (currency: string) => format('Amount in {0} for taxation purposes', [currency]),
      },
      generalTax: {
        label: () => 'Included General Tax',
        placeholder: () => 'Select a tax',
      },
      partialForBusiness: {
        label: () => 'Partial Business Purpose',
      },
      percentOnBusiness: {
        label: () => '% related to business activities',
      },
    },
    additionalInformation: {
      header: () => 'Additional Information',
      notes: {
        label: () => 'Notes',
        placeholder: () => 'Any additional information to be stored for this expense record',
      },
    },
    attachments: {
      header: () => 'Attachments',
    },
    cancel: () => 'Cancel',
    save: () => 'Save',
    validations: {
      currency: () => 'Please select a currency',
      title: () => 'Please provide the title',
      datePaid: () => 'Please provide the date when expense is paid',
      originalAmount: () => 'Please provide expense amount',
    },
  },

  expensesOverviewPanel: {
    datePaid: {
      tooltip: () => 'Date Paid',
      label: () => 'Date Paid',
    },
    notes: {
      tooltip: () => 'Additional notes provided',
      header: () => 'Additional Notes',
    },
    generalTax: {
      tooltip: () => 'General Tax applied',
      label: () => 'Applicable General Tax',
    },
    generalTaxRate: {
      label: () => 'Applicable General Tax Rate',
      value: (bps: number) => format('{0, bps, percent}', [bps]),
    },
    generalTaxAmount: {
      label: () => 'Applicable General Tax Amount',
      notProvided: () => 'Not yet available',
    },
    attachments: {
      tooltip: () => 'Attachments provided',
      header: () => 'Attachments',
    },
    foreignCurrency: {
      tooltip: () => 'In foreign currency',
      header: () => 'Currency Conversion',
    },
    partialBusinessPurpose: {
      tooltip: () => 'Partial business purpose',
      label: () => 'Partial Business Purpose',
      value: (value: number) => format('{0, number, percent} related to business activities', [value]),
    },
    copy: () => 'Copy',
    edit: () => 'Edit',
    summary: {
      header: () => 'Summary',
    },
    status: {
      label: () => 'Status',
      short: {
        finalized: () => 'Finalized',
        pending: () => 'Pending',
      },
      full: {
        finalized: () => 'Finalized',
        pendingConversion: (currency: string) => format('Conversion to {0} pending', [currency]),
        waitingExchangeRate: () => 'Waiting for exchange rate',
      },
    },
    category: {
      label: () => 'Category',
    },
    incomeTaxableAmounts: {
      originalAmountInDefaultCurrency: {
        label: (currency: string) => format('Amount in {0} for taxation purposes', [currency]),
        notProvided: () => 'Not yet available',
      },
      adjustedAmountInDefaultCurrency: {
        label: () => 'Amount for Taxation Purposes',
        notProvided: () => 'Not yet provided',
      },
    },
    convertedAmounts: {
      originalAmountInDefaultCurrency: {
        label: (currency: string) => format('Amount in {0}', [currency]),
        notProvided: () => 'Not yet available',
      },
    },
    generalInformation: {
      header: () => 'General Information',
    },
    originalCurrency: {
      label: () => 'Original Currency',
    },
    originalAmount: {
      label: () => 'Original Amount',
    },
    differentExchangeRate: {
      label: () => 'Using different exchange rate for taxation purposes?',
      value: (value: boolean) => format('{0, yesNo}', [value]),
    },
  },

  expensesOverview: {
    header: () => 'Expenses',
    filters: {
      announcement: () => 'Filters coming soon',
      input: {
        placeholder: () => 'Search expenses',
      },
    },
    create: () => 'Add new',
  },

  editIncomeTaxPayment: {
    header: {
      edit: () => 'Edit Income Tax Payment',
      create: () => 'Record New Income Tax Payment',
    },
    generalInformation: {
      header: () => 'General Information',

      title: {
        label: () => 'Description / Title',
        placeholder: () => 'Provide a short summary',
      },
      amount: {
        label: () => 'Amount',
      },
      datePaid: {
        label: () => 'Date Paid',
        placeholder: () => 'Date tax is paid',
      },
      reportingDate: {
        label: () => 'Reporting Date',
        placeholder: () => 'Date to include this payment into reporting by',
      },
    },
    additionalInformation: {
      header: () => 'Additional notes',
      notes: {
        label: () => 'Notes',
        placeholder: () => 'Any additional information to be stored for this tax payment record',
      },
    },
    attachments: {
      header: () => 'Attachments',
    },
    cancel: () => 'Cancel',
    save: () => 'Save',
    validations: {
      title: () => 'Please provide the title',
      datePaid: () => 'Please provide the date when tax payment is done',
      amount: () => 'Please provide tax payment amount',
    },
  },

  incomeTaxPaymentsOverviewPanel: {
    datePaid: {
      label: () => 'Date paid',
    },
    notes: {
      tooltip: () => 'Additional notes provided',
      header: () => 'Additional Notes',
    },
    attachments: {
      tooltip: () => 'Attachments provided',
      header: () => 'Attachments',
    },
    edit: () => 'Edit',
    summary: {
      header: () => 'Summary',
    },
    reportingDate: {
      label: () => 'Reporting Date',
    },
  },

  incomeTaxPaymentsOverview: {
    header: () => 'Income Tax Payments',
    filters: {
      announcement: () => 'Filters coming soon',
    },
    create: () => 'Add new',
  },

  myProfile: {
    pageHeader: () => 'My Profile',
    documentsStorage: {
      header: () => 'Documents Storage',
      googleDrive: () => 'Google Drive',
    },
    languagePreferences: {
      header: () => 'Language Preferences',
      language: {
        label: () => 'Interface Language',
        placeholder: () => 'Please select interface language',
      },
      locale: {
        label: () => 'Language to display dates, amounts, etc',
        placeholder: () => 'Please select formatting language',
      },
      feedback: {
        success: () => 'Language preferences have been saved',
      },
    },
    changePassword: {
      header: () => 'Change Password',
      currentPassword: {
        label: () => 'Current Password',
        placeholder: () => 'Please provide your current password',
      },
      newPassword: {
        label: () => 'New Password',
        placeholder: () => 'Please provide your new password',
      },
      newPasswordConfirmation: {
        label: () => 'New Password Confirmation',
        placeholder: () => 'Please confirm your new password',
      },
      submit: {
        label: () => 'Apply new password',
      },
      validations: {
        confirmationDoesNotMatch: () => 'New password confirmation does not match',
        currentPasswordMismatch: () => 'Current password does not match',
      },
      feedback: {
        success: () => 'Password has been changed',
      },
    },
  },

  editIncome: {
    pageHeader: {
      edit: () => 'Edit Income',
      create: () => 'Record New Income',
    },
    generalInformation: {
      header: () => 'General Information',
      category: {
        label: () => 'Category',
        placeholder: () => 'Select a category',
      },
      title: {
        label: () => 'Description / Title',
        placeholder: () => 'Provide a short summary',
      },
      currency: {
        label: () => 'Currency',
      },
      originalAmount: {
        label: () => 'Amount',
      },
      dateReceived: {
        label: () => 'Date Received',
        placeholder: () => 'Date income is received',
      },
      convertedAmountInDefaultCurrency: {
        label: (currency: string) => format('Amount in {0}', [currency]),
      },
      useDifferentExchangeRateForIncomeTaxPurposes: {
        label: () => 'Using different exchange rate for taxation purposes',
      },
      incomeTaxableAmountInDefaultCurrency: {
        label: (currency: string) => format('Amount in {0} for taxation purposes', [currency]),
      },
      generalTax: {
        label: () => 'Included General Tax',
        placeholder: () => 'Select a tax',
      },
    },
    additionalInformation: {
      header: () => 'Additional Information',
      linkedInvoice: {
        label: () => 'Linked Invoice',
      },
      notes: {
        label: () => 'Notes',
        placeholder: () => 'Any additional information to be stored for this income record',
      },
    },
    attachments: {
      header: () => 'Attachments',
    },
    cancel: () => 'Cancel',
    save: () => 'Save',
    validations: {
      currency: () => 'Please select a currency',
      title: () => 'Please provide the title',
      dateReceived: () => 'Please provide the date when income is received',
      originalAmount: () => 'Please provide income amount',
    },
    fromInvoice: {
      title: (invoiceTitle: string) => format('Payment for {0}', [invoiceTitle]),
    },
  },

  incomesOverviewPanel: {
    dateReceived: {
      tooltip: () => 'Date received',
      label: () => 'Date Received',
    },
    notes: {
      tooltip: () => 'Additional notes provided',
      header: () => 'Additional Notes',
    },
    generalTax: {
      tooltip: () => 'General Tax applied',
      label: () => 'Applicable General Tax',
    },
    generalTaxRate: {
      label: () => 'Applicable General Tax Rate',
      value: (bps: number) => format('{0, bps, percent}', [bps]),
    },
    generalTaxAmount: {
      label: () => 'Applicable General Tax Amount',
      notProvided: () => 'Not yet available',
    },
    attachments: {
      tooltip: () => 'Attachments provided',
      header: () => 'Attachments',
    },
    foreignCurrency: {
      tooltip: () => 'In foreign currency',
      header: () => 'Currency Conversion',
    },
    linkedInvoice: {
      label: () => 'Associated Invoice',
      tooltip: () => 'Invoice associated',
    },
    edit: () => 'Edit',
    summary: {
      header: () => 'Summary',
    },
    status: {
      label: () => 'Status',
      short: {
        finalized: () => 'Finalized',
        pending: () => 'Pending',
      },
      full: {
        finalized: () => 'Finalized',
        pendingConversion: (currency: string) => format('Conversion to {0} pending', [currency]),
        waitingExchangeRate: () => 'Waiting for exchange rate',
      },
    },
    category: {
      label: () => 'Category',
    },
    incomeTaxableAmounts: {
      originalAmountInDefaultCurrency: {
        label: (currency: string) => format('Amount in {0} for taxation purposes', [currency]),
        notProvided: () => 'Not yet available',
      },
      adjustedAmountInDefaultCurrency: {
        label: () => 'Amount for Taxation Purposes',
        notProvided: () => 'Not yet provided',
      },
    },
    convertedAmounts: {
      originalAmountInDefaultCurrency: {
        label: (currency: string) => format('Amount in {0}', [currency]),
        notProvided: () => 'Not yet available',
      },
    },
    generalInformation: {
      header: () => 'General Information',
    },
    originalCurrency: {
      label: () => 'Original Currency',
    },
    originalAmount: {
      label: () => 'Original Amount',
    },
    differentExchangeRate: {
      label: () => 'Using different exchange rate for taxation purposes?',
      value: (value: boolean) => format('{0, yesNo}', [value]),
    },
  },

  incomesOverview: {
    header: () => 'Incomes',
    filters: {
      announcement: () => 'Filters coming soon',
      input: {
        placeholder: () => 'Search incomes',
      },
    },
    create: () => 'Add new',
  },

  editInvoice: {
    cancelInvoice: {
      button: () => 'Cancel Invoice',
      confirm: {
        message: () => 'This will permanently cancel this invoice. Continue?',
        yes: () => 'Yes',
        no: () => 'No',
      },
      status: () => 'This invoice has been cancelled',
    },
    pageHeader: {
      edit: () => 'Edit Invoice',
      create: () => 'Create New Invoice',
    },
    generalInformation: {
      header: () => 'General Information',
      customer: {
        label: () => 'Customer',
        placeholder: () => 'Select a customer',
      },
      title: {
        label: () => 'Description / Title',
        placeholder: () => 'Provide a short summary',
      },
      currency: {
        label: () => 'Currency',
      },
      amount: {
        label: () => 'Amount',
      },
      dateIssued: {
        label: () => 'Date Issued',
        placeholder: () => 'Date invoice is issued',
      },
      dueDate: {
        label: () => 'Due Date',
        placeholder: () => 'Date invoice is due',
      },
      alreadySent: {
        label: () => 'Already Sent',
      },
      dateSent: {
        label: () => 'Date Sent',
        placeholder: () => 'Date invoice is sent',
      },
      alreadyPaid: {
        label: () => 'Already Paid',
      },
      datePaid: {
        label: () => 'Date Paid',
        placeholder: () => 'Date invoice is paid',
      },
      generalTax: {
        label: () => 'Included General Tax',
        placeholder: () => 'Select a tax',
      },
    },
    additionalNotes: {
      header: () => 'Additional notes',
      notes: {
        label: () => 'Notes',
        placeholder: () => 'Any additional information to be stored for this invoice record',
      },
    },
    attachments: {
      header: () => 'Attachments',
    },
    cancel: () => 'Cancel',
    save: () => 'Save',
    validations: {
      customer: () => 'Please select a customer',
      currency: () => 'Please select a currency',
      title: () => 'Please provide the title',
      amount: () => 'Please provide invoice amount',
      dateIssued: () => 'Please provide the date when invoice is issued',
      dueDate: () => 'Please provide the date when invoice is due',
      dateSent: () => 'Please provide the date when invoice is sent',
      datePaid: () => 'Please provide the date when invoice is paid',
    },
  },

  invoicesOverview: {
    header: () => 'Invoices',
    filters: {
      announcement: () => 'Filters coming soon',
      input: {
        placeholder: () => 'Search invoices',
      },
    },
    create: () => 'Add new',
  },

  invoicesOverviewPanel: {
    customer: {
      tooltip: () => 'Customer',
      label: () => 'Customer',
    },
    datePaid: {
      tooltip: () => 'Date paid',
      label: () => 'Date Paid',
    },
    notes: {
      tooltip: () => 'Additional notes provided',
      header: () => 'Additional Notes',
    },
    attachments: {
      tooltip: () => 'Attachments provided',
      header: () => 'Attachments',
    },
    generalTax: {
      tooltip: () => 'General Tax applied',
      label: () => 'Applicable General Tax',
    },
    generalTaxRate: {
      label: () => 'Applicable General Tax Rate',
      value: (bps: number) => format('{0, bps, percent}', [bps]),
    },
    foreignCurrency: {
      tooltip: () => 'In foreign currency',
    },
    edit: () => 'Edit',
    markAsSent: () => 'Sent today',
    markAsPaid: () => 'Paid',
    generalInformation: {
      header: () => 'General Information',
    },
    status: {
      label: () => 'Status',
      finalized: () => 'Finalized',
      draft: () => 'Draft',
      cancelled: () => 'Cancelled',
      sent: () => 'Sent',
      overdue: () => 'Overdue',
    },
    currency: {
      label: () => 'Invoice Currency',
    },
    amount: {
      label: () => 'Invoice Amount',
    },
    dateIssued: {
      label: () => 'Date Issued',
    },
    dueDate: {
      label: () => 'Due Date',
    },
    dateSent: {
      label: () => 'Date Sent',
    },
  },

  saCurrencyInput: {
    groups: {
      recent: () => 'Recently Used Currencies',
      all: () => 'All Currencies',
    },
    currencyLabel: ({
      code,
      displayName,
    }: { code: string, displayName: string }) => `${code} - ${displayName}`,
  },

  el: {
    datepicker: {
      now: () => 'Now',
      today: () => 'Today',
      cancel: () => 'Cancel',
      clear: () => 'Clear',
      confirm: () => 'OK',
      selectDate: () => 'Select date',
      selectTime: () => 'Select time',
      startDate: () => 'Start Date',
      startTime: () => 'Start Time',
      endDate: () => 'End Date',
      endTime: () => 'End Time',
      prevYear: () => 'Previous Year',
      nextYear: () => 'Next Year',
      prevMonth: () => 'Previous Month',
      nextMonth: () => 'Next Month',
      year: () => '',
      month1: () => 'January',
      month2: () => 'February',
      month3: () => 'March',
      month4: () => 'April',
      month5: () => 'May',
      month6: () => 'June',
      month7: () => 'July',
      month8: () => 'August',
      month9: () => 'September',
      month10: () => 'October',
      month11: () => 'November',
      month12: () => 'December',
      week: () => 'week',
      weeks: {
        sun: () => 'Sun',
        mon: () => 'Mon',
        tue: () => 'Tue',
        wed: () => 'Wed',
        thu: () => 'Thu',
        fri: () => 'Fri',
        sat: () => 'Sat',
      },
      months: {
        jan: () => 'Jan',
        feb: () => 'Feb',
        mar: () => 'Mar',
        apr: () => 'Apr',
        may: () => 'May',
        jun: () => 'Jun',
        jul: () => 'Jul',
        aug: () => 'Aug',
        sep: () => 'Sep',
        oct: () => 'Oct',
        nov: () => 'Nov',
        dec: () => 'Dec',
      },
    },
    select: {
      loading: () => 'Loading',
      noMatch: () => 'No matching data',
      noData: () => 'No data',
      placeholder: () => 'Select',
    },
    pagination: {
      goto: () => 'Go to',
      pagesize: () => '/page',
      total: () => 'Total {total}',
      pageClassifier: () => '',
    },
    table: {
      emptyText: () => 'No Data',
      confirmFilter: () => 'Confirm',
      resetFilter: () => 'Reset',
      clearFilter: () => 'All',
      sumText: () => 'Sum',
    },
    messagebox: {
      confirm: () => 'OK',
    },
  },

  useDocumentsUpload: {
    documentsUploadFailure: () => 'Some of the documents have not been uploaded. Please retry or remove them.',
  },

  saGoogleDriveIntegrationSetup: {
    successful: {
      status: () => 'Google Drive integration is active',
      details: () => 'All documents are stored in {folderLink} folder',
    },
    unknown: {
      status: () => 'Verifying integration status...',
      details: () => 'Please hold on while we are checking the status',
    },
    authorizationRequired: {
      status: () => 'Authorization required',
      details: {
        message: () => 'Please authorize the application to store documents in your Google Drive.{action}',
        startAction: () => 'Start authorization now',
      },
    },
    authorizationInProgress: {
      status: () => 'Authorization in progress...',
      details: {
        line1: () => 'Please continue the authorization in the popup browser window.',
        line2: () => 'Once finished, we will automatically update the status here.',
      },
    },
    authorizationFailed: {
      status: () => 'Application authorization failed',
      details: {
        message: () => 'We could not complete the authorization, sorry :( {action}',
        retryAction: () => 'Try again',
      },
    },
  },

  saFailedDocumentsStorageMessage: {
    title: () => 'Documents storage is not active',
    message: () => 'Please navigate to your {default} and complete the configuration.',
    profileLink: () => 'profile settings',
  },

  errorHandler: {
    fatalErrorMessage: () => 'We encountered a technical error. Please consider reporting the problem and refresh the page to try again.',
  },

  saBasicErrorMessage: {
    defaultMessage: () => 'An error happened. Please try again later.',
  },

  saEntitySelect: {
    loading: {
      text: () => 'Loading...',
    },
    noData: {
      text: () => 'No data found',
    },
    moreElements: {
      text: (count: number) => format('{0} more items..', [count]),
    },
  },

  saInvoiceSelect: {
    placeholder: () => 'Select or search for invoice',
  },

  usersOverview: {
    header: () => 'Users',
    filters: {
      announcement: () => 'Filters coming soon',
      input: {
        placeholder: () => 'Search users',
      },
    },
    create: () => 'Create user',
  },

  adminOverviewPanel: {
    userTypeAdmin: () => 'Admin user',
    userTypeRegular: () => 'User',
    userActivated: () => 'Active',
    userNotActivated: () => 'Not yet activated',
    edit: () => 'Edit',
  },

  formValidationMessages: {
    sizeMinMax: (min: number, max: number) => format('The length of this value should be not shorter than {min, number} and no longer than {max, number} characters', {
      min,
      max,
    }),
    sizeMax: (max: number) => format('The length of this value should be no longer than {max, number} characters', {
      max,
    }),
    notBlank: () => 'This value is required and should not be blank',
  },

  accountActivationPage: {
    loading: () => 'We are verifying your token...',
    badToken: () => 'Provided token is invalid or expired. Please request a new one.',
    instructions: () => 'Please provide your new password. You will then need to login using your username and new password.',
    form: {
      password: {
        label: () => 'New Password',
        placeholder: () => 'Please provide your new password',
      },
      passwordConfirmation: {
        label: () => 'New Password Confirmation',
        placeholder: () => 'Please confirm your new password',
        notMatchingError: () => 'Passwords do not match',
      },
      submit: () => 'Activate Account',
    },
    success: {
      message: () => 'Account has been activated. You can now login using your credentials.',
      button: () => 'Login now',
    },
  },

  editUser: {
    pageHeader: {
      edit: () => 'Edit User',
      create: () => 'Create New User',
    },
    form: {
      userName: {
        label: () => 'Username',
        errors: {
          userAlreadyExists: (userName: string) => format('User with username "{0}" already exists', [userName]),
        },
      },
      role: {
        label: () => 'User role',
        options: {
          user: () => 'User',
          admin: () => 'Admin user',
        },
      },
      activationStatus: {
        label: () => 'Activation status',
        activated: () => 'Activated',
        loading: () => 'Loading...',
        notActivated: () => 'User account is not yet activated. Please share the link below with the user so they can setup their password.',
        copied: () => 'Link copied to clipboard',
      },
    },
    successNotification: (userName: string) => format('User {0} has been successfully saved', [userName]),
  },

  saForm: {
    inputValidationFailed: () => 'Some of the fields have not been filled correctly. Please check the form and try again.',
  },

  accountSetup: {
    welcomeMessage: () => 'Welcome to simple-accounting! We now will create a new workspace for you - a place where the data of your business is stored. If you have multiple businesses, each of them can be represented as a separate workspace. You will be able to setup other workspaces, if needed, after the initial configuration.',
    workspaceNameLabel: () => 'Workspace Name',
    workspaceNamePlaceholder: () => 'Provide a name for your workspace',
    defaultCurrencyLabel: () => 'Main (default) Currency',
    defaultCurrencyPlaceholder: () => 'Provide the main currency of this workspace',
    submitButton: () => 'Complete setup',
  },

  reporting: {
    header: () => 'Reporting',
    wizard: {
      steps: {
        selectReport: {
          title: () => 'Select a report',
          description: {
            select: () => 'Please select a report',
            selected: () => 'Tax Report',
            unknown: () => 'Unknown Report o_O',
          },
        },
        selectDates: {
          title: () => 'Select reporting dates',
          description: {
            select: () => 'Please select reporting date range',
            selected: (fromDate: string, toDate: string) => format('{0} to {1}', [fromDate, toDate]),
          },
        },
        viewReport: {
          title: () => 'View the report',
          description: {
            loading: () => 'Loading..',
            ready: () => 'Ready',
          },
        },
      },
      reports: {
        generalTax: {
          title: () => 'General Tax Report',
          description: () => 'Collected and paid general taxes',
        },
      },
      dateRange: {
        separator: () => 'To',
        startPlaceholder: () => 'Start date',
        endPlaceholder: () => 'End date',
      },
      buttons: {
        select: () => 'Select',
        next: () => 'Next',
      },
    },
  },

  saPageableItems: {
    emptyResults: () => 'No results here',
  },

  customersOverview: {
    header: () => 'Customers',
    filters: {
      announcement: () => 'Filters coming soon',
    },
    create: () => 'Add new',
    edit: () => 'Edit',
  },

  generalTaxReport: {
    emptyData: () => 'No data to show',
    columns: {
      tax: () => 'Tax',
      taxableIncome: () => 'Taxable Income',
      taxCollected: () => 'Tax Collected',
      taxPaid: () => 'Tax Paid',
      balance: () => 'Balance',
    },
    sections: {
      collected: () => 'Collected',
      paid: () => 'Paid',
      total: () => 'Total',
    },
    tableColumns: {
      tax: () => 'Tax',
      numberOfItems: () => 'Number of Items',
      itemsAmount: () => 'Items Amount',
      taxAmount: () => 'Tax Amount',
    },
  },

  loginByLinkPage: {
    loading: () => 'We are verifying your access token...',
    error: () => 'The access token is not valid. Please request a new link.',
    success: () => 'Access granted. Redirecting to your workspace...',
  },

  infra: {
    sessionExpired: () => 'Your session has expired. Please login again.',
  },
};
