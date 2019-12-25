// todo #6: a test that bundles contain the same keys
export default {
  common: {
    date: {
      medium: '{0, date, medium}[UK] ',
    },
    dateTime: {
      medium: '{0, saDateTime, medium}[UK] ',
    },
    amount: {
      withCurrency: '{0, amount, withCurrency}[UK] ',
    },
    yesNo: {
      yes: 'Yes[UK] ',
      no: 'No[UK] ',
    },
    percent: '{0, number, percent}[UK] ',
  },

  loginPage: {
    userName: {
      placeholder: 'Login[UK] ',
    },
  },

  navigationMenu: {
    dashboard: 'Dashboard[UK] ',
    expenses: 'Expenses[UK] ',
    incomes: 'Incomes[UK] ',
    invoices: 'Invoices[UK] ',
    taxPayments: 'Income Tax Payments[UK] ',
    reporting: 'Reporting[UK] ',
    settings: {
      header: 'Settings[UK] ',
      customers: 'Customers[UK] ',
      categories: 'Categories[UK] ',
      generalTaxes: 'General Taxes[UK] ',
      workspaces: 'Workspaces[UK] ',
    },
    user: {
      header: 'User[UK] ',
      profile: 'My Profile[UK] ',
      logout: 'Logout[UK] ',
    },
  },

  saDocumentDownloadLink: {
    label: 'Download[UK] ',
  },

  saDocumentUpload: {
    fileSelector: {
      message: 'Drop file here or click to upload[UK] ',
      hint: 'Files up to {0, fileSize, pretty} are allowed[UK] ',
    },
    uploadStatusMessage: {
      error: 'Upload failed, please try again[UK] ',
      uploading: 'Uploading...[UK] ',
      scheduled: 'New document to be uploaded[UK] ',
    },
  },

  saDocument: {
    size: {
      label: '({0, fileSize, pretty})[UK] ',
    },
  },

  dashboard: {
    header: 'Dashboard[UK] ',
  },

  editInvoice: {
    recordedOn: 'Recorded on {0, saDateTime, medium}[UK] ',
    cancelledOn: 'Cancelled on {0, date, medium}[UK] ',
  },

  editExpense: {
    pageHeader: {
      edit: 'Edit Expense[UK] ',
      create: 'Record New Expense[UK] ',
    },
    generalInformation: {
      header: 'General Information[UK] ',
      category: {
        label: 'Category[UK] ',
        placeholder: 'Select a category[UK] ',
      },
      title: {
        label: 'Description / Title[UK] ',
        placeholder: 'Provide a short summary[UK] ',
      },
      currency: {
        label: 'Currency[UK] ',
      },
      originalAmount: {
        label: 'Original Amount[UK] ',
      },
      datePaid: {
        label: 'Date Paid[UK] ',
        placeholder: 'Date expense is paid[UK] ',
      },
      convertedAmountInDefaultCurrency: {
        label: 'Amount in {0}[UK] ',
      },
      useDifferentExchangeRateForIncomeTaxPurposes: {
        label: 'Using different exchange rate for taxation purposes[UK] ',
      },
      incomeTaxableAmountInDefaultCurrency: {
        label: 'Amount in {0} for taxation purposes[UK] ',
      },
      generalTax: {
        label: 'Included General Tax[UK] ',
        placeholder: 'Select a tax[UK] ',
      },
      partialForBusiness: {
        label: 'Partial Business Purpose[UK] ',
      },
      percentOnBusiness: {
        label: '% related to business activities[UK] ',
      },
    },
    additionalInformation: {
      header: 'Additional Information[UK] ',
      notes: {
        label: 'Notes[UK] ',
        placeholder: 'Any additional information to be stored for this expense record[UK] ',
      },
    },
    attachments: {
      header: 'Attachments[UK] ',
    },
    cancel: 'Cancel[UK] ',
    save: 'Save[UK] ',
    validations: {
      currency: 'Please select a currency[UK] ',
      title: 'Please provide the title[UK] ',
      datePaid: 'Please provide the date when expense is paid[UK] ',
      originalAmount: 'Please provide expense amount[UK] ',
    },
    documentsUploadFailure: 'Some of the documents have not been uploaded. Please retry or remove them.[UK] ',
  },

  expensesOverviewPanel: {
    datePaid: {
      tooltip: 'Date Paid[UK] ',
      label: 'Date Paid[UK] ',
    },
    notes: {
      tooltip: 'Additional notes provided[UK] ',
      header: 'Additional Notes[UK] ',
    },
    generalTax: {
      tooltip: 'General Tax applied[UK] ',
      label: 'Applicable General Tax[UK] ',
    },
    generalTaxRate: {
      label: 'Applicable General Tax Rate[UK] ',
      value: '{0, bps, percent}[UK] ',
    },
    generalTaxAmount: {
      label: 'Applicable General Tax Amount[UK] ',
      notProvided: 'Not yet available[UK] ',
    },
    attachments: {
      tooltip: 'Attachments provided[UK] ',
      header: 'Attachments[UK] ',
    },
    foreignCurrency: {
      tooltip: 'In foreign currency[UK] ',
      header: 'Currency Conversion[UK] ',
    },
    partialBusinessPurpose: {
      tooltip: 'Partial business purpose[UK] ',
      label: 'Partial Business Purpose[UK] ',
      value: '{0, number, percent} related to business activities[UK] ',
    },
    copy: 'Copy[UK] ',
    edit: 'Edit[UK] ',
    summary: {
      header: 'Summary[UK] ',
    },
    status: {
      label: 'Status[UK] ',
      short: {
        finalized: 'Finalized[UK] ',
        pending: 'Pending[UK] ',
      },
      full: {
        finalized: 'Finalized[UK] ',
        pendingConversion: 'Conversion to {0} pending[UK] ',
        waitingExchangeRate: 'Waiting for exchange rate[UK] ',
      },
    },
    category: {
      label: 'Category[UK] ',
    },
    incomeTaxableAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Amount in {0} for taxation purposes[UK] ',
        notProvided: 'Not yet available[UK] ',
      },
      adjustedAmountInDefaultCurrency: {
        label: 'Amount for Taxation Purposes[UK] ',
        notProvided: 'Not yet provided[UK] ',
      },
    },
    convertedAmounts: {
      originalAmountInDefaultCurrency: {
        label: 'Amount in {0}[UK] ',
        notProvided: 'Not yet available[UK] ',
      },
    },
    generalInformation: {
      header: 'General Information[UK] ',
    },
    originalCurrency: {
      label: 'Original Currency[UK] ',
    },
    originalAmount: {
      label: 'Original Amount[UK] ',
    },
    differentExchangeRate: {
      label: 'Using different exchange rate for taxation purposes?[UK] ',
      value: '{0, yesNo}[UK] ',
    },
  },

  expensesOverview: {
    header: 'Expenses[UK] ',
    filters: {
      announcement: 'Filters coming soon[UK] ',
      input: {
        placeholder: 'Search expenses[UK] ',
      },
    },
    create: 'Add new[UK] ',
  },

  editIncomeTaxPayment: {
    header: {
      edit: 'Edit Income Tax Payment[UK] ',
      create: 'Record New Income Tax Payment[UK] ',
    },
    generalInformation: {
      header: 'General Information[UK] ',

      title: {
        label: 'Description / Title[UK] ',
        placeholder: 'Provide a short summary[UK] ',
      },
      amount: {
        label: 'Amount[UK] ',
      },
      datePaid: {
        label: 'Date Paid[UK] ',
        placeholder: 'Date tax is paid[UK] ',
      },
      reportingDate: {
        label: 'Reporting Date[UK] ',
        placeholder: 'Date to include this payment into reporting by[UK] ',
      },
    },
    additionalInformation: {
      header: 'Additional notes[UK] ',
      notes: {
        label: 'Notes[UK] ',
        placeholder: 'Any additional information to be stored for this tax payment record[UK] ',
      },
    },
    attachments: {
      header: 'Attachments[UK] ',
    },
    cancel: 'Cancel[UK] ',
    save: 'Save[UK] ',
    validations: {
      title: 'Please provide the title[UK] ',
      datePaid: 'Please provide the date when tax payment is done[UK] ',
      amount: 'Please provide tax payment amount[UK] ',
    },
    uploadFailure: 'Some of the documents have not been uploaded. Please retry or remove them.[UK] ',
  },

  myProfile: {
    languagePreferences: {
      header: 'Language Preferences[UK] ',
      language: {
        label: 'Interface Language[UK] ',
        placeholder: 'Please select interface language[UK] ',
      },
      locale: {
        label: 'Language to display dates, amounts, etc[UK] ',
        placeholder: 'Please select formatting language[UK] ',
      },
    },
  },
};
