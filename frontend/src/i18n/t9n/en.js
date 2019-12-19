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
};
