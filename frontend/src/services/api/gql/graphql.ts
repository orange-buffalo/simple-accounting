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
export type CategoryGqlDto = {
  __typename?: 'CategoryGqlDto';
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

/** Documents storage status for the current user. */
export type DocumentsStorageStatusResponse = {
  __typename?: 'DocumentsStorageStatusResponse';
  /** Whether the documents storage is active and available. */
  active: Scalars['Boolean']['output'];
};

/** Business expense. */
export type ExpenseGqlDto = {
  __typename?: 'ExpenseGqlDto';
  /** Category of the expense. */
  category?: Maybe<CategoryGqlDto>;
  /** Title of the expense. */
  title: Scalars['String']['output'];
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

export type Query = {
  __typename?: 'Query';
  /** Returns the current user's documents storage status. */
  documentsStorageStatus: DocumentsStorageStatusResponse;
  /** Returns the current user profile information. Current is defined as the user that is authenticated in the current request. */
  userProfile: UserProfile;
  /** Returns all workspaces accessible by the current user. */
  workspaces: Array<WorkspaceGqlDto>;
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
  /** Indicates that one or more input fields failed validation constraints. */
  FieldValidationFailure = 'FIELD_VALIDATION_FAILURE',
  /** Indicates that the request requires authentication or the user is not authorized to perform the operation. */
  NotAuthorized = 'NOT_AUTHORIZED'
}

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
export type WorkspaceGqlDto = {
  __typename?: 'WorkspaceGqlDto';
  /** Categories in this workspace. */
  categories: Array<CategoryGqlDto>;
  /** Expenses in this workspace. */
  expenses: Array<ExpenseGqlDto>;
  /** Name of the workspace. */
  name: Scalars['String']['output'];
};

export type DocumentsStorageStatusQueryVariables = Exact<{ [key: string]: never; }>;


export type DocumentsStorageStatusQuery = { __typename?: 'Query', documentsStorageStatus: { __typename?: 'DocumentsStorageStatusResponse', active: boolean } };

export type UserProfileLoginQueryVariables = Exact<{ [key: string]: never; }>;


export type UserProfileLoginQuery = { __typename?: 'Query', userProfile: { __typename?: 'UserProfile', i18n: { __typename?: 'I18nSettings', language: string, locale: string } } };

export type UserProfileQueryVariables = Exact<{ [key: string]: never; }>;


export type UserProfileQuery = { __typename?: 'Query', userProfile: { __typename?: 'UserProfile', documentsStorage?: string | null, userName: string, i18n: { __typename?: 'I18nSettings', language: string, locale: string } } };

export type ChangePasswordMutationVariables = Exact<{
  currentPassword: Scalars['String']['input'];
  newPassword: Scalars['String']['input'];
}>;


export type ChangePasswordMutation = { __typename?: 'Mutation', changePassword: { __typename?: 'ChangePasswordResponse', success: boolean } };

export type UpdateProfileMutationVariables = Exact<{
  documentsStorage?: InputMaybe<Scalars['String']['input']>;
  locale: Scalars['String']['input'];
  language: Scalars['String']['input'];
}>;


export type UpdateProfileMutation = { __typename?: 'Mutation', updateProfile: { __typename?: 'UserProfile', documentsStorage?: string | null, userName: string, i18n: { __typename?: 'I18nSettings', language: string, locale: string } } };

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


export const DocumentsStorageStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"documentsStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorageStatus"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"active"}}]}}]}}]} as unknown as DocumentNode<DocumentsStorageStatusQuery, DocumentsStorageStatusQueryVariables>;
export const UserProfileLoginDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"userProfileLogin"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}}]}}]}}]} as unknown as DocumentNode<UserProfileLoginQuery, UserProfileLoginQueryVariables>;
export const UserProfileDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorage"}},{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}},{"kind":"Field","name":{"kind":"Name","value":"userName"}}]}}]}}]} as unknown as DocumentNode<UserProfileQuery, UserProfileQueryVariables>;
export const ChangePasswordDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"changePassword"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"currentPassword"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"newPassword"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"changePassword"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"currentPassword"},"value":{"kind":"Variable","name":{"kind":"Name","value":"currentPassword"}}},{"kind":"Argument","name":{"kind":"Name","value":"newPassword"},"value":{"kind":"Variable","name":{"kind":"Name","value":"newPassword"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"success"}}]}}]}}]} as unknown as DocumentNode<ChangePasswordMutation, ChangePasswordMutationVariables>;
export const UpdateProfileDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"updateProfile"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"locale"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"language"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"updateProfile"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"documentsStorage"},"value":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}}},{"kind":"Argument","name":{"kind":"Name","value":"locale"},"value":{"kind":"Variable","name":{"kind":"Name","value":"locale"}}},{"kind":"Argument","name":{"kind":"Name","value":"language"},"value":{"kind":"Variable","name":{"kind":"Name","value":"language"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorage"}},{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}},{"kind":"Field","name":{"kind":"Name","value":"userName"}}]}}]}}]} as unknown as DocumentNode<UpdateProfileMutation, UpdateProfileMutationVariables>;
export const UpdateProfileLanguageDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"updateProfileLanguage"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"locale"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"language"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"updateProfile"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"documentsStorage"},"value":{"kind":"Variable","name":{"kind":"Name","value":"documentsStorage"}}},{"kind":"Argument","name":{"kind":"Name","value":"locale"},"value":{"kind":"Variable","name":{"kind":"Name","value":"locale"}}},{"kind":"Argument","name":{"kind":"Name","value":"language"},"value":{"kind":"Variable","name":{"kind":"Name","value":"language"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"documentsStorage"}},{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}},{"kind":"Field","name":{"kind":"Name","value":"userName"}}]}}]}}]} as unknown as DocumentNode<UpdateProfileLanguageMutation, UpdateProfileLanguageMutationVariables>;
export const CompleteOAuth2FlowDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"completeOAuth2Flow"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"code"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"error"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"state"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"completeOAuth2Flow"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"code"},"value":{"kind":"Variable","name":{"kind":"Name","value":"code"}}},{"kind":"Argument","name":{"kind":"Name","value":"error"},"value":{"kind":"Variable","name":{"kind":"Name","value":"error"}}},{"kind":"Argument","name":{"kind":"Name","value":"state"},"value":{"kind":"Variable","name":{"kind":"Name","value":"state"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"success"}},{"kind":"Field","name":{"kind":"Name","value":"errorId"}}]}}]}}]} as unknown as DocumentNode<CompleteOAuth2FlowMutation, CompleteOAuth2FlowMutationVariables>;
export const RefreshAccessTokenDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"refreshAccessToken"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"refreshAccessToken"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"accessToken"}}]}}]}}]} as unknown as DocumentNode<RefreshAccessTokenMutation, RefreshAccessTokenMutationVariables>;
export const CreateAccessTokenByCredentialsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createAccessTokenByCredentials"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"userName"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"password"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"issueRefreshTokenCookie"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"Boolean"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createAccessTokenByCredentials"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"userName"},"value":{"kind":"Variable","name":{"kind":"Name","value":"userName"}}},{"kind":"Argument","name":{"kind":"Name","value":"password"},"value":{"kind":"Variable","name":{"kind":"Name","value":"password"}}},{"kind":"Argument","name":{"kind":"Name","value":"issueRefreshTokenCookie"},"value":{"kind":"Variable","name":{"kind":"Name","value":"issueRefreshTokenCookie"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"accessToken"}}]}}]}}]} as unknown as DocumentNode<CreateAccessTokenByCredentialsMutation, CreateAccessTokenByCredentialsMutationVariables>;
export const CreateAccessTokenByWorkspaceAccessTokenDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createAccessTokenByWorkspaceAccessToken"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"workspaceAccessToken"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createAccessTokenByWorkspaceAccessToken"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"workspaceAccessToken"},"value":{"kind":"Variable","name":{"kind":"Name","value":"workspaceAccessToken"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"accessToken"}}]}}]}}]} as unknown as DocumentNode<CreateAccessTokenByWorkspaceAccessTokenMutation, CreateAccessTokenByWorkspaceAccessTokenMutationVariables>;
export const UserProfileBootstrapDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"userProfileBootstrap"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"userProfile"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"i18n"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"language"}},{"kind":"Field","name":{"kind":"Name","value":"locale"}}]}}]}}]}}]} as unknown as DocumentNode<UserProfileBootstrapQuery, UserProfileBootstrapQueryVariables>;