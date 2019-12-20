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
};
