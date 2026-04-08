/* eslint-disable */
import type { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
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

/** Business expense. */
export type Expense = {
  __typename?: 'Expense';
  /** Category of the expense. */
  category?: Maybe<Category>;
  /** Title of the expense. */
  title: Scalars['String']['output'];
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

export type Mutation = {
  __typename?: 'Mutation';
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
  /** Creates a new general tax in the specified workspace. */
  createGeneralTax: GeneralTax;
  /** Creates a new income tax payment in the specified workspace. */
  createIncomeTaxPayment: IncomeTaxPayment;
  /** Creates a new workspace for the current user. */
  createWorkspace: Workspace;
  /** Updates an existing category in the specified workspace. */
  editCategory: Category;
  /** Updates an existing customer in the specified workspace. */
  editCustomer: Customer;
  /** Updates an existing general tax in the specified workspace. */
  editGeneralTax: GeneralTax;
  /** Updates an existing income tax payment in the specified workspace. */
  editIncomeTaxPayment: IncomeTaxPayment;
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


export type MutationCreateGeneralTaxArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  rateInBps: Scalars['Int']['input'];
  title: Scalars['String']['input'];
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


export type MutationCreateWorkspaceArgs = {
  defaultCurrency: Scalars['String']['input'];
  name: Scalars['String']['input'];
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


export type MutationEditGeneralTaxArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['Long']['input'];
  rateInBps: Scalars['Int']['input'];
  title: Scalars['String']['input'];
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
  /** Returns the current user profile information. Current is defined as the user that is authenticated in the current request. */
  userProfile: UserProfile;
  /** Returns a workspace by its ID, if accessible by the current user. */
  workspace: Workspace;
  /** Returns all workspaces accessible by the current user with cursor-based pagination. */
  workspaces: WorkspacesConnection;
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

/** System-wide settings. */
export type SystemSettings = {
  __typename?: 'SystemSettings';
  /** Whether local file system documents storage is enabled. */
  localFileSystemDocumentsStorageEnabled: Scalars['Boolean']['output'];
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
  /** Expenses in this workspace. */
  expenses: Array<Expense>;
  /** Returns a general tax by its ID if it belongs to this workspace, or null if not found. */
  generalTax?: Maybe<GeneralTax>;
  /** General taxes in this workspace with cursor-based pagination. */
  generalTaxes: GeneralTaxesConnection;
  /** ID of the workspace. */
  id: Scalars['Long']['output'];
  /** Returns an income tax payment by its ID if it belongs to this workspace, or null if not found. */
  incomeTaxPayment?: Maybe<IncomeTaxPayment>;
  /** Income tax payments in this workspace with cursor-based pagination. */
  incomeTaxPayments: IncomeTaxPaymentsConnection;
  /** Name of the workspace. */
  name: Scalars['String']['output'];
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
export type WorkspaceGeneralTaxArgs = {
  id: Scalars['Long']['input'];
};


/** Workspace of a user. */
export type WorkspaceGeneralTaxesArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
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

export type GetCategoriesForInputQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
}>;


export type GetCategoriesForInputQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', categories: { __typename?: 'CategoriesConnection', edges: Array<{ __typename?: 'CategoryEdge', node: { __typename?: 'Category', id: number, name: string } }> } } };

export type GetCategoryForOutputQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  categoryId: Scalars['Long']['input'];
}>;


export type GetCategoryForOutputQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', category?: { __typename?: 'Category', name: string } | null } };

export type DownloadDocumentStoragesQueryVariables = Exact<{ [key: string]: never; }>;


export type DownloadDocumentStoragesQuery = { __typename?: 'Query', getDownloadDocumentStorages: Array<{ __typename?: 'DownloadDocumentStorageResponse', id: string }> };

export type DocumentsUploadStorageStatusQueryVariables = Exact<{ [key: string]: never; }>;


export type DocumentsUploadStorageStatusQuery = { __typename?: 'Query', documentsStorageStatus: { __typename?: 'DocumentsStorageStatusResponse', active: boolean }, getDownloadDocumentStorages: Array<{ __typename?: 'DownloadDocumentStorageResponse', id: string }> };

export type GoogleDriveStorageIntegrationStatusQueryVariables = Exact<{ [key: string]: never; }>;


export type GoogleDriveStorageIntegrationStatusQuery = { __typename?: 'Query', googleDriveStorageIntegrationStatus: { __typename?: 'GoogleDriveStorageIntegrationStatusResponse', authorizationRequired: boolean, authorizationUrl?: string | null, folderId?: string | null, folderName?: string | null } };

export type DocumentsStorageStatusQueryVariables = Exact<{ [key: string]: never; }>;


export type DocumentsStorageStatusQuery = { __typename?: 'Query', documentsStorageStatus: { __typename?: 'DocumentsStorageStatusResponse', active: boolean } };

export type GetGeneralTaxesForInputQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
}>;


export type GetGeneralTaxesForInputQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', generalTaxes: { __typename?: 'GeneralTaxesConnection', edges: Array<{ __typename?: 'GeneralTaxEdge', node: { __typename?: 'GeneralTax', id: number, title: string } }> } } };

export type GetGeneralTaxForOutputQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  taxId: Scalars['Long']['input'];
}>;


export type GetGeneralTaxForOutputQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', generalTax?: { __typename?: 'GeneralTax', title: string } | null } };

export type PaginationPageInfoFragment = { __typename?: 'PageInfo', endCursor?: string | null, hasNextPage: boolean, hasPreviousPage: boolean, startCursor?: string | null } & { ' $fragmentName'?: 'PaginationPageInfoFragment' };

export type SaveSharedWorkspaceLoginByLinkMutationVariables = Exact<{
  token: Scalars['String']['input'];
}>;


export type SaveSharedWorkspaceLoginByLinkMutation = { __typename?: 'Mutation', saveSharedWorkspace: { __typename?: 'Workspace', id: number, name: string, defaultCurrency: string } };

export type CreateWorkspaceAccountSetupMutationVariables = Exact<{
  name: Scalars['String']['input'];
  defaultCurrency: Scalars['String']['input'];
}>;


export type CreateWorkspaceAccountSetupMutation = { __typename?: 'Mutation', createWorkspace: { __typename?: 'Workspace', id: number, name: string, defaultCurrency: string } };

export type DocumentsPageQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  first: Scalars['Int']['input'];
  after?: InputMaybe<Scalars['String']['input']>;
}>;


export type DocumentsPageQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', documents: { __typename?: 'DocumentsConnection', totalCount: number, edges: Array<{ __typename?: 'DocumentEdge', cursor: string, node: { __typename?: 'Document', id: number, name: string, timeUploaded: string, storageId: string, usedBy: Array<{ __typename?: 'DocumentUsage', type: DocumentUsageType, relatedEntityId: number, displayName: string }> } }>, pageInfo: (
        { __typename?: 'PageInfo' }
        & { ' $fragmentRefs'?: { 'PaginationPageInfoFragment': PaginationPageInfoFragment } }
      ) } } };

export type GetIncomeTaxPaymentForEditQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  id: Scalars['Long']['input'];
}>;


export type GetIncomeTaxPaymentForEditQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', incomeTaxPayment?: { __typename?: 'IncomeTaxPayment', id: number, title: string, datePaid: string, reportingDate: string, amount: number, notes?: string | null, attachments: Array<{ __typename?: 'Document', id: number }> } | null } };

export type CreateIncomeTaxPaymentMutationMutationVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  title: Scalars['String']['input'];
  datePaid: Scalars['LocalDate']['input'];
  reportingDate?: InputMaybe<Scalars['LocalDate']['input']>;
  amount: Scalars['Long']['input'];
  notes?: InputMaybe<Scalars['String']['input']>;
  attachments?: InputMaybe<Array<Scalars['Long']['input']> | Scalars['Long']['input']>;
}>;


export type CreateIncomeTaxPaymentMutationMutation = { __typename?: 'Mutation', createIncomeTaxPayment: { __typename?: 'IncomeTaxPayment', id: number } };

export type EditIncomeTaxPaymentMutationMutationVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  id: Scalars['Long']['input'];
  title: Scalars['String']['input'];
  datePaid: Scalars['LocalDate']['input'];
  reportingDate?: InputMaybe<Scalars['LocalDate']['input']>;
  amount: Scalars['Long']['input'];
  notes?: InputMaybe<Scalars['String']['input']>;
  attachments?: InputMaybe<Array<Scalars['Long']['input']> | Scalars['Long']['input']>;
}>;


export type EditIncomeTaxPaymentMutationMutation = { __typename?: 'Mutation', editIncomeTaxPayment: { __typename?: 'IncomeTaxPayment', id: number } };

export type IncomeTaxPaymentsPageQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  first: Scalars['Int']['input'];
  after?: InputMaybe<Scalars['String']['input']>;
}>;


export type IncomeTaxPaymentsPageQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', incomeTaxPayments: { __typename?: 'IncomeTaxPaymentsConnection', totalCount: number, edges: Array<{ __typename?: 'IncomeTaxPaymentEdge', cursor: string, node: { __typename?: 'IncomeTaxPayment', id: number, title: string, datePaid: string, reportingDate: string, amount: number, notes?: string | null, attachments: Array<{ __typename?: 'Document', id: number }> } }>, pageInfo: (
        { __typename?: 'PageInfo' }
        & { ' $fragmentRefs'?: { 'PaginationPageInfoFragment': PaginationPageInfoFragment } }
      ) } } };

export type GetGeneralTaxForInvoiceQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  taxId: Scalars['Long']['input'];
}>;


export type GetGeneralTaxForInvoiceQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', generalTax?: { __typename?: 'GeneralTax', rateInBps: number } | null } };

export type UserProfileLoginQueryVariables = Exact<{ [key: string]: never; }>;


export type UserProfileLoginQuery = { __typename?: 'Query', userProfile: { __typename?: 'UserProfile', i18n: { __typename?: 'I18nSettings', language: string, locale: string } } };

export type UserProfileQueryVariables = Exact<{ [key: string]: never; }>;


export type UserProfileQuery = { __typename?: 'Query', userProfile: { __typename?: 'UserProfile', documentsStorage?: string | null, userName: string, i18n: { __typename?: 'I18nSettings', language: string, locale: string } } };

export type ChangePasswordMutationVariables = Exact<{
  currentPassword: Scalars['String']['input'];
  newPassword: Scalars['String']['input'];
}>;


export type ChangePasswordMutation = { __typename?: 'Mutation', changePassword: { __typename?: 'ChangePasswordResponse', success: boolean } };

export type DocumentsStorageConfigQueryVariables = Exact<{ [key: string]: never; }>;


export type DocumentsStorageConfigQuery = { __typename?: 'Query', documentsStorageStatistics: Array<{ __typename?: 'DocumentsStorageStatisticsItem', storageId: string, documentsCount: number }>, systemSettings: { __typename?: 'SystemSettings', localFileSystemDocumentsStorageEnabled: boolean } };

export type UpdateProfileStorageMutationVariables = Exact<{
  documentsStorage?: InputMaybe<Scalars['String']['input']>;
  locale: Scalars['String']['input'];
  language: Scalars['String']['input'];
}>;


export type UpdateProfileStorageMutation = { __typename?: 'Mutation', updateProfile: { __typename?: 'UserProfile', documentsStorage?: string | null, userName: string, i18n: { __typename?: 'I18nSettings', language: string, locale: string } } };

export type UpdateProfileLanguageMutationVariables = Exact<{
  documentsStorage?: InputMaybe<Scalars['String']['input']>;
  locale: Scalars['String']['input'];
  language: Scalars['String']['input'];
}>;


export type UpdateProfileLanguageMutation = { __typename?: 'Mutation', updateProfile: { __typename?: 'UserProfile', documentsStorage?: string | null, userName: string, i18n: { __typename?: 'I18nSettings', language: string, locale: string } } };

export type CompleteOAuth2FlowMutationVariables = Exact<{
  code?: InputMaybe<Scalars['String']['input']>;
  error?: InputMaybe<Scalars['String']['input']>;
  state: Scalars['String']['input'];
}>;


export type CompleteOAuth2FlowMutation = { __typename?: 'Mutation', completeOAuth2Flow: { __typename?: 'CompleteOAuth2FlowResponse', success: boolean, errorId?: string | null } };

export type CategoriesPageQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  first: Scalars['Int']['input'];
  after?: InputMaybe<Scalars['String']['input']>;
}>;


export type CategoriesPageQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', categories: { __typename?: 'CategoriesConnection', totalCount: number, edges: Array<{ __typename?: 'CategoryEdge', cursor: string, node: { __typename?: 'Category', id: number, name: string, description?: string | null, income: boolean, expense: boolean } }>, pageInfo: (
        { __typename?: 'PageInfo' }
        & { ' $fragmentRefs'?: { 'PaginationPageInfoFragment': PaginationPageInfoFragment } }
      ) } } };

export type CreateCategoryMutationMutationVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  income: Scalars['Boolean']['input'];
  expense: Scalars['Boolean']['input'];
}>;


export type CreateCategoryMutationMutation = { __typename?: 'Mutation', createCategory: { __typename?: 'Category', id: number } };

export type GetCategoryForEditQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  categoryId: Scalars['Long']['input'];
}>;


export type GetCategoryForEditQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', category?: { __typename?: 'Category', id: number, name: string, description?: string | null, income: boolean, expense: boolean } | null } };

export type EditCategoryMutationMutationVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  id: Scalars['Long']['input'];
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  income: Scalars['Boolean']['input'];
  expense: Scalars['Boolean']['input'];
}>;


export type EditCategoryMutationMutation = { __typename?: 'Mutation', editCategory: { __typename?: 'Category', id: number } };

export type CustomersPageQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  first: Scalars['Int']['input'];
  after?: InputMaybe<Scalars['String']['input']>;
}>;


export type CustomersPageQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', customers: { __typename?: 'CustomersConnection', totalCount: number, edges: Array<{ __typename?: 'CustomerEdge', cursor: string, node: { __typename?: 'Customer', id: number, name: string } }>, pageInfo: (
        { __typename?: 'PageInfo' }
        & { ' $fragmentRefs'?: { 'PaginationPageInfoFragment': PaginationPageInfoFragment } }
      ) } } };

export type GetGeneralTaxForEditQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  taxId: Scalars['Long']['input'];
}>;


export type GetGeneralTaxForEditQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', generalTax?: { __typename?: 'GeneralTax', id: number, title: string, description?: string | null, rateInBps: number } | null } };

export type CreateGeneralTaxMutationMutationVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  title: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  rateInBps: Scalars['Int']['input'];
}>;


export type CreateGeneralTaxMutationMutation = { __typename?: 'Mutation', createGeneralTax: { __typename?: 'GeneralTax', id: number } };

export type EditGeneralTaxMutationMutationVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  id: Scalars['Long']['input'];
  title: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  rateInBps: Scalars['Int']['input'];
}>;


export type EditGeneralTaxMutationMutation = { __typename?: 'Mutation', editGeneralTax: { __typename?: 'GeneralTax', id: number } };

export type GeneralTaxesPageQueryVariables = Exact<{
  workspaceId: Scalars['Long']['input'];
  first: Scalars['Int']['input'];
  after?: InputMaybe<Scalars['String']['input']>;
}>;


export type GeneralTaxesPageQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', generalTaxes: { __typename?: 'GeneralTaxesConnection', totalCount: number, edges: Array<{ __typename?: 'GeneralTaxEdge', cursor: string, node: { __typename?: 'GeneralTax', id: number, title: string, description?: string | null, rateInBps: number } }>, pageInfo: (
        { __typename?: 'PageInfo' }
        & { ' $fragmentRefs'?: { 'PaginationPageInfoFragment': PaginationPageInfoFragment } }
      ) } } };

export type WorkspaceForEditorQueryVariables = Exact<{
  id: Scalars['Long']['input'];
}>;


export type WorkspaceForEditorQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', id: number, name: string, defaultCurrency: string } };

export type CreateWorkspaceEditorMutationVariables = Exact<{
  name: Scalars['String']['input'];
  defaultCurrency: Scalars['String']['input'];
}>;


export type CreateWorkspaceEditorMutation = { __typename?: 'Mutation', createWorkspace: { __typename?: 'Workspace', id: number, name: string, defaultCurrency: string } };

export type EditWorkspaceEditorMutationVariables = Exact<{
  id: Scalars['Long']['input'];
  name: Scalars['String']['input'];
}>;


export type EditWorkspaceEditorMutation = { __typename?: 'Mutation', editWorkspace: { __typename?: 'Workspace', id: number, name: string, defaultCurrency: string } };

export type WorkspacesPageQueryVariables = Exact<{
  first: Scalars['Int']['input'];
  after?: InputMaybe<Scalars['String']['input']>;
}>;


export type WorkspacesPageQuery = { __typename?: 'Query', workspaces: { __typename?: 'WorkspacesConnection', totalCount: number, edges: Array<{ __typename?: 'WorkspaceEdge', cursor: string, node: { __typename?: 'Workspace', id: number, name: string, defaultCurrency: string } }>, pageInfo: (
      { __typename?: 'PageInfo' }
      & { ' $fragmentRefs'?: { 'PaginationPageInfoFragment': PaginationPageInfoFragment } }
    ) } };

export type RefreshAccessTokenMutationVariables = Exact<{ [key: string]: never; }>;


export type RefreshAccessTokenMutation = { __typename?: 'Mutation', refreshAccessToken: { __typename?: 'RefreshAccessTokenResponse', accessToken?: string | null } };

export type CreateAccessTokenByCredentialsMutationVariables = Exact<{
  userName: Scalars['String']['input'];
  password: Scalars['String']['input'];
  issueRefreshTokenCookie?: InputMaybe<Scalars['Boolean']['input']>;
}>;


export type CreateAccessTokenByCredentialsMutation = { __typename?: 'Mutation', createAccessTokenByCredentials: { __typename?: 'CreateAccessTokenByCredentialsResponse', accessToken: string } };

export type CreateAccessTokenByWorkspaceAccessTokenMutationVariables = Exact<{
  workspaceAccessToken: Scalars['String']['input'];
}>;


export type CreateAccessTokenByWorkspaceAccessTokenMutation = { __typename?: 'Mutation', createAccessTokenByWorkspaceAccessToken: { __typename?: 'CreateAccessTokenByWorkspaceAccessTokenResponse', accessToken: string, workspace: { __typename?: 'Workspace', id: number, name: string, defaultCurrency: string } } };

export type AllWorkspacesQueryVariables = Exact<{
  first: Scalars['Int']['input'];
}>;


export type AllWorkspacesQuery = { __typename?: 'Query', workspaces: { __typename?: 'WorkspacesConnection', edges: Array<{ __typename?: 'WorkspaceEdge', node: { __typename?: 'Workspace', id: number, name: string, defaultCurrency: string } }> } };

export type WorkspaceByIdQueryVariables = Exact<{
  id: Scalars['Long']['input'];
}>;


export type WorkspaceByIdQuery = { __typename?: 'Query', workspace: { __typename?: 'Workspace', id: number, name: string, defaultCurrency: string } };

export type UserProfileBootstrapQueryVariables = Exact<{ [key: string]: never; }>;


export type UserProfileBootstrapQuery = { __typename?: 'Query', userProfile: { __typename?: 'UserProfile', i18n: { __typename?: 'I18nSettings', language: string, locale: string } } };

export const PaginationPageInfoFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<PaginationPageInfoFragment, unknown>;
export const GetCategoriesForInputDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"getCategoriesForInput"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"categories"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"IntValue","value":"500"}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}}]}}]}}]}}]}}]}}]} as unknown as DocumentNode<GetCategoriesForInputQuery, GetCategoriesForInputQueryVariables>;
export const GetCategoryForOutputDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"getCategoryForOutput"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"categoryId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"category"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"categoryId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"name"}}]}}]}}]}}]} as unknown as DocumentNode<GetCategoryForOutputQuery, GetCategoryForOutputQueryVariables>;
export const DownloadDocumentStoragesDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"downloadDocumentStorages"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"getDownloadDocumentStorages"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<DownloadDocumentStoragesQuery, DownloadDocumentStoragesQueryVariables>;
export const DocumentsUploadStorageStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"documentsUploadStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"active"}}]}},{"kind":"Field","name":{"kind":"Name","value":"getDownloadDocumentStorages"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<DocumentsUploadStorageStatusQuery, DocumentsUploadStorageStatusQueryVariables>;
export const GoogleDriveStorageIntegrationStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"googleDriveStorageIntegrationStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"googleDriveStorageIntegrationStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"authorizationRequired"}},{"kind":"Field","name":{"kind":"Name","value":"authorizationUrl"}},{"kind":"Field","name":{"kind":"Name","value":"folderId"}},{"kind":"Field","name":{"kind":"Name","value":"folderName"}}]}}]}}]} as unknown as DocumentNode<GoogleDriveStorageIntegrationStatusQuery, GoogleDriveStorageIntegrationStatusQueryVariables>;
export const DocumentsStorageStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"documentsStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"active"}}]}}]}}]} as unknown as DocumentNode<DocumentsStorageStatusQuery, DocumentsStorageStatusQueryVariables>;
export const GetGeneralTaxesForInputDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"getGeneralTaxesForInput"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"generalTaxes"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"IntValue","value":"500"}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}}]}}]}}]}}]}}]}}]} as unknown as DocumentNode<GetGeneralTaxesForInputQuery, GetGeneralTaxesForInputQueryVariables>;
export const GetGeneralTaxForOutputDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"getGeneralTaxForOutput"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"taxId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"generalTax"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"taxId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"title"}}]}}]}}]}}]} as unknown as DocumentNode<GetGeneralTaxForOutputQuery, GetGeneralTaxForOutputQueryVariables>;
export const SaveSharedWorkspaceLoginByLinkDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"saveSharedWorkspaceLoginByLink"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"token"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"saveSharedWorkspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"token"},"value":{"kind":"Variable","name":{"kind":"Name","value":"token"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}}]} as unknown as DocumentNode<SaveSharedWorkspaceLoginByLinkMutation, SaveSharedWorkspaceLoginByLinkMutationVariables>;
export const CreateWorkspaceAccountSetupDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createWorkspaceAccountSetup"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"name"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"defaultCurrency"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createWorkspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"name"}}},{"kind":"Argument","name":{"kind":"Name","value":"defaultCurrency"},"value":{"kind":"Variable","name":{"kind":"Name","value":"defaultCurrency"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}}]} as unknown as DocumentNode<CreateWorkspaceAccountSetupMutation, CreateWorkspaceAccountSetupMutationVariables>;
export const DocumentsPageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"documentsPage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"first"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"after"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documents"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"Variable","name":{"kind":"Name","value":"first"}}},{"kind":"Argument","name":{"kind":"Name","value":"after"},"value":{"kind":"Variable","name":{"kind":"Name","value":"after"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"cursor"}},{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"timeUploaded"}},{"kind":"Field","name":{"kind":"Name","value":"storageId"}},{"kind":"Field","name":{"kind":"Name","value":"usedBy"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"type"}},{"kind":"Field","name":{"kind":"Name","value":"relatedEntityId"}},{"kind":"Field","name":{"kind":"Name","value":"displayName"}}]}}]}}]}},{"kind":"Field","name":{"kind":"Name","value":"pageInfo"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"PaginationPageInfo"}}]}},{"kind":"Field","name":{"kind":"Name","value":"totalCount"}}]}}]}}]}},{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<DocumentsPageQuery, DocumentsPageQueryVariables>;
export const GetIncomeTaxPaymentForEditDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"getIncomeTaxPaymentForEdit"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"incomeTaxPayment"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"datePaid"}},{"kind":"Field","name":{"kind":"Name","value":"reportingDate"}},{"kind":"Field","name":{"kind":"Name","value":"amount"}},{"kind":"Field","name":{"kind":"Name","value":"notes"}},{"kind":"Field","name":{"kind":"Name","value":"attachments"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]}}]}}]} as unknown as DocumentNode<GetIncomeTaxPaymentForEditQuery, GetIncomeTaxPaymentForEditQueryVariables>;
export const CreateIncomeTaxPaymentMutationDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createIncomeTaxPaymentMutation"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"title"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"datePaid"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"LocalDate"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"reportingDate"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"LocalDate"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"amount"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"notes"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"attachments"}},"type":{"kind":"ListType","type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createIncomeTaxPayment"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceId"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}},{"kind":"Argument","name":{"kind":"Name","value":"title"},"value":{"kind":"Variable","name":{"kind":"Name","value":"title"}}},{"kind":"Argument","name":{"kind":"Name","value":"datePaid"},"value":{"kind":"Variable","name":{"kind":"Name","value":"datePaid"}}},{"kind":"Argument","name":{"kind":"Name","value":"reportingDate"},"value":{"kind":"Variable","name":{"kind":"Name","value":"reportingDate"}}},{"kind":"Argument","name":{"kind":"Name","value":"amount"},"value":{"kind":"Variable","name":{"kind":"Name","value":"amount"}}},{"kind":"Argument","name":{"kind":"Name","value":"notes"},"value":{"kind":"Variable","name":{"kind":"Name","value":"notes"}}},{"kind":"Argument","name":{"kind":"Name","value":"attachments"},"value":{"kind":"Variable","name":{"kind":"Name","value":"attachments"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<CreateIncomeTaxPaymentMutationMutation, CreateIncomeTaxPaymentMutationMutationVariables>;
export const EditIncomeTaxPaymentMutationDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"editIncomeTaxPaymentMutation"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"title"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"datePaid"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"LocalDate"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"reportingDate"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"LocalDate"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"amount"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"notes"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"attachments"}},"type":{"kind":"ListType","type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"editIncomeTaxPayment"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceId"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}},{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}},{"kind":"Argument","name":{"kind":"Name","value":"title"},"value":{"kind":"Variable","name":{"kind":"Name","value":"title"}}},{"kind":"Argument","name":{"kind":"Name","value":"datePaid"},"value":{"kind":"Variable","name":{"kind":"Name","value":"datePaid"}}},{"kind":"Argument","name":{"kind":"Name","value":"reportingDate"},"value":{"kind":"Variable","name":{"kind":"Name","value":"reportingDate"}}},{"kind":"Argument","name":{"kind":"Name","value":"amount"},"value":{"kind":"Variable","name":{"kind":"Name","value":"amount"}}},{"kind":"Argument","name":{"kind":"Name","value":"notes"},"value":{"kind":"Variable","name":{"kind":"Name","value":"notes"}}},{"kind":"Argument","name":{"kind":"Name","value":"attachments"},"value":{"kind":"Variable","name":{"kind":"Name","value":"attachments"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<EditIncomeTaxPaymentMutationMutation, EditIncomeTaxPaymentMutationMutationVariables>;
export const IncomeTaxPaymentsPageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"incomeTaxPaymentsPage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"first"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"after"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"incomeTaxPayments"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"Variable","name":{"kind":"Name","value":"first"}}},{"kind":"Argument","name":{"kind":"Name","value":"after"},"value":{"kind":"Variable","name":{"kind":"Name","value":"after"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"cursor"}},{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"datePaid"}},{"kind":"Field","name":{"kind":"Name","value":"reportingDate"}},{"kind":"Field","name":{"kind":"Name","value":"amount"}},{"kind":"Field","name":{"kind":"Name","value":"attachments"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}},{"kind":"Field","name":{"kind":"Name","value":"notes"}}]}}]}},{"kind":"Field","name":{"kind":"Name","value":"pageInfo"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"PaginationPageInfo"}}]}},{"kind":"Field","name":{"kind":"Name","value":"totalCount"}}]}}]}}]}},{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<IncomeTaxPaymentsPageQuery, IncomeTaxPaymentsPageQueryVariables>;
export const GetGeneralTaxForInvoiceDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"getGeneralTaxForInvoice"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"taxId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"generalTax"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"taxId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"rateInBps"}}]}}]}}]}}]} as unknown as DocumentNode<GetGeneralTaxForInvoiceQuery, GetGeneralTaxForInvoiceQueryVariables>;
export const UserProfileLoginDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"userProfileLogin"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}}]}}]}}]} as unknown as DocumentNode<UserProfileLoginQuery, UserProfileLoginQueryVariables>;
export const UserProfileDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorage"}},{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}},{"kind":"Field","name":{"kind":"Name","value":"userName"}}]}}]}}]} as unknown as DocumentNode<UserProfileQuery, UserProfileQueryVariables>;
export const ChangePasswordDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"changePassword"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"currentPassword"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"newPassword"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"changePassword"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"currentPassword"},"value":{"kind":"Variable","name":{"kind":"Name","value":"currentPassword"}}},{"kind":"Argument","name":{"kind":"Name","value":"newPassword"},"value":{"kind":"Variable","name":{"kind":"Name","value":"newPassword"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"success"}}]}}]}}]} as unknown as DocumentNode<ChangePasswordMutation, ChangePasswordMutationVariables>;
export const DocumentsStorageConfigDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"documentsStorageConfig"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorageStatistics"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"storageId"}},{"kind":"Field","name":{"kind":"Name","value":"documentsCount"}}]}},{"kind":"Field","name":{"kind":"Name","value":"systemSettings"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"localFileSystemDocumentsStorageEnabled"}}]}}]}}]} as unknown as DocumentNode<DocumentsStorageConfigQuery, DocumentsStorageConfigQueryVariables>;
export const UpdateProfileStorageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"updateProfileStorage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"locale"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"language"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"updateProfile"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"documentsStorage"},"value":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}}},{"kind":"Argument","name":{"kind":"Name","value":"locale"},"value":{"kind":"Variable","name":{"kind":"Name","value":"locale"}}},{"kind":"Argument","name":{"kind":"Name","value":"language"},"value":{"kind":"Variable","name":{"kind":"Name","value":"language"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorage"}},{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}},{"kind":"Field","name":{"kind":"Name","value":"userName"}}]}}]}}]} as unknown as DocumentNode<UpdateProfileStorageMutation, UpdateProfileStorageMutationVariables>;
export const UpdateProfileLanguageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"updateProfileLanguage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"locale"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"language"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"updateProfile"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"documentsStorage"},"value":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}}},{"kind":"Argument","name":{"kind":"Name","value":"locale"},"value":{"kind":"Variable","name":{"kind":"Name","value":"locale"}}},{"kind":"Argument","name":{"kind":"Name","value":"language"},"value":{"kind":"Variable","name":{"kind":"Name","value":"language"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorage"}},{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}},{"kind":"Field","name":{"kind":"Name","value":"userName"}}]}}]}}]} as unknown as DocumentNode<UpdateProfileLanguageMutation, UpdateProfileLanguageMutationVariables>;
export const CompleteOAuth2FlowDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"completeOAuth2Flow"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"code"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"error"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"state"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"completeOAuth2Flow"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"code"},"value":{"kind":"Variable","name":{"kind":"Name","value":"code"}}},{"kind":"Argument","name":{"kind":"Name","value":"error"},"value":{"kind":"Variable","name":{"kind":"Name","value":"error"}}},{"kind":"Argument","name":{"kind":"Name","value":"state"},"value":{"kind":"Variable","name":{"kind":"Name","value":"state"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"success"}},{"kind":"Field","name":{"kind":"Name","value":"errorId"}}]}}]}}]} as unknown as DocumentNode<CompleteOAuth2FlowMutation, CompleteOAuth2FlowMutationVariables>;
export const CategoriesPageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"categoriesPage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"first"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"after"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"categories"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"Variable","name":{"kind":"Name","value":"first"}}},{"kind":"Argument","name":{"kind":"Name","value":"after"},"value":{"kind":"Variable","name":{"kind":"Name","value":"after"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"cursor"}},{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"description"}},{"kind":"Field","name":{"kind":"Name","value":"income"}},{"kind":"Field","name":{"kind":"Name","value":"expense"}}]}}]}},{"kind":"Field","name":{"kind":"Name","value":"pageInfo"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"PaginationPageInfo"}}]}},{"kind":"Field","name":{"kind":"Name","value":"totalCount"}}]}}]}}]}},{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<CategoriesPageQuery, CategoriesPageQueryVariables>;
export const CreateCategoryMutationDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createCategoryMutation"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"name"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"description"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"income"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Boolean"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"expense"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Boolean"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createCategory"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceId"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}},{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"name"}}},{"kind":"Argument","name":{"kind":"Name","value":"description"},"value":{"kind":"Variable","name":{"kind":"Name","value":"description"}}},{"kind":"Argument","name":{"kind":"Name","value":"income"},"value":{"kind":"Variable","name":{"kind":"Name","value":"income"}}},{"kind":"Argument","name":{"kind":"Name","value":"expense"},"value":{"kind":"Variable","name":{"kind":"Name","value":"expense"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<CreateCategoryMutationMutation, CreateCategoryMutationMutationVariables>;
export const GetCategoryForEditDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"getCategoryForEdit"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"categoryId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"category"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"categoryId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"description"}},{"kind":"Field","name":{"kind":"Name","value":"income"}},{"kind":"Field","name":{"kind":"Name","value":"expense"}}]}}]}}]}}]} as unknown as DocumentNode<GetCategoryForEditQuery, GetCategoryForEditQueryVariables>;
export const EditCategoryMutationDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"editCategoryMutation"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"name"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"description"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"income"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Boolean"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"expense"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Boolean"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"editCategory"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceId"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}},{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}},{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"name"}}},{"kind":"Argument","name":{"kind":"Name","value":"description"},"value":{"kind":"Variable","name":{"kind":"Name","value":"description"}}},{"kind":"Argument","name":{"kind":"Name","value":"income"},"value":{"kind":"Variable","name":{"kind":"Name","value":"income"}}},{"kind":"Argument","name":{"kind":"Name","value":"expense"},"value":{"kind":"Variable","name":{"kind":"Name","value":"expense"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<EditCategoryMutationMutation, EditCategoryMutationMutationVariables>;
export const CustomersPageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"customersPage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"first"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"after"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"customers"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"Variable","name":{"kind":"Name","value":"first"}}},{"kind":"Argument","name":{"kind":"Name","value":"after"},"value":{"kind":"Variable","name":{"kind":"Name","value":"after"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"cursor"}},{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}}]}}]}},{"kind":"Field","name":{"kind":"Name","value":"pageInfo"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"PaginationPageInfo"}}]}},{"kind":"Field","name":{"kind":"Name","value":"totalCount"}}]}}]}}]}},{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<CustomersPageQuery, CustomersPageQueryVariables>;
export const GetGeneralTaxForEditDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"getGeneralTaxForEdit"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"taxId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"generalTax"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"taxId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"description"}},{"kind":"Field","name":{"kind":"Name","value":"rateInBps"}}]}}]}}]}}]} as unknown as DocumentNode<GetGeneralTaxForEditQuery, GetGeneralTaxForEditQueryVariables>;
export const CreateGeneralTaxMutationDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createGeneralTaxMutation"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"title"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"description"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"rateInBps"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createGeneralTax"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceId"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}},{"kind":"Argument","name":{"kind":"Name","value":"title"},"value":{"kind":"Variable","name":{"kind":"Name","value":"title"}}},{"kind":"Argument","name":{"kind":"Name","value":"description"},"value":{"kind":"Variable","name":{"kind":"Name","value":"description"}}},{"kind":"Argument","name":{"kind":"Name","value":"rateInBps"},"value":{"kind":"Variable","name":{"kind":"Name","value":"rateInBps"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<CreateGeneralTaxMutationMutation, CreateGeneralTaxMutationMutationVariables>;
export const EditGeneralTaxMutationDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"editGeneralTaxMutation"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"title"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"description"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"rateInBps"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"editGeneralTax"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceId"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}},{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}},{"kind":"Argument","name":{"kind":"Name","value":"title"},"value":{"kind":"Variable","name":{"kind":"Name","value":"title"}}},{"kind":"Argument","name":{"kind":"Name","value":"description"},"value":{"kind":"Variable","name":{"kind":"Name","value":"description"}}},{"kind":"Argument","name":{"kind":"Name","value":"rateInBps"},"value":{"kind":"Variable","name":{"kind":"Name","value":"rateInBps"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<EditGeneralTaxMutationMutation, EditGeneralTaxMutationMutationVariables>;
export const GeneralTaxesPageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"generalTaxesPage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"first"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"after"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"generalTaxes"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"Variable","name":{"kind":"Name","value":"first"}}},{"kind":"Argument","name":{"kind":"Name","value":"after"},"value":{"kind":"Variable","name":{"kind":"Name","value":"after"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"cursor"}},{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"description"}},{"kind":"Field","name":{"kind":"Name","value":"rateInBps"}}]}}]}},{"kind":"Field","name":{"kind":"Name","value":"pageInfo"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"PaginationPageInfo"}}]}},{"kind":"Field","name":{"kind":"Name","value":"totalCount"}}]}}]}}]}},{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<GeneralTaxesPageQuery, GeneralTaxesPageQueryVariables>;
export const WorkspaceForEditorDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"workspaceForEditor"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}}]} as unknown as DocumentNode<WorkspaceForEditorQuery, WorkspaceForEditorQueryVariables>;
export const CreateWorkspaceEditorDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createWorkspaceEditor"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"name"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"defaultCurrency"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createWorkspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"name"}}},{"kind":"Argument","name":{"kind":"Name","value":"defaultCurrency"},"value":{"kind":"Variable","name":{"kind":"Name","value":"defaultCurrency"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}}]} as unknown as DocumentNode<CreateWorkspaceEditorMutation, CreateWorkspaceEditorMutationVariables>;
export const EditWorkspaceEditorDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"editWorkspaceEditor"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"name"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"editWorkspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}},{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"name"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}}]} as unknown as DocumentNode<EditWorkspaceEditorMutation, EditWorkspaceEditorMutationVariables>;
export const WorkspacesPageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"workspacesPage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"first"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"after"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspaces"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"Variable","name":{"kind":"Name","value":"first"}}},{"kind":"Argument","name":{"kind":"Name","value":"after"},"value":{"kind":"Variable","name":{"kind":"Name","value":"after"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"cursor"}},{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}},{"kind":"Field","name":{"kind":"Name","value":"pageInfo"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"PaginationPageInfo"}}]}},{"kind":"Field","name":{"kind":"Name","value":"totalCount"}}]}}]}},{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<WorkspacesPageQuery, WorkspacesPageQueryVariables>;
export const RefreshAccessTokenDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"refreshAccessToken"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"refreshAccessToken"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"accessToken"}}]}}]}}]} as unknown as DocumentNode<RefreshAccessTokenMutation, RefreshAccessTokenMutationVariables>;
export const CreateAccessTokenByCredentialsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createAccessTokenByCredentials"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"userName"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"password"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"issueRefreshTokenCookie"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"Boolean"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createAccessTokenByCredentials"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"userName"},"value":{"kind":"Variable","name":{"kind":"Name","value":"userName"}}},{"kind":"Argument","name":{"kind":"Name","value":"password"},"value":{"kind":"Variable","name":{"kind":"Name","value":"password"}}},{"kind":"Argument","name":{"kind":"Name","value":"issueRefreshTokenCookie"},"value":{"kind":"Variable","name":{"kind":"Name","value":"issueRefreshTokenCookie"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"accessToken"}}]}}]}}]} as unknown as DocumentNode<CreateAccessTokenByCredentialsMutation, CreateAccessTokenByCredentialsMutationVariables>;
export const CreateAccessTokenByWorkspaceAccessTokenDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createAccessTokenByWorkspaceAccessToken"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceAccessToken"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createAccessTokenByWorkspaceAccessToken"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceAccessToken"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceAccessToken"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"accessToken"}},{"kind":"Field","name":{"kind":"Name","value":"workspace"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}}]}}]} as unknown as DocumentNode<CreateAccessTokenByWorkspaceAccessTokenMutation, CreateAccessTokenByWorkspaceAccessTokenMutationVariables>;
export const AllWorkspacesDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"allWorkspaces"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"first"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspaces"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"Variable","name":{"kind":"Name","value":"first"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}}]}}]}}]} as unknown as DocumentNode<AllWorkspacesQuery, AllWorkspacesQueryVariables>;
export const WorkspaceByIdDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"workspaceById"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspace"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}}]} as unknown as DocumentNode<WorkspaceByIdQuery, WorkspaceByIdQueryVariables>;
export const UserProfileBootstrapDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"userProfileBootstrap"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}}]}}]}}]} as unknown as DocumentNode<UserProfileBootstrapQuery, UserProfileBootstrapQueryVariables>;