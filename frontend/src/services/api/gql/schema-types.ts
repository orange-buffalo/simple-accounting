/* eslint-disable */
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  /** A date-time instant, serialized as an ISO-8601 string (e.g. '2025-01-15T10:30:00Z'). */
  DateTime: { input: string; output: string; }
  /** A date without time, serialized as an ISO-8601 string (e.g. '2025-01-15'). */
  LocalDate: { input: string; output: string; }
  /** A 64-bit signed integer. */
  Long: { input: number; output: number; }
};

/** Additional error extensions for the ACCOUNT_LOCKED business error. */
export type AccountLockedErrorExtensions = {
  __typename?: 'AccountLockedErrorExtensions';
  /** The remaining lock duration in seconds. */
  lockExpiresInSec: Scalars['Int']['output'];
};

/** Possible business error codes for the activateUser operation. */
export enum ActivateUserErrorCodes {
  /** The provided activation token has expired. */
  TokenExpired = 'TOKEN_EXPIRED'
}

/** Response for the activateUser mutation. Always succeeds if no error is returned by the standard GraphQL error response structure. */
export type ActivateUserResponse = {
  __typename?: 'ActivateUserResponse';
  success: Scalars['Boolean']['output'];
};

/** Defines the type of authorization required to execute the request. This is used in conjunction with the `@auth` directive. */
export enum AuthType {
  /** Requires a request to be executed by an admin user, i.e. authenticated and has admin privileges. */
  AdminUser = 'ADMIN_USER',
  /** Allows a request to be executed by an anonymous user, i.e. not authenticated at all. With this restriction, any authenticated user is allowed to execute the request too.  */
  Anonymous = 'ANONYMOUS',
  /** Allows a request to be executed by any authenticated actor, including by workspace access token. */
  AuthenticatedActor = 'AUTHENTICATED_ACTOR',
  /** Allows a request to be executed by any authenticated user, be it admin or regular user, but not via workspace access token. */
  AuthenticatedUser = 'AUTHENTICATED_USER',
  /** Requires a request to be executed by a regular user, i.e. authenticated and not an admin user. */
  RegularUser = 'REGULAR_USER'
}

/** A paginated connection of categories following the GraphQL Cursor Connections Specification. */
export type CategoriesConnection = {
  __typename?: 'CategoriesConnection';
  /** The list of edges in the current page. */
  edges: Array<CategoryEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

/** Category of incomes or expenses. */
export type Category = {
  __typename?: 'Category';
  /** Description of the category. */
  description?: Maybe<Scalars['String']['output']>;
  /** Whether this category is used for expenses. */
  expense: Scalars['Boolean']['output'];
  /** ID of the category. */
  id: Scalars['Long']['output'];
  /** Whether this category is used for incomes. */
  income: Scalars['Boolean']['output'];
  /** Name of the category. */
  name: Scalars['String']['output'];
};

/** An edge in a categories connection. */
export type CategoryEdge = {
  __typename?: 'CategoryEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The category at the end of this edge. */
  node: Category;
};

/** Possible business error codes for the changePassword operation. */
export enum ChangePasswordErrorCodes {
  /** The provided current password does not match the user's actual password. */
  CurrentPasswordMismatch = 'CURRENT_PASSWORD_MISMATCH'
}

/** Response for the changePassword mutation. Always succeeds if no error is returned by standard GraphQL error response structure. */
export type ChangePasswordResponse = {
  __typename?: 'ChangePasswordResponse';
  success: Scalars['Boolean']['output'];
};

/** Response for the completeOAuth2Flow mutation. */
export type CompleteOAuth2FlowResponse = {
  __typename?: 'CompleteOAuth2FlowResponse';
  /** An error reference ID that can be used to identify the specific failure in the logs. Present only when the flow failed. */
  errorId?: Maybe<Scalars['String']['output']>;
  /** Whether the OAuth2 authorization flow was completed successfully. */
  success: Scalars['Boolean']['output'];
};

/** Possible business error codes for the createAccessTokenByCredentials operation. */
export enum CreateAccessTokenByCredentialsErrorCodes {
  /** The account is temporarily locked due to too many failed login attempts. The error extensions will include 'lockExpiresInSec' with the remaining lock duration in seconds. */
  AccountLocked = 'ACCOUNT_LOCKED',
  /** The provided credentials are invalid. */
  BadCredentials = 'BAD_CREDENTIALS',
  /** Login is temporarily unavailable due to too many concurrent authentication requests for this user. */
  LoginNotAvailable = 'LOGIN_NOT_AVAILABLE',
  /** The user account has not been activated yet. */
  UserNotActivated = 'USER_NOT_ACTIVATED'
}

/** Response for the createAccessTokenByCredentials mutation. */
export type CreateAccessTokenByCredentialsResponse = {
  __typename?: 'CreateAccessTokenByCredentialsResponse';
  /** The JWT access token for the authenticated user. */
  accessToken: Scalars['String']['output'];
};

/** Possible business error codes for the createAccessTokenByWorkspaceAccessToken operation. */
export enum CreateAccessTokenByWorkspaceAccessTokenErrorCodes {
  /** The provided workspace access token is not valid (unknown, expired, or revoked). */
  InvalidWorkspaceAccessToken = 'INVALID_WORKSPACE_ACCESS_TOKEN'
}

/** Response for the createAccessTokenByWorkspaceAccessToken mutation. */
export type CreateAccessTokenByWorkspaceAccessTokenResponse = {
  __typename?: 'CreateAccessTokenByWorkspaceAccessTokenResponse';
  /** The JWT access token for the authenticated user. */
  accessToken: Scalars['String']['output'];
  /** The workspace that the access token grants access to. */
  workspace: Workspace;
};

/** Response containing the temporary download URL for a document. */
export type CreateDocumentDownloadUrlResponse = {
  __typename?: 'CreateDocumentDownloadUrlResponse';
  /** Absolute URL to download the document content. The URL is temporary and will expire. */
  url: Scalars['String']['output'];
};

/** Response containing the temporary upload URL for a document. */
export type CreateDocumentUploadUrlResponse = {
  __typename?: 'CreateDocumentUploadUrlResponse';
  /** The name of the multipart form-data part that should contain the file content. */
  filePartName: Scalars['String']['output'];
  /** Absolute URL to upload the document content via multipart/form-data POST. The URL is temporary and will expire. */
  url: Scalars['String']['output'];
};

/** Possible business error codes for the createUserActivationToken operation. */
export enum CreateUserActivationTokenErrorCodes {
  /** The user is already activated and cannot receive an activation token. */
  UserAlreadyActivated = 'USER_ALREADY_ACTIVATED'
}

/** Possible business error codes for the createUser operation. */
export enum CreateUserErrorCodes {
  /** A user with the given username already exists. */
  UserAlreadyExists = 'USER_ALREADY_EXISTS'
}

/** A customer in a workspace. */
export type Customer = {
  __typename?: 'Customer';
  /** ID of the customer. */
  id: Scalars['Long']['output'];
  /** Name of the customer. */
  name: Scalars['String']['output'];
};

/** An edge in a customers connection. */
export type CustomerEdge = {
  __typename?: 'CustomerEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The customer at the end of this edge. */
  node: Customer;
};

/** A paginated connection of customers following the GraphQL Cursor Connections Specification. */
export type CustomersConnection = {
  __typename?: 'CustomersConnection';
  /** The list of edges in the current page. */
  edges: Array<CustomerEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

/** A document in a workspace. */
export type Document = {
  __typename?: 'Document';
  /** ID of the document. */
  id: Scalars['Long']['output'];
  /** MIME type of the document. */
  mimeType: Scalars['String']['output'];
  /** Name of the document. */
  name: Scalars['String']['output'];
  /** Size of the document in bytes. */
  sizeInBytes?: Maybe<Scalars['Long']['output']>;
  /** ID of the storage where the document is stored. */
  storageId: Scalars['String']['output'];
  /** Time when the document was uploaded, as ISO 8601 timestamp. */
  timeUploaded: Scalars['DateTime']['output'];
  /** Entities that use this document. */
  usedBy: Array<DocumentUsage>;
  /** Version of the document for optimistic locking. */
  version: Scalars['Int']['output'];
};

/** An edge in a documents connection. */
export type DocumentEdge = {
  __typename?: 'DocumentEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The document at the end of this edge. */
  node: Document;
};

/** Describes usage of a document by another entity. */
export type DocumentUsage = {
  __typename?: 'DocumentUsage';
  /** Display name of the entity using the document. */
  displayName: Scalars['String']['output'];
  /** ID of the entity using the document. */
  relatedEntityId: Scalars['Long']['output'];
  /** Type of entity using the document. */
  type: DocumentUsageType;
};

/** Type of entity that uses a document. */
export enum DocumentUsageType {
  /** Document is used by an expense. */
  Expense = 'EXPENSE',
  /** Document is used by an income. */
  Income = 'INCOME',
  /** Document is used by an income tax payment. */
  IncomeTaxPayment = 'INCOME_TAX_PAYMENT',
  /** Document is used by an invoice. */
  Invoice = 'INVOICE'
}

/** A paginated connection of documents following the GraphQL Cursor Connections Specification. */
export type DocumentsConnection = {
  __typename?: 'DocumentsConnection';
  /** The list of edges in the current page. */
  edges: Array<DocumentEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

/** Statistics about document storage usage. */
export type DocumentsStorageStatisticsItem = {
  __typename?: 'DocumentsStorageStatisticsItem';
  /** The total number of documents stored in this storage across all workspaces of the current user. */
  documentsCount: Scalars['Int']['output'];
  /** The identifier of the document storage. */
  storageId: Scalars['String']['output'];
};

/** Documents storage status for the current user. */
export type DocumentsStorageStatusResponse = {
  __typename?: 'DocumentsStorageStatusResponse';
  /** Whether the documents storage is active and available. */
  active: Scalars['Boolean']['output'];
};

/** A document storage available for downloading documents. */
export type DownloadDocumentStorageResponse = {
  __typename?: 'DownloadDocumentStorageResponse';
  /** The identifier of the document storage. */
  id: Scalars['String']['output'];
};

/** Possible business error codes for the editUser operation. */
export enum EditUserErrorCodes {
  /** A user with the given username already exists. */
  UserAlreadyExists = 'USER_ALREADY_EXISTS'
}

/** Business expense. */
export type Expense = {
  __typename?: 'Expense';
  /** Documents attached to this expense. */
  attachments: Array<Document>;
  /** Category of the expense. */
  category?: Maybe<Category>;
  /** Amounts converted to the default currency. */
  convertedAmounts: ExpenseAmounts;
  /** Time when the expense was created, as ISO 8601 timestamp. */
  createdAt: Scalars['DateTime']['output'];
  /** Currency of the expense. */
  currency: Scalars['String']['output'];
  /** Date when the expense was paid. */
  datePaid: Scalars['LocalDate']['output'];
  /** General tax applied to this expense. */
  generalTax?: Maybe<GeneralTax>;
  /** Amount of the general tax in cents. */
  generalTaxAmount?: Maybe<Scalars['Long']['output']>;
  /** ID of the general tax applied to this expense. */
  generalTaxId?: Maybe<Scalars['Long']['output']>;
  /** Rate of the general tax in basis points. */
  generalTaxRateInBps?: Maybe<Scalars['Int']['output']>;
  /** ID of the expense. */
  id: Scalars['Long']['output'];
  /** Amounts for income tax purposes in the default currency. */
  incomeTaxableAmounts: ExpenseAmounts;
  /** Optional notes for the expense. */
  notes?: Maybe<Scalars['String']['output']>;
  /** Original amount of the expense in original currency, in cents. */
  originalAmount: Scalars['Long']['output'];
  /** Percentage of the expense on business. */
  percentOnBusiness: Scalars['Int']['output'];
  /** Status of the expense. */
  status: ExpenseStatus;
  /** Title of the expense. */
  title: Scalars['String']['output'];
  /** Whether different exchange rate is used for income tax purposes. */
  useDifferentExchangeRateForIncomeTaxPurposes: Scalars['Boolean']['output'];
  /** Version of the expense for optimistic locking. */
  version: Scalars['Int']['output'];
};

/** Amounts for an expense in the default currency. */
export type ExpenseAmounts = {
  __typename?: 'ExpenseAmounts';
  /** Adjusted amount in the default currency. */
  adjustedAmountInDefaultCurrency?: Maybe<Scalars['Long']['output']>;
  /** Original amount in the default currency, before adjustments. */
  originalAmountInDefaultCurrency?: Maybe<Scalars['Long']['output']>;
};

/** An edge in a expenses connection. */
export type ExpenseEdge = {
  __typename?: 'ExpenseEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The expense at the end of this edge. */
  node: Expense;
};

export enum ExpenseStatus {
  Finalized = 'FINALIZED',
  PendingConversion = 'PENDING_CONVERSION',
  PendingConversionForTaxationPurposes = 'PENDING_CONVERSION_FOR_TAXATION_PURPOSES'
}

/** A paginated connection of expenses following the GraphQL Cursor Connections Specification. */
export type ExpensesConnection = {
  __typename?: 'ExpensesConnection';
  /** The list of edges in the current page. */
  edges: Array<ExpenseEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

/** Summary of expenses for a date range. */
export type ExpensesSummary = {
  __typename?: 'ExpensesSummary';
  /** Total currency exchange difference for finalized expenses in the range. */
  currencyExchangeDifference: Scalars['Long']['output'];
  /** Number of finalized expenses in the range. */
  finalizedCount: Scalars['Long']['output'];
  /** Per-category breakdown of expenses. */
  items: Array<ExpensesSummaryItem>;
  /** Number of pending expenses in the range. */
  pendingCount: Scalars['Long']['output'];
  /** Total amount of all finalized expenses in the range. */
  totalAmount: Scalars['Long']['output'];
};

/** Expenses summary for a single category. */
export type ExpensesSummaryItem = {
  __typename?: 'ExpensesSummaryItem';
  /** Category of the expenses, or null if no category. */
  category?: Maybe<Category>;
  /** Currency exchange difference for finalized expenses. */
  currencyExchangeDifference: Scalars['Long']['output'];
  /** Number of finalized expenses. */
  finalizedCount: Scalars['Long']['output'];
  /** Number of pending expenses. */
  pendingCount: Scalars['Long']['output'];
  /** Total amount for this category. */
  totalAmount: Scalars['Long']['output'];
};

/** Summary of a finalized general tax. */
export type FinalizedGeneralTaxSummaryItem = {
  __typename?: 'FinalizedGeneralTaxSummaryItem';
  /** Total amount of items contributing to this tax. */
  includedItemsAmount: Scalars['Long']['output'];
  /** Number of items contributing to this tax. */
  includedItemsNumber: Scalars['Long']['output'];
  /** The general tax. */
  tax: GeneralTax;
  /** Total amount of tax collected or paid. */
  taxAmount: Scalars['Long']['output'];
};

/** General tax applicable to incomes or expenses. */
export type GeneralTax = {
  __typename?: 'GeneralTax';
  /** Description of the general tax. */
  description?: Maybe<Scalars['String']['output']>;
  /** ID of the general tax. */
  id: Scalars['Long']['output'];
  /** Rate of the general tax in basis points (1/100 of a percent). */
  rateInBps: Scalars['Int']['output'];
  /** Title of the general tax. */
  title: Scalars['String']['output'];
};

/** An edge in a general taxes connection. */
export type GeneralTaxEdge = {
  __typename?: 'GeneralTaxEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The general tax at the end of this edge. */
  node: GeneralTax;
};

/** A paginated connection of general taxes following the GraphQL Cursor Connections Specification. */
export type GeneralTaxesConnection = {
  __typename?: 'GeneralTaxesConnection';
  /** The list of edges in the current page. */
  edges: Array<GeneralTaxEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

/** Summary of general taxes for a date range. */
export type GeneralTaxesSummary = {
  __typename?: 'GeneralTaxesSummary';
  /** Finalized taxes collected on incomes. */
  finalizedCollectedTaxes: Array<FinalizedGeneralTaxSummaryItem>;
  /** Finalized taxes paid on expenses. */
  finalizedPaidTaxes: Array<FinalizedGeneralTaxSummaryItem>;
  /** Pending taxes to be collected on incomes. */
  pendingCollectedTaxes: Array<PendingGeneralTaxSummaryItem>;
  /** Pending taxes to be paid on expenses. */
  pendingPaidTaxes: Array<PendingGeneralTaxSummaryItem>;
};

/** Google Drive storage integration status for the current user. */
export type GoogleDriveStorageIntegrationStatusResponse = {
  __typename?: 'GoogleDriveStorageIntegrationStatusResponse';
  /** Whether Google Drive authorization is required to use the storage. */
  authorizationRequired: Scalars['Boolean']['output'];
  /** The URL to authorize access to Google Drive. Present only when authorization is required. */
  authorizationUrl?: Maybe<Scalars['String']['output']>;
  /** The ID of the Google Drive folder used for storing documents. */
  folderId?: Maybe<Scalars['String']['output']>;
  /** The name of the Google Drive folder used for storing documents. */
  folderName?: Maybe<Scalars['String']['output']>;
};

/** Internationalization settings of the user profile. */
export type I18nSettings = {
  __typename?: 'I18nSettings';
  /** The language of the user profile, e.g. 'en'. Used for translations. */
  language: Scalars['String']['output'];
  /** The locale of the user profile, e.g. 'en-US'. Used for formatting dates, numbers, etc. */
  locale: Scalars['String']['output'];
};

/** Income record of a workspace. */
export type Income = {
  __typename?: 'Income';
  /** Documents attached to this income. */
  attachments: Array<Document>;
  /** Category of this income. */
  category?: Maybe<Category>;
  /** Converted amounts in default currency. */
  convertedAmounts: IncomeAmounts;
  /** Time when the income was created, as ISO 8601 timestamp. */
  createdAt: Scalars['DateTime']['output'];
  /** Currency of the income. */
  currency: Scalars['String']['output'];
  /** Date when the income was received. */
  dateReceived: Scalars['LocalDate']['output'];
  /** General tax applied to this income. */
  generalTax?: Maybe<GeneralTax>;
  /** Amount of general tax applied to this income. */
  generalTaxAmount?: Maybe<Scalars['Long']['output']>;
  /** Rate in basis points of the general tax applied to this income. */
  generalTaxRateInBps?: Maybe<Scalars['Int']['output']>;
  /** ID of the income. */
  id: Scalars['Long']['output'];
  /** Amounts for income tax purposes. */
  incomeTaxableAmounts: IncomeAmounts;
  /** Invoice linked to this income. */
  linkedInvoice?: Maybe<Invoice>;
  /** Optional notes for the income. */
  notes?: Maybe<Scalars['String']['output']>;
  /** Amount in original currency. */
  originalAmount: Scalars['Long']['output'];
  /** Status of the income. */
  status: IncomeStatus;
  /** Title of the income. */
  title: Scalars['String']['output'];
  /** Indicates if income taxable amounts use a different exchange rate. */
  useDifferentExchangeRateForIncomeTaxPurposes: Scalars['Boolean']['output'];
  /** Version of the income for optimistic locking. */
  version: Scalars['Int']['output'];
};

/** Amounts in default currency for an income. */
export type IncomeAmounts = {
  __typename?: 'IncomeAmounts';
  /** Adjusted amount in default currency after tax deduction. */
  adjustedAmountInDefaultCurrency?: Maybe<Scalars['Long']['output']>;
  /** Original amount in default currency before tax deduction. */
  originalAmountInDefaultCurrency?: Maybe<Scalars['Long']['output']>;
};

/** An edge in a incomes connection. */
export type IncomeEdge = {
  __typename?: 'IncomeEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The income at the end of this edge. */
  node: Income;
};

export enum IncomeStatus {
  Finalized = 'FINALIZED',
  PendingConversion = 'PENDING_CONVERSION',
  PendingConversionForTaxationPurposes = 'PENDING_CONVERSION_FOR_TAXATION_PURPOSES'
}

/** An income tax payment in a workspace. */
export type IncomeTaxPayment = {
  __typename?: 'IncomeTaxPayment';
  /** Amount of the tax payment in cents. */
  amount: Scalars['Long']['output'];
  /** Documents attached to this income tax payment. */
  attachments: Array<Document>;
  /** Date when the tax payment was made. */
  datePaid: Scalars['LocalDate']['output'];
  /** ID of the income tax payment. */
  id: Scalars['Long']['output'];
  /** Optional notes for the income tax payment. */
  notes?: Maybe<Scalars['String']['output']>;
  /** Date used for reporting purposes. */
  reportingDate: Scalars['LocalDate']['output'];
  /** Title of the income tax payment. */
  title: Scalars['String']['output'];
};

/** An edge in a income tax payments connection. */
export type IncomeTaxPaymentEdge = {
  __typename?: 'IncomeTaxPaymentEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The income tax payment at the end of this edge. */
  node: IncomeTaxPayment;
};

/** A paginated connection of income tax payments following the GraphQL Cursor Connections Specification. */
export type IncomeTaxPaymentsConnection = {
  __typename?: 'IncomeTaxPaymentsConnection';
  /** The list of edges in the current page. */
  edges: Array<IncomeTaxPaymentEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

/** Summary of income tax payments for a date range. */
export type IncomeTaxPaymentsSummary = {
  __typename?: 'IncomeTaxPaymentsSummary';
  /** Total amount of all income tax payments in the range. */
  totalTaxPayments: Scalars['Long']['output'];
};

/** A paginated connection of incomes following the GraphQL Cursor Connections Specification. */
export type IncomesConnection = {
  __typename?: 'IncomesConnection';
  /** The list of edges in the current page. */
  edges: Array<IncomeEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

/** Summary of incomes for a date range. */
export type IncomesSummary = {
  __typename?: 'IncomesSummary';
  /** Total currency exchange difference for finalized incomes in the range. */
  currencyExchangeDifference: Scalars['Long']['output'];
  /** Number of finalized incomes in the range. */
  finalizedCount: Scalars['Long']['output'];
  /** Per-category breakdown of incomes. */
  items: Array<IncomesSummaryItem>;
  /** Number of pending incomes in the range. */
  pendingCount: Scalars['Long']['output'];
  /** Total amount of all finalized incomes in the range. */
  totalAmount: Scalars['Long']['output'];
};

/** Incomes summary for a single category. */
export type IncomesSummaryItem = {
  __typename?: 'IncomesSummaryItem';
  /** Category of the incomes, or null if no category. */
  category?: Maybe<Category>;
  /** Currency exchange difference for finalized incomes. */
  currencyExchangeDifference: Scalars['Long']['output'];
  /** Number of finalized incomes. */
  finalizedCount: Scalars['Long']['output'];
  /** Number of pending incomes. */
  pendingCount: Scalars['Long']['output'];
  /** Total amount for this category. */
  totalAmount: Scalars['Long']['output'];
};

/** Invoice for a customer. */
export type Invoice = {
  __typename?: 'Invoice';
  /** Amount of the invoice in cents. */
  amount: Scalars['Long']['output'];
  /** Documents attached to this invoice. */
  attachments: Array<Document>;
  /** Time when the invoice was created, as ISO 8601 timestamp. */
  createdAt: Scalars['DateTime']['output'];
  /** Currency of the invoice. */
  currency: Scalars['String']['output'];
  /** Customer of the invoice. */
  customer?: Maybe<Customer>;
  /** Date when the invoice was issued. */
  dateIssued: Scalars['LocalDate']['output'];
  /** Date when the invoice was paid. */
  datePaid?: Maybe<Scalars['LocalDate']['output']>;
  /** Date when the invoice was sent. */
  dateSent?: Maybe<Scalars['LocalDate']['output']>;
  /** Due date of the invoice. */
  dueDate: Scalars['LocalDate']['output'];
  /** General tax applied to this invoice. */
  generalTax?: Maybe<GeneralTax>;
  /** ID of the invoice. */
  id: Scalars['Long']['output'];
  /** Optional notes for the invoice. */
  notes?: Maybe<Scalars['String']['output']>;
  /** Status of the invoice. */
  status: InvoiceStatus;
  /** Time when the invoice was cancelled, as ISO 8601 timestamp. */
  timeCancelled?: Maybe<Scalars['DateTime']['output']>;
  /** Title of the invoice. */
  title: Scalars['String']['output'];
  /** Version of the invoice for optimistic locking. */
  version: Scalars['Int']['output'];
};

/** An edge in a invoices connection. */
export type InvoiceEdge = {
  __typename?: 'InvoiceEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The invoice at the end of this edge. */
  node: Invoice;
};

export enum InvoiceStatus {
  Cancelled = 'CANCELLED',
  Draft = 'DRAFT',
  Overdue = 'OVERDUE',
  Paid = 'PAID',
  Sent = 'SENT'
}

/** A paginated connection of invoices following the GraphQL Cursor Connections Specification. */
export type InvoicesConnection = {
  __typename?: 'InvoicesConnection';
  /** The list of edges in the current page. */
  edges: Array<InvoiceEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

export type Mutation = {
  __typename?: 'Mutation';
  /** Activates a user account using the provided token and sets the user's password. The token is invalidated after successful activation. Accessible by anonymous users. */
  activateUser: ActivateUserResponse;
  /** Cancels an existing invoice in the specified workspace. */
  cancelInvoice: Invoice;
  /** Changes the password of the current user. */
  changePassword: ChangePasswordResponse;
  /** Completes the OAuth2 authorization flow by processing the authorization server callback. */
  completeOAuth2Flow: CompleteOAuth2FlowResponse;
  /** Authenticates a user by username and password credentials and returns an access token. Optionally issues a refresh token cookie for persistent sessions. */
  createAccessTokenByCredentials: CreateAccessTokenByCredentialsResponse;
  /** Authenticates a user by a shared workspace access token and returns an access token. This is used for login-by-link functionality. */
  createAccessTokenByWorkspaceAccessToken: CreateAccessTokenByWorkspaceAccessTokenResponse;
  /** Creates a new category in the specified workspace. */
  createCategory: Category;
  /** Creates a new customer in the specified workspace. */
  createCustomer: Customer;
  /** Creates a short-lived download URL for a document (token expires in 2 minutes). The URL is absolute and can be used directly by the client to download the document content. The URL does not require authentication. */
  createDocumentDownloadUrl: CreateDocumentDownloadUrlResponse;
  /** Creates a short-lived upload URL for a document (token expires in 2 minutes). The URL is absolute and can be used directly by the client to upload a document. The URL does not require authentication. The upload should be done as a multipart/form-data POST request, with the file content in a part named as specified in `filePartName`. The response of the upload request is a JSON object matching the `Document` type in this schema. */
  createDocumentUploadUrl: CreateDocumentUploadUrlResponse;
  /** Creates a new expense in the specified workspace. */
  createExpense: Expense;
  /** Creates a new general tax in the specified workspace. */
  createGeneralTax: GeneralTax;
  /** Creates a new income in the specified workspace. */
  createIncome: Income;
  /** Creates a new income tax payment in the specified workspace. */
  createIncomeTaxPayment: IncomeTaxPayment;
  /** Creates a new invoice in the specified workspace. */
  createInvoice: Invoice;
  /** Creates a new user account. */
  createUser: PlatformUser;
  /** Creates a new activation token for the specified user. If an existing token is present, it will be replaced. Only accessible by admin users. */
  createUserActivationToken: UserActivationTokenGqlDto;
  /** Creates a new workspace for the current user. */
  createWorkspace: Workspace;
  /** Creates a new access token for sharing workspace access. */
  createWorkspaceAccessToken: WorkspaceAccessToken;
  /** Updates an existing category in the specified workspace. */
  editCategory: Category;
  /** Updates an existing customer in the specified workspace. */
  editCustomer: Customer;
  /** Updates an existing expense in the specified workspace. */
  editExpense: Expense;
  /** Updates an existing general tax in the specified workspace. */
  editGeneralTax: GeneralTax;
  /** Updates an existing income in the specified workspace. */
  editIncome: Income;
  /** Updates an existing income tax payment in the specified workspace. */
  editIncomeTaxPayment: IncomeTaxPayment;
  /** Updates an existing invoice in the specified workspace. */
  editInvoice: Invoice;
  /** Updates an existing user's username. */
  editUser: PlatformUser;
  /** Updates an existing workspace. */
  editWorkspace: Workspace;
  /** Invalidates the refresh token cookie, effectively logging out the current user. */
  invalidateRefreshToken: Scalars['Boolean']['output'];
  /** Refreshes the access token using the refresh token from cookies or current authentication. Returns a response with either a valid access token or null if authentication fails. */
  refreshAccessToken: RefreshAccessTokenResponse;
  /** Saves a shared workspace to the current user's list using an access token. */
  saveSharedWorkspace: Workspace;
  /** Updates the current user profile information. */
  updateProfile: UserProfile;
};


export type MutationActivateUserArgs = {
  password: Scalars['String']['input'];
  token: Scalars['String']['input'];
};


export type MutationCancelInvoiceArgs = {
  invoiceId: Scalars['Long']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationChangePasswordArgs = {
  currentPassword: Scalars['String']['input'];
  newPassword: Scalars['String']['input'];
};


export type MutationCompleteOAuth2FlowArgs = {
  code?: InputMaybe<Scalars['String']['input']>;
  error?: InputMaybe<Scalars['String']['input']>;
  state: Scalars['String']['input'];
};


export type MutationCreateAccessTokenByCredentialsArgs = {
  issueRefreshTokenCookie?: InputMaybe<Scalars['Boolean']['input']>;
  password: Scalars['String']['input'];
  userName: Scalars['String']['input'];
};


export type MutationCreateAccessTokenByWorkspaceAccessTokenArgs = {
  workspaceAccessToken: Scalars['String']['input'];
};


export type MutationCreateCategoryArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  expense: Scalars['Boolean']['input'];
  income: Scalars['Boolean']['input'];
  name: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationCreateCustomerArgs = {
  name: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationCreateDocumentDownloadUrlArgs = {
  documentId: Scalars['Long']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationCreateDocumentUploadUrlArgs = {
  workspaceId: Scalars['Long']['input'];
};


export type MutationCreateExpenseArgs = {
  attachments?: InputMaybe<Array<Scalars['Long']['input']>>;
  categoryId?: InputMaybe<Scalars['Long']['input']>;
  convertedAmountInDefaultCurrency?: InputMaybe<Scalars['Long']['input']>;
  currency: Scalars['String']['input'];
  datePaid: Scalars['LocalDate']['input'];
  generalTaxId?: InputMaybe<Scalars['Long']['input']>;
  incomeTaxableAmountInDefaultCurrency?: InputMaybe<Scalars['Long']['input']>;
  notes?: InputMaybe<Scalars['String']['input']>;
  originalAmount: Scalars['Long']['input'];
  percentOnBusiness?: InputMaybe<Scalars['Int']['input']>;
  title: Scalars['String']['input'];
  useDifferentExchangeRateForIncomeTaxPurposes: Scalars['Boolean']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationCreateGeneralTaxArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  rateInBps: Scalars['Int']['input'];
  title: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationCreateIncomeArgs = {
  attachments?: InputMaybe<Array<Scalars['Long']['input']>>;
  categoryId?: InputMaybe<Scalars['Long']['input']>;
  convertedAmountInDefaultCurrency?: InputMaybe<Scalars['Long']['input']>;
  currency: Scalars['String']['input'];
  dateReceived: Scalars['LocalDate']['input'];
  generalTaxId?: InputMaybe<Scalars['Long']['input']>;
  incomeTaxableAmountInDefaultCurrency?: InputMaybe<Scalars['Long']['input']>;
  linkedInvoiceId?: InputMaybe<Scalars['Long']['input']>;
  notes?: InputMaybe<Scalars['String']['input']>;
  originalAmount: Scalars['Long']['input'];
  title: Scalars['String']['input'];
  useDifferentExchangeRateForIncomeTaxPurposes: Scalars['Boolean']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationCreateIncomeTaxPaymentArgs = {
  amount: Scalars['Long']['input'];
  attachments?: InputMaybe<Array<Scalars['Long']['input']>>;
  datePaid: Scalars['LocalDate']['input'];
  notes?: InputMaybe<Scalars['String']['input']>;
  reportingDate?: InputMaybe<Scalars['LocalDate']['input']>;
  title: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationCreateInvoiceArgs = {
  amount: Scalars['Long']['input'];
  attachments?: InputMaybe<Array<Scalars['Long']['input']>>;
  currency: Scalars['String']['input'];
  customerId: Scalars['Long']['input'];
  dateIssued: Scalars['LocalDate']['input'];
  datePaid?: InputMaybe<Scalars['LocalDate']['input']>;
  dateSent?: InputMaybe<Scalars['LocalDate']['input']>;
  dueDate: Scalars['LocalDate']['input'];
  generalTaxId?: InputMaybe<Scalars['Long']['input']>;
  notes?: InputMaybe<Scalars['String']['input']>;
  title: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationCreateUserArgs = {
  admin: Scalars['Boolean']['input'];
  userName: Scalars['String']['input'];
};


export type MutationCreateUserActivationTokenArgs = {
  userId: Scalars['Long']['input'];
};


export type MutationCreateWorkspaceArgs = {
  defaultCurrency: Scalars['String']['input'];
  name: Scalars['String']['input'];
};


export type MutationCreateWorkspaceAccessTokenArgs = {
  validTill: Scalars['DateTime']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationEditCategoryArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  expense: Scalars['Boolean']['input'];
  id: Scalars['Long']['input'];
  income: Scalars['Boolean']['input'];
  name: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationEditCustomerArgs = {
  id: Scalars['Long']['input'];
  name: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationEditExpenseArgs = {
  attachments?: InputMaybe<Array<Scalars['Long']['input']>>;
  categoryId?: InputMaybe<Scalars['Long']['input']>;
  convertedAmountInDefaultCurrency?: InputMaybe<Scalars['Long']['input']>;
  currency: Scalars['String']['input'];
  datePaid: Scalars['LocalDate']['input'];
  generalTaxId?: InputMaybe<Scalars['Long']['input']>;
  id: Scalars['Long']['input'];
  incomeTaxableAmountInDefaultCurrency?: InputMaybe<Scalars['Long']['input']>;
  notes?: InputMaybe<Scalars['String']['input']>;
  originalAmount: Scalars['Long']['input'];
  percentOnBusiness?: InputMaybe<Scalars['Int']['input']>;
  title: Scalars['String']['input'];
  useDifferentExchangeRateForIncomeTaxPurposes: Scalars['Boolean']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationEditGeneralTaxArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['Long']['input'];
  rateInBps: Scalars['Int']['input'];
  title: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationEditIncomeArgs = {
  attachments?: InputMaybe<Array<Scalars['Long']['input']>>;
  categoryId?: InputMaybe<Scalars['Long']['input']>;
  convertedAmountInDefaultCurrency?: InputMaybe<Scalars['Long']['input']>;
  currency: Scalars['String']['input'];
  dateReceived: Scalars['LocalDate']['input'];
  generalTaxId?: InputMaybe<Scalars['Long']['input']>;
  id: Scalars['Long']['input'];
  incomeTaxableAmountInDefaultCurrency?: InputMaybe<Scalars['Long']['input']>;
  linkedInvoiceId?: InputMaybe<Scalars['Long']['input']>;
  notes?: InputMaybe<Scalars['String']['input']>;
  originalAmount: Scalars['Long']['input'];
  title: Scalars['String']['input'];
  useDifferentExchangeRateForIncomeTaxPurposes: Scalars['Boolean']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationEditIncomeTaxPaymentArgs = {
  amount: Scalars['Long']['input'];
  attachments?: InputMaybe<Array<Scalars['Long']['input']>>;
  datePaid: Scalars['LocalDate']['input'];
  id: Scalars['Long']['input'];
  notes?: InputMaybe<Scalars['String']['input']>;
  reportingDate?: InputMaybe<Scalars['LocalDate']['input']>;
  title: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationEditInvoiceArgs = {
  amount: Scalars['Long']['input'];
  attachments?: InputMaybe<Array<Scalars['Long']['input']>>;
  currency: Scalars['String']['input'];
  customerId: Scalars['Long']['input'];
  dateIssued: Scalars['LocalDate']['input'];
  datePaid?: InputMaybe<Scalars['LocalDate']['input']>;
  dateSent?: InputMaybe<Scalars['LocalDate']['input']>;
  dueDate: Scalars['LocalDate']['input'];
  generalTaxId?: InputMaybe<Scalars['Long']['input']>;
  id: Scalars['Long']['input'];
  notes?: InputMaybe<Scalars['String']['input']>;
  title: Scalars['String']['input'];
  workspaceId: Scalars['Long']['input'];
};


export type MutationEditUserArgs = {
  id: Scalars['Long']['input'];
  userName: Scalars['String']['input'];
};


export type MutationEditWorkspaceArgs = {
  id: Scalars['Long']['input'];
  name: Scalars['String']['input'];
};


export type MutationSaveSharedWorkspaceArgs = {
  token: Scalars['String']['input'];
};


export type MutationUpdateProfileArgs = {
  documentsStorage?: InputMaybe<Scalars['String']['input']>;
  language: Scalars['String']['input'];
  locale: Scalars['String']['input'];
};

/** Pagination information following the GraphQL Cursor Connections Specification. */
export type PageInfo = {
  __typename?: 'PageInfo';
  /** Cursor of the last edge in the page. */
  endCursor?: Maybe<Scalars['String']['output']>;
  /** Whether there are more items when paginating forwards. */
  hasNextPage: Scalars['Boolean']['output'];
  /** Whether there are more items when paginating backwards. */
  hasPreviousPage: Scalars['Boolean']['output'];
  /** Cursor of the first edge in the page. */
  startCursor?: Maybe<Scalars['String']['output']>;
};

/** Summary of a pending general tax. */
export type PendingGeneralTaxSummaryItem = {
  __typename?: 'PendingGeneralTaxSummaryItem';
  /** Number of items contributing to this tax. */
  includedItemsNumber: Scalars['Long']['output'];
  /** The general tax. */
  tax: GeneralTax;
};

/** A platform user. */
export type PlatformUser = {
  __typename?: 'PlatformUser';
  /** Whether the user account has been activated. */
  activated: Scalars['Boolean']['output'];
  /** Whether the user has admin privileges. */
  admin: Scalars['Boolean']['output'];
  /** The unique ID of the user. */
  id: Scalars['Long']['output'];
  /** The username / login of the user. */
  userName: Scalars['String']['output'];
};

/** An edge in a platform users connection. */
export type PlatformUserEdge = {
  __typename?: 'PlatformUserEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The platform user at the end of this edge. */
  node: PlatformUser;
};

/** A paginated connection of platform users following the GraphQL Cursor Connections Specification. */
export type PlatformUsersConnection = {
  __typename?: 'PlatformUsersConnection';
  /** The list of edges in the current page. */
  edges: Array<PlatformUserEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

/** A push notification message. */
export type PushNotificationMessage = {
  __typename?: 'PushNotificationMessage';
  /** Optional event data payload, serialized as a JSON string. */
  data?: Maybe<Scalars['String']['output']>;
  /** The name of the event. */
  eventName: Scalars['String']['output'];
};

export type Query = {
  __typename?: 'Query';
  /** Returns statistics about document storage usage across all workspaces of the current user. Only storages that have at least one document are included. */
  documentsStorageStatistics: Array<DocumentsStorageStatisticsItem>;
  /** Returns the current user's documents storage status. */
  documentsStorageStatus: DocumentsStorageStatusResponse;
  /** Returns document storages that are currently available for downloading documents. Iterates over all storage implementations and checks their download availability for the current user context. */
  getDownloadDocumentStorages: Array<DownloadDocumentStorageResponse>;
  /** Returns the current user's Google Drive storage integration status. */
  googleDriveStorageIntegrationStatus: GoogleDriveStorageIntegrationStatusResponse;
  /** Returns the system settings. */
  systemSettings: SystemSettings;
  /** Retrieves the activation token for a user by their ID. Returns null if the token does not exist or has expired. Only accessible by admin users. */
  tokenByUser?: Maybe<UserActivationTokenGqlDto>;
  /** Retrieves an activation token by its value. Returns null if the token does not exist or has expired. Accessible by anonymous users. */
  tokenByValue?: Maybe<UserActivationTokenGqlDto>;
  /** Returns the user with the given ID. */
  user: PlatformUser;
  /** Returns the current user profile information. Current is defined as the user that is authenticated in the current request. */
  userProfile: UserProfile;
  /** Returns all users with cursor-based pagination. Only accessible by admin users. */
  users: PlatformUsersConnection;
  /** Returns a workspace by its ID, if accessible by the current user. */
  workspace: Workspace;
  /** Returns all workspaces accessible by the current user with cursor-based pagination. */
  workspaces: WorkspacesConnection;
};


export type QueryTokenByUserArgs = {
  userId: Scalars['Long']['input'];
};


export type QueryTokenByValueArgs = {
  token: Scalars['String']['input'];
};


export type QueryUserArgs = {
  id: Scalars['Long']['input'];
};


export type QueryUsersArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
  freeSearchText?: InputMaybe<Scalars['String']['input']>;
};


export type QueryWorkspaceArgs = {
  id: Scalars['Long']['input'];
};


export type QueryWorkspacesArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
};

/** Response for refreshing access token. */
export type RefreshAccessTokenResponse = {
  __typename?: 'RefreshAccessTokenResponse';
  /** The new access token if authentication was successful, null otherwise. */
  accessToken?: Maybe<Scalars['String']['output']>;
};

/** Defines the error types that can be returned in GraphQL errors. These error types are included in the `extensions.errorType` field of GraphQL errors. */
export enum SaGrapQlErrorType {
  /** Indicates that a business error occurred during the operation. The specific error code will be provided in `extensions.errorCode`. */
  BusinessError = 'BUSINESS_ERROR',
  /** Indicates that the requested entity was not found or is not accessible by the current user. */
  EntityNotFound = 'ENTITY_NOT_FOUND',
  /** Indicates that one or more input fields failed validation constraints. */
  FieldValidationFailure = 'FIELD_VALIDATION_FAILURE',
  /** Indicates that the request requires authentication or the user is not authorized to perform the operation. */
  NotAuthorized = 'NOT_AUTHORIZED'
}

/** Possible business error codes for the saveSharedWorkspace operation. */
export enum SaveSharedWorkspaceErrorCodes {
  /** The provided workspace access token is not valid (unknown, expired, or revoked). */
  InvalidWorkspaceAccessToken = 'INVALID_WORKSPACE_ACCESS_TOKEN'
}

export type Subscription = {
  __typename?: 'Subscription';
  /** Subscribes to push notifications for the current user. Returns a stream of push notification messages targeted at the authenticated user or broadcast to all users. Uses the `graphql-transport-ws` WebSocket subprotocol. Clients must provide a JWT token in the `connection_init` payload: `{ "type": "connection_init", "payload": { "token": "<JWT>" } }`. Once `connection_ack` is received, subscribe with a standard `subscribe` message. */
  pushNotifications: PushNotificationMessage;
};

/** System-wide settings. */
export type SystemSettings = {
  __typename?: 'SystemSettings';
  /** Whether local file system documents storage is enabled. */
  localFileSystemDocumentsStorageEnabled: Scalars['Boolean']['output'];
};

/** A user activation token used to activate a new user account. */
export type UserActivationTokenGqlDto = {
  __typename?: 'UserActivationTokenGqlDto';
  /** The date and time when the token expires. */
  expiresAt: Scalars['DateTime']['output'];
  /** The token value. */
  token: Scalars['String']['output'];
};

/** Information about the user profile. */
export type UserProfile = {
  __typename?: 'UserProfile';
  /** The identifier of the documents storage used by the user. */
  documentsStorage?: Maybe<Scalars['String']['output']>;
  /** Internationalization settings of the user. */
  i18n: I18nSettings;
  /** The user name / login of the user. */
  userName: Scalars['String']['output'];
};

/** Error codes for validation failures, matching REST API constraint violation error keys. */
export enum ValidationErrorCode {
  /** The field value must be less than or equal to the specified maximum. */
  MaxConstraintViolated = 'MaxConstraintViolated',
  /** The field value must be greater than or equal to the specified minimum. */
  MinConstraintViolated = 'MinConstraintViolated',
  /** The field must not be null, empty, or blank. */
  MustNotBeBlank = 'MustNotBeBlank',
  /** The field must not be null. */
  MustNotBeNull = 'MustNotBeNull',
  /** The field size must be within the specified min/max bounds. */
  SizeConstraintViolated = 'SizeConstraintViolated'
}

/** Details of a field validation error that occurred during input validation. */
export type ValidationErrorDetails = {
  __typename?: 'ValidationErrorDetails';
  /** The error code identifying the type of validation failure. */
  error: ValidationErrorCode;
  /** A human-readable message describing the validation failure. */
  message: Scalars['String']['output'];
  /** Additional constraint parameters if applicable (e.g., min/max values for size constraints). */
  params?: Maybe<Array<ValidationErrorParam>>;
  /** The path to the field that failed validation (e.g., 'currentPassword'). */
  path: Scalars['String']['output'];
};

/** A key-value pair for validation constraint parameters. */
export type ValidationErrorParam = {
  __typename?: 'ValidationErrorParam';
  /** The parameter name (e.g., 'min', 'max'). */
  name: Scalars['String']['output'];
  /** The parameter value. */
  value: Scalars['String']['output'];
};

/** Workspace of a user. */
export type Workspace = {
  __typename?: 'Workspace';
  /** Analytics data for this workspace. */
  analytics: WorkspaceAnalytics;
  /** Categories in this workspace with cursor-based pagination. */
  categories: CategoriesConnection;
  /** Returns a category by its ID if it belongs to this workspace, or null if not found. */
  category?: Maybe<Category>;
  /** Returns a customer by its ID if it belongs to this workspace, or null if not found. */
  customer?: Maybe<Customer>;
  /** Customers in this workspace with cursor-based pagination. */
  customers: CustomersConnection;
  /** Default currency of the workspace. */
  defaultCurrency: Scalars['String']['output'];
  /** Documents in this workspace with cursor-based pagination. */
  documents: DocumentsConnection;
  /** Returns an expense by its ID if it belongs to this workspace, or null if not found. */
  expense?: Maybe<Expense>;
  /** Expenses in this workspace with cursor-based pagination. */
  expenses: ExpensesConnection;
  /** Returns a general tax by its ID if it belongs to this workspace, or null if not found. */
  generalTax?: Maybe<GeneralTax>;
  /** General taxes in this workspace with cursor-based pagination. */
  generalTaxes: GeneralTaxesConnection;
  /** ID of the workspace. */
  id: Scalars['Long']['output'];
  /** Returns an income by its ID if it belongs to this workspace, or null if not found. */
  income?: Maybe<Income>;
  /** Returns an income tax payment by its ID if it belongs to this workspace, or null if not found. */
  incomeTaxPayment?: Maybe<IncomeTaxPayment>;
  /** Income tax payments in this workspace with cursor-based pagination. */
  incomeTaxPayments: IncomeTaxPaymentsConnection;
  /** Incomes in this workspace with cursor-based pagination. */
  incomes: IncomesConnection;
  /** Returns an invoice by its ID if it belongs to this workspace, or null if not found. */
  invoice?: Maybe<Invoice>;
  /** Invoices in this workspace with cursor-based pagination. */
  invoices: InvoicesConnection;
  /** Name of the workspace. */
  name: Scalars['String']['output'];
  /** Workspace access tokens in this workspace with cursor-based pagination. */
  workspaceAccessTokens: WorkspaceAccessTokensConnection;
};


/** Workspace of a user. */
export type WorkspaceCategoriesArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
};


/** Workspace of a user. */
export type WorkspaceCategoryArgs = {
  id: Scalars['Long']['input'];
};


/** Workspace of a user. */
export type WorkspaceCustomerArgs = {
  id: Scalars['Long']['input'];
};


/** Workspace of a user. */
export type WorkspaceCustomersArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
};


/** Workspace of a user. */
export type WorkspaceDocumentsArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
};


/** Workspace of a user. */
export type WorkspaceExpenseArgs = {
  id: Scalars['Long']['input'];
};


/** Workspace of a user. */
export type WorkspaceExpensesArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
  freeSearchText?: InputMaybe<Scalars['String']['input']>;
};


/** Workspace of a user. */
export type WorkspaceGeneralTaxArgs = {
  id: Scalars['Long']['input'];
};


/** Workspace of a user. */
export type WorkspaceGeneralTaxesArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
};


/** Workspace of a user. */
export type WorkspaceIncomeArgs = {
  id: Scalars['Long']['input'];
};


/** Workspace of a user. */
export type WorkspaceIncomeTaxPaymentArgs = {
  id: Scalars['Long']['input'];
};


/** Workspace of a user. */
export type WorkspaceIncomeTaxPaymentsArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
};


/** Workspace of a user. */
export type WorkspaceIncomesArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
  freeSearchText?: InputMaybe<Scalars['String']['input']>;
};


/** Workspace of a user. */
export type WorkspaceInvoiceArgs = {
  id: Scalars['Long']['input'];
};


/** Workspace of a user. */
export type WorkspaceInvoicesArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
  freeSearchText?: InputMaybe<Scalars['String']['input']>;
  statusIn?: InputMaybe<Array<InvoiceStatus>>;
};


/** Workspace of a user. */
export type WorkspaceWorkspaceAccessTokensArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
};

/** An access token for sharing workspace access. */
export type WorkspaceAccessToken = {
  __typename?: 'WorkspaceAccessToken';
  /** ID of the access token. */
  id: Scalars['Long']['output'];
  /** Whether this token has been revoked. */
  revoked: Scalars['Boolean']['output'];
  /** The token value used to share workspace access. */
  token: Scalars['String']['output'];
  /** The expiration time of this token. */
  validTill: Scalars['DateTime']['output'];
  /** Version of the access token. */
  version: Scalars['Int']['output'];
};

/** An edge in a workspace access tokens connection. */
export type WorkspaceAccessTokenEdge = {
  __typename?: 'WorkspaceAccessTokenEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The workspace access token at the end of this edge. */
  node: WorkspaceAccessToken;
};

/** A paginated connection of workspace access tokens following the GraphQL Cursor Connections Specification. */
export type WorkspaceAccessTokensConnection = {
  __typename?: 'WorkspaceAccessTokensConnection';
  /** The list of edges in the current page. */
  edges: Array<WorkspaceAccessTokenEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};

/** Analytics data for a workspace. */
export type WorkspaceAnalytics = {
  __typename?: 'WorkspaceAnalytics';
  /** Shortlist of recently used currency codes, sorted by usage frequency. */
  currenciesShortlist: Array<Scalars['String']['output']>;
  /** Summary of expenses in the given date range. */
  expensesSummary: ExpensesSummary;
  /** Summary of general taxes in the given date range. */
  generalTaxesSummary: GeneralTaxesSummary;
  /** Summary of income tax payments in the given date range. */
  incomeTaxPaymentsSummary: IncomeTaxPaymentsSummary;
  /** Summary of incomes in the given date range. */
  incomesSummary: IncomesSummary;
};


/** Analytics data for a workspace. */
export type WorkspaceAnalyticsExpensesSummaryArgs = {
  fromDate: Scalars['LocalDate']['input'];
  toDate: Scalars['LocalDate']['input'];
};


/** Analytics data for a workspace. */
export type WorkspaceAnalyticsGeneralTaxesSummaryArgs = {
  fromDate: Scalars['LocalDate']['input'];
  toDate: Scalars['LocalDate']['input'];
};


/** Analytics data for a workspace. */
export type WorkspaceAnalyticsIncomeTaxPaymentsSummaryArgs = {
  fromDate: Scalars['LocalDate']['input'];
  toDate: Scalars['LocalDate']['input'];
};


/** Analytics data for a workspace. */
export type WorkspaceAnalyticsIncomesSummaryArgs = {
  fromDate: Scalars['LocalDate']['input'];
  toDate: Scalars['LocalDate']['input'];
};

/** An edge in a workspaces connection. */
export type WorkspaceEdge = {
  __typename?: 'WorkspaceEdge';
  /** The cursor of this edge, which can be used for pagination. */
  cursor: Scalars['String']['output'];
  /** The workspace at the end of this edge. */
  node: Workspace;
};

/** A paginated connection of workspaces following the GraphQL Cursor Connections Specification. */
export type WorkspacesConnection = {
  __typename?: 'WorkspacesConnection';
  /** The list of edges in the current page. */
  edges: Array<WorkspaceEdge>;
  /** Pagination information about the current page. */
  pageInfo: PageInfo;
  /** The total number of items in the connection across all pages. */
  totalCount: Scalars['Int']['output'];
};
