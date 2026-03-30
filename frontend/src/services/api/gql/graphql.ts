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

/** Category of incomes or expenses. */
export type Category = {
  __typename?: 'Category';
  /** Name of the category. */
  name: Scalars['String']['output'];
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
};

/** A customer in a workspace. */
export type Customer = {
  __typename?: 'Customer';
  /** ID of the customer. */
  id: Scalars['Int']['output'];
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
  id: Scalars['Int']['output'];
  /** MIME type of the document. */
  mimeType: Scalars['String']['output'];
  /** Name of the document. */
  name: Scalars['String']['output'];
  /** Size of the document in bytes. */
  sizeInBytes?: Maybe<Scalars['Int']['output']>;
  /** ID of the storage where the document is stored. */
  storageId: Scalars['String']['output'];
  /** Time when the document was uploaded, as ISO 8601 timestamp. */
  timeUploaded: Scalars['String']['output'];
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
  relatedEntityId: Scalars['Int']['output'];
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
  /** Invalidates the refresh token cookie, effectively logging out the current user. */
  invalidateRefreshToken: Scalars['Boolean']['output'];
  /** Refreshes the access token using the refresh token from cookies or current authentication. Returns a response with either a valid access token or null if authentication fails. */
  refreshAccessToken: RefreshAccessTokenResponse;
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
  /** Returns all customers in a workspace with cursor-based pagination. */
  customers: CustomersConnection;
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


export type QueryCustomersArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  first: Scalars['Int']['input'];
  workspaceId: Scalars['Int']['input'];
};


export type QueryWorkspaceArgs = {
  id: Scalars['Int']['input'];
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
  /** Categories in this workspace. */
  categories: Array<Category>;
  /** Customers in this workspace with cursor-based pagination. */
  customers: CustomersConnection;
  /** Default currency of the workspace. */
  defaultCurrency: Scalars['String']['output'];
  /** Documents in this workspace with cursor-based pagination. */
  documents: DocumentsConnection;
  /** Expenses in this workspace. */
  expenses: Array<Expense>;
  /** ID of the workspace. */
  id: Scalars['Int']['output'];
  /** Name of the workspace. */
  name: Scalars['String']['output'];
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

export type DownloadDocumentStoragesQueryVariables = Exact<{ [key: string]: never; }>;


export type DownloadDocumentStoragesQuery = { __typename?: 'Query', getDownloadDocumentStorages: Array<{ __typename?: 'DownloadDocumentStorageResponse', id: string }> };

export type DocumentsUploadStorageStatusQueryVariables = Exact<{ [key: string]: never; }>;


export type DocumentsUploadStorageStatusQuery = { __typename?: 'Query', documentsStorageStatus: { __typename?: 'DocumentsStorageStatusResponse', active: boolean }, getDownloadDocumentStorages: Array<{ __typename?: 'DownloadDocumentStorageResponse', id: string }> };

export type GoogleDriveStorageIntegrationStatusQueryVariables = Exact<{ [key: string]: never; }>;


export type GoogleDriveStorageIntegrationStatusQuery = { __typename?: 'Query', googleDriveStorageIntegrationStatus: { __typename?: 'GoogleDriveStorageIntegrationStatusResponse', authorizationRequired: boolean, authorizationUrl?: string | null, folderId?: string | null, folderName?: string | null } };

export type DocumentsStorageStatusQueryVariables = Exact<{ [key: string]: never; }>;


export type DocumentsStorageStatusQuery = { __typename?: 'Query', documentsStorageStatus: { __typename?: 'DocumentsStorageStatusResponse', active: boolean } };

export type PaginationPageInfoFragment = { __typename?: 'PageInfo', endCursor?: string | null, hasNextPage: boolean, hasPreviousPage: boolean, startCursor?: string | null } & { ' $fragmentName'?: 'PaginationPageInfoFragment' };

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

export type CustomersPageQueryVariables = Exact<{
  workspaceId: Scalars['Int']['input'];
  first: Scalars['Int']['input'];
  after?: InputMaybe<Scalars['String']['input']>;
}>;


export type CustomersPageQuery = { __typename?: 'Query', customers: { __typename?: 'CustomersConnection', totalCount: number, edges: Array<{ __typename?: 'CustomerEdge', cursor: string, node: { __typename?: 'Customer', id: number, name: string } }>, pageInfo: (
      { __typename?: 'PageInfo' }
      & { ' $fragmentRefs'?: { 'PaginationPageInfoFragment': PaginationPageInfoFragment } }
    ) } };

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


export type CreateAccessTokenByWorkspaceAccessTokenMutation = { __typename?: 'Mutation', createAccessTokenByWorkspaceAccessToken: { __typename?: 'CreateAccessTokenByWorkspaceAccessTokenResponse', accessToken: string } };

export type UserProfileBootstrapQueryVariables = Exact<{ [key: string]: never; }>;


export type UserProfileBootstrapQuery = { __typename?: 'Query', userProfile: { __typename?: 'UserProfile', i18n: { __typename?: 'I18nSettings', language: string, locale: string } } };

export const PaginationPageInfoFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<PaginationPageInfoFragment, unknown>;
export const DownloadDocumentStoragesDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"downloadDocumentStorages"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"getDownloadDocumentStorages"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<DownloadDocumentStoragesQuery, DownloadDocumentStoragesQueryVariables>;
export const DocumentsUploadStorageStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"documentsUploadStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"active"}}]}},{"kind":"Field","name":{"kind":"Name","value":"getDownloadDocumentStorages"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}}]}}]}}]} as unknown as DocumentNode<DocumentsUploadStorageStatusQuery, DocumentsUploadStorageStatusQueryVariables>;
export const GoogleDriveStorageIntegrationStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"googleDriveStorageIntegrationStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"googleDriveStorageIntegrationStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"authorizationRequired"}},{"kind":"Field","name":{"kind":"Name","value":"authorizationUrl"}},{"kind":"Field","name":{"kind":"Name","value":"folderId"}},{"kind":"Field","name":{"kind":"Name","value":"folderName"}}]}}]}}]} as unknown as DocumentNode<GoogleDriveStorageIntegrationStatusQuery, GoogleDriveStorageIntegrationStatusQueryVariables>;
export const DocumentsStorageStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"documentsStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"active"}}]}}]}}]} as unknown as DocumentNode<DocumentsStorageStatusQuery, DocumentsStorageStatusQueryVariables>;
export const UserProfileLoginDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"userProfileLogin"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}}]}}]}}]} as unknown as DocumentNode<UserProfileLoginQuery, UserProfileLoginQueryVariables>;
export const UserProfileDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorage"}},{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}},{"kind":"Field","name":{"kind":"Name","value":"userName"}}]}}]}}]} as unknown as DocumentNode<UserProfileQuery, UserProfileQueryVariables>;
export const ChangePasswordDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"changePassword"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"currentPassword"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"newPassword"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"changePassword"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"currentPassword"},"value":{"kind":"Variable","name":{"kind":"Name","value":"currentPassword"}}},{"kind":"Argument","name":{"kind":"Name","value":"newPassword"},"value":{"kind":"Variable","name":{"kind":"Name","value":"newPassword"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"success"}}]}}]}}]} as unknown as DocumentNode<ChangePasswordMutation, ChangePasswordMutationVariables>;
export const DocumentsStorageConfigDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"documentsStorageConfig"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorageStatistics"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"storageId"}},{"kind":"Field","name":{"kind":"Name","value":"documentsCount"}}]}},{"kind":"Field","name":{"kind":"Name","value":"systemSettings"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"localFileSystemDocumentsStorageEnabled"}}]}}]}}]} as unknown as DocumentNode<DocumentsStorageConfigQuery, DocumentsStorageConfigQueryVariables>;
export const UpdateProfileStorageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"updateProfileStorage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"locale"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"language"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"updateProfile"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"documentsStorage"},"value":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}}},{"kind":"Argument","name":{"kind":"Name","value":"locale"},"value":{"kind":"Variable","name":{"kind":"Name","value":"locale"}}},{"kind":"Argument","name":{"kind":"Name","value":"language"},"value":{"kind":"Variable","name":{"kind":"Name","value":"language"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorage"}},{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}},{"kind":"Field","name":{"kind":"Name","value":"userName"}}]}}]}}]} as unknown as DocumentNode<UpdateProfileStorageMutation, UpdateProfileStorageMutationVariables>;
export const UpdateProfileLanguageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"updateProfileLanguage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"locale"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"language"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"updateProfile"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"documentsStorage"},"value":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}}},{"kind":"Argument","name":{"kind":"Name","value":"locale"},"value":{"kind":"Variable","name":{"kind":"Name","value":"locale"}}},{"kind":"Argument","name":{"kind":"Name","value":"language"},"value":{"kind":"Variable","name":{"kind":"Name","value":"language"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorage"}},{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}},{"kind":"Field","name":{"kind":"Name","value":"userName"}}]}}]}}]} as unknown as DocumentNode<UpdateProfileLanguageMutation, UpdateProfileLanguageMutationVariables>;
export const CompleteOAuth2FlowDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"completeOAuth2Flow"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"code"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"error"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"state"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"completeOAuth2Flow"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"code"},"value":{"kind":"Variable","name":{"kind":"Name","value":"code"}}},{"kind":"Argument","name":{"kind":"Name","value":"error"},"value":{"kind":"Variable","name":{"kind":"Name","value":"error"}}},{"kind":"Argument","name":{"kind":"Name","value":"state"},"value":{"kind":"Variable","name":{"kind":"Name","value":"state"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"success"}},{"kind":"Field","name":{"kind":"Name","value":"errorId"}}]}}]}}]} as unknown as DocumentNode<CompleteOAuth2FlowMutation, CompleteOAuth2FlowMutationVariables>;
export const CustomersPageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"customersPage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"first"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"after"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"customers"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceId"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceId"}}},{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"Variable","name":{"kind":"Name","value":"first"}}},{"kind":"Argument","name":{"kind":"Name","value":"after"},"value":{"kind":"Variable","name":{"kind":"Name","value":"after"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"cursor"}},{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}}]}}]}},{"kind":"Field","name":{"kind":"Name","value":"pageInfo"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"PaginationPageInfo"}}]}},{"kind":"Field","name":{"kind":"Name","value":"totalCount"}}]}}]}},{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<CustomersPageQuery, CustomersPageQueryVariables>;
export const WorkspacesPageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"workspacesPage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"first"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"after"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"workspaces"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"first"},"value":{"kind":"Variable","name":{"kind":"Name","value":"first"}}},{"kind":"Argument","name":{"kind":"Name","value":"after"},"value":{"kind":"Variable","name":{"kind":"Name","value":"after"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"edges"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"cursor"}},{"kind":"Field","name":{"kind":"Name","value":"node"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"defaultCurrency"}}]}}]}},{"kind":"Field","name":{"kind":"Name","value":"pageInfo"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"PaginationPageInfo"}}]}},{"kind":"Field","name":{"kind":"Name","value":"totalCount"}}]}}]}},{"kind":"FragmentDefinition","name":{"kind":"Name","value":"PaginationPageInfo"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"PageInfo"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"endCursor"}},{"kind":"Field","name":{"kind":"Name","value":"hasNextPage"}},{"kind":"Field","name":{"kind":"Name","value":"hasPreviousPage"}},{"kind":"Field","name":{"kind":"Name","value":"startCursor"}}]}}]} as unknown as DocumentNode<WorkspacesPageQuery, WorkspacesPageQueryVariables>;
export const RefreshAccessTokenDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"refreshAccessToken"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"refreshAccessToken"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"accessToken"}}]}}]}}]} as unknown as DocumentNode<RefreshAccessTokenMutation, RefreshAccessTokenMutationVariables>;
export const CreateAccessTokenByCredentialsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createAccessTokenByCredentials"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"userName"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"password"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"issueRefreshTokenCookie"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"Boolean"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createAccessTokenByCredentials"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"userName"},"value":{"kind":"Variable","name":{"kind":"Name","value":"userName"}}},{"kind":"Argument","name":{"kind":"Name","value":"password"},"value":{"kind":"Variable","name":{"kind":"Name","value":"password"}}},{"kind":"Argument","name":{"kind":"Name","value":"issueRefreshTokenCookie"},"value":{"kind":"Variable","name":{"kind":"Name","value":"issueRefreshTokenCookie"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"accessToken"}}]}}]}}]} as unknown as DocumentNode<CreateAccessTokenByCredentialsMutation, CreateAccessTokenByCredentialsMutationVariables>;
export const CreateAccessTokenByWorkspaceAccessTokenDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createAccessTokenByWorkspaceAccessToken"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceAccessToken"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createAccessTokenByWorkspaceAccessToken"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceAccessToken"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceAccessToken"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"accessToken"}}]}}]}}]} as unknown as DocumentNode<CreateAccessTokenByWorkspaceAccessTokenMutation, CreateAccessTokenByWorkspaceAccessTokenMutationVariables>;
export const UserProfileBootstrapDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"userProfileBootstrap"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}}]}}]}}]} as unknown as DocumentNode<UserProfileBootstrapQuery, UserProfileBootstrapQueryVariables>;