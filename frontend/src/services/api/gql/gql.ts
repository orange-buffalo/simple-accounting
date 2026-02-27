/* eslint-disable */
import * as types from './graphql';
import type { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';

/**
 * Map of all GraphQL operations in the project.
 *
 * This map has several performance disadvantages:
 * 1. It is not tree-shakeable, so it will include all operations in the project.
 * 2. It is not minifiable, so the string of a GraphQL query will be multiple times inside the bundle.
 * 3. It does not support dead code elimination, so it will add unused operations.
 *
 * Therefore it is highly recommended to use the babel or swc plugin for production.
 * Learn more about it here: https://the-guild.dev/graphql/codegen/plugins/presets/preset-client#reducing-bundle-size
 */
type Documents = {
    "\n    query documentsStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n    }\n  ": typeof types.DocumentsStorageStatusDocument,
    "\n    query userProfile {\n      userProfile {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": typeof types.UserProfileDocument,
    "\n    mutation changePassword($currentPassword: String!, $newPassword: String!) {\n      changePassword(currentPassword: $currentPassword, newPassword: $newPassword) {\n        success\n      }\n    }\n  ": typeof types.ChangePasswordDocument,
    "\n    mutation updateProfile($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": typeof types.UpdateProfileDocument,
    "\n    mutation updateProfileLanguage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": typeof types.UpdateProfileLanguageDocument,
    "\n    mutation completeOAuth2Flow($code: String, $error: String, $state: String!) {\n      completeOAuth2Flow(code: $code, error: $error, state: $state) {\n        success\n        errorId\n      }\n    }\n  ": typeof types.CompleteOAuth2FlowDocument,
    "\n    mutation refreshAccessToken {\n        refreshAccessToken {\n            accessToken\n        }\n    }\n": typeof types.RefreshAccessTokenDocument,
    "\n  mutation createAccessTokenByCredentials(\n    $userName: String!\n    $password: String!\n    $issueRefreshTokenCookie: Boolean\n  ) {\n    createAccessTokenByCredentials(\n      userName: $userName\n      password: $password\n      issueRefreshTokenCookie: $issueRefreshTokenCookie\n    ) {\n      accessToken\n    }\n  }\n": typeof types.CreateAccessTokenByCredentialsDocument,
    "\n  mutation createAccessTokenByWorkspaceAccessToken(\n    $workspaceAccessToken: String!\n  ) {\n    createAccessTokenByWorkspaceAccessToken(\n      workspaceAccessToken: $workspaceAccessToken\n    ) {\n      accessToken\n    }\n  }\n": typeof types.CreateAccessTokenByWorkspaceAccessTokenDocument,
    "\n  query userProfileBootstrap {\n    userProfile {\n      i18n {\n        language\n        locale\n      }\n    }\n  }\n": typeof types.UserProfileBootstrapDocument,
};
const documents: Documents = {
    "\n    query documentsStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n    }\n  ": types.DocumentsStorageStatusDocument,
    "\n    query userProfile {\n      userProfile {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": types.UserProfileDocument,
    "\n    mutation changePassword($currentPassword: String!, $newPassword: String!) {\n      changePassword(currentPassword: $currentPassword, newPassword: $newPassword) {\n        success\n      }\n    }\n  ": types.ChangePasswordDocument,
    "\n    mutation updateProfile($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": types.UpdateProfileDocument,
    "\n    mutation updateProfileLanguage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": types.UpdateProfileLanguageDocument,
    "\n    mutation completeOAuth2Flow($code: String, $error: String, $state: String!) {\n      completeOAuth2Flow(code: $code, error: $error, state: $state) {\n        success\n        errorId\n      }\n    }\n  ": types.CompleteOAuth2FlowDocument,
    "\n    mutation refreshAccessToken {\n        refreshAccessToken {\n            accessToken\n        }\n    }\n": types.RefreshAccessTokenDocument,
    "\n  mutation createAccessTokenByCredentials(\n    $userName: String!\n    $password: String!\n    $issueRefreshTokenCookie: Boolean\n  ) {\n    createAccessTokenByCredentials(\n      userName: $userName\n      password: $password\n      issueRefreshTokenCookie: $issueRefreshTokenCookie\n    ) {\n      accessToken\n    }\n  }\n": types.CreateAccessTokenByCredentialsDocument,
    "\n  mutation createAccessTokenByWorkspaceAccessToken(\n    $workspaceAccessToken: String!\n  ) {\n    createAccessTokenByWorkspaceAccessToken(\n      workspaceAccessToken: $workspaceAccessToken\n    ) {\n      accessToken\n    }\n  }\n": types.CreateAccessTokenByWorkspaceAccessTokenDocument,
    "\n  query userProfileBootstrap {\n    userProfile {\n      i18n {\n        language\n        locale\n      }\n    }\n  }\n": types.UserProfileBootstrapDocument,
};

/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 *
 *
 * @example
 * ```ts
 * const query = graphql(`query GetUser($id: ID!) { user(id: $id) { name } }`);
 * ```
 *
 * The query argument is unknown!
 * Please regenerate the types.
 */
export function graphql(source: string): unknown;

/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    query documentsStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n    }\n  "): (typeof documents)["\n    query documentsStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    query userProfile {\n      userProfile {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  "): (typeof documents)["\n    query userProfile {\n      userProfile {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    mutation changePassword($currentPassword: String!, $newPassword: String!) {\n      changePassword(currentPassword: $currentPassword, newPassword: $newPassword) {\n        success\n      }\n    }\n  "): (typeof documents)["\n    mutation changePassword($currentPassword: String!, $newPassword: String!) {\n      changePassword(currentPassword: $currentPassword, newPassword: $newPassword) {\n        success\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    mutation updateProfile($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  "): (typeof documents)["\n    mutation updateProfile($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    mutation updateProfileLanguage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  "): (typeof documents)["\n    mutation updateProfileLanguage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    mutation completeOAuth2Flow($code: String, $error: String, $state: String!) {\n      completeOAuth2Flow(code: $code, error: $error, state: $state) {\n        success\n        errorId\n      }\n    }\n  "): (typeof documents)["\n    mutation completeOAuth2Flow($code: String, $error: String, $state: String!) {\n      completeOAuth2Flow(code: $code, error: $error, state: $state) {\n        success\n        errorId\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    mutation refreshAccessToken {\n        refreshAccessToken {\n            accessToken\n        }\n    }\n"): (typeof documents)["\n    mutation refreshAccessToken {\n        refreshAccessToken {\n            accessToken\n        }\n    }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation createAccessTokenByCredentials(\n    $userName: String!\n    $password: String!\n    $issueRefreshTokenCookie: Boolean\n  ) {\n    createAccessTokenByCredentials(\n      userName: $userName\n      password: $password\n      issueRefreshTokenCookie: $issueRefreshTokenCookie\n    ) {\n      accessToken\n    }\n  }\n"): (typeof documents)["\n  mutation createAccessTokenByCredentials(\n    $userName: String!\n    $password: String!\n    $issueRefreshTokenCookie: Boolean\n  ) {\n    createAccessTokenByCredentials(\n      userName: $userName\n      password: $password\n      issueRefreshTokenCookie: $issueRefreshTokenCookie\n    ) {\n      accessToken\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  mutation createAccessTokenByWorkspaceAccessToken(\n    $workspaceAccessToken: String!\n  ) {\n    createAccessTokenByWorkspaceAccessToken(\n      workspaceAccessToken: $workspaceAccessToken\n    ) {\n      accessToken\n    }\n  }\n"): (typeof documents)["\n  mutation createAccessTokenByWorkspaceAccessToken(\n    $workspaceAccessToken: String!\n  ) {\n    createAccessTokenByWorkspaceAccessToken(\n      workspaceAccessToken: $workspaceAccessToken\n    ) {\n      accessToken\n    }\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  query userProfileBootstrap {\n    userProfile {\n      i18n {\n        language\n        locale\n      }\n    }\n  }\n"): (typeof documents)["\n  query userProfileBootstrap {\n    userProfile {\n      i18n {\n        language\n        locale\n      }\n    }\n  }\n"];

export function graphql(source: string) {
  return (documents as any)[source] ?? {};
}

export type DocumentType<TDocumentNode extends DocumentNode<any, any>> = TDocumentNode extends DocumentNode<  infer TType,  any>  ? TType  : never;