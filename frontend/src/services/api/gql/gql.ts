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
    "\n    query downloadDocumentStorages {\n      getDownloadDocumentStorages {\n        id\n      }\n    }\n  ": typeof types.DownloadDocumentStoragesDocument,
    "\n    query documentsUploadStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n      getDownloadDocumentStorages {\n        id\n      }\n    }\n  ": typeof types.DocumentsUploadStorageStatusDocument,
    "\n    query googleDriveStorageIntegrationStatus {\n      googleDriveStorageIntegrationStatus {\n        authorizationRequired\n        authorizationUrl\n        folderId\n        folderName\n      }\n    }\n  ": typeof types.GoogleDriveStorageIntegrationStatusDocument,
    "\n    query documentsStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n    }\n  ": typeof types.DocumentsStorageStatusDocument,
    "\n  fragment PaginationPageInfo on PageInfo {\n    endCursor\n    hasNextPage\n    hasPreviousPage\n    startCursor\n  }\n": typeof types.PaginationPageInfoFragmentDoc,
    "\n    query documentsPage($workspaceId: Int!, $first: Int!, $after: String) {\n      workspace(id: $workspaceId) {\n        documents(first: $first, after: $after) {\n          edges {\n            cursor\n            node {\n              id\n              name\n              timeUploaded\n              storageId\n              usedBy {\n                type\n                relatedEntityId\n                displayName\n              }\n            }\n          }\n          pageInfo {\n            ...PaginationPageInfo\n          }\n          totalCount\n        }\n      }\n    }\n  ": typeof types.DocumentsPageDocument,
    "\n    query userProfileLogin {\n      userProfile {\n        i18n {\n          language\n          locale\n        }\n      }\n    }\n  ": typeof types.UserProfileLoginDocument,
    "\n    query userProfile {\n      userProfile {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": typeof types.UserProfileDocument,
    "\n    mutation changePassword($currentPassword: String!, $newPassword: String!) {\n      changePassword(currentPassword: $currentPassword, newPassword: $newPassword) {\n        success\n      }\n    }\n  ": typeof types.ChangePasswordDocument,
    "\n    query documentsStorageConfig {\n      documentsStorageStatistics {\n        storageId\n        documentsCount\n      }\n      systemSettings {\n        localFileSystemDocumentsStorageEnabled\n      }\n    }\n  ": typeof types.DocumentsStorageConfigDocument,
    "\n    mutation updateProfileStorage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": typeof types.UpdateProfileStorageDocument,
    "\n    mutation updateProfileLanguage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": typeof types.UpdateProfileLanguageDocument,
    "\n    mutation completeOAuth2Flow($code: String, $error: String, $state: String!) {\n      completeOAuth2Flow(code: $code, error: $error, state: $state) {\n        success\n        errorId\n      }\n    }\n  ": typeof types.CompleteOAuth2FlowDocument,
    "\n    query workspacesPage($first: Int!, $after: String) {\n      workspaces(first: $first, after: $after) {\n        edges {\n          cursor\n          node {\n            id\n            name\n            defaultCurrency\n          }\n        }\n        pageInfo {\n          ...PaginationPageInfo\n        }\n        totalCount\n      }\n    }\n  ": typeof types.WorkspacesPageDocument,
    "\n    mutation refreshAccessToken {\n        refreshAccessToken {\n            accessToken\n        }\n    }\n": typeof types.RefreshAccessTokenDocument,
    "\n  mutation createAccessTokenByCredentials(\n    $userName: String!\n    $password: String!\n    $issueRefreshTokenCookie: Boolean\n  ) {\n    createAccessTokenByCredentials(\n      userName: $userName\n      password: $password\n      issueRefreshTokenCookie: $issueRefreshTokenCookie\n    ) {\n      accessToken\n    }\n  }\n": typeof types.CreateAccessTokenByCredentialsDocument,
    "\n  mutation createAccessTokenByWorkspaceAccessToken(\n    $workspaceAccessToken: String!\n  ) {\n    createAccessTokenByWorkspaceAccessToken(\n      workspaceAccessToken: $workspaceAccessToken\n    ) {\n      accessToken\n    }\n  }\n": typeof types.CreateAccessTokenByWorkspaceAccessTokenDocument,
    "\n  query userProfileBootstrap {\n    userProfile {\n      i18n {\n        language\n        locale\n      }\n    }\n  }\n": typeof types.UserProfileBootstrapDocument,
};
const documents: Documents = {
    "\n    query downloadDocumentStorages {\n      getDownloadDocumentStorages {\n        id\n      }\n    }\n  ": types.DownloadDocumentStoragesDocument,
    "\n    query documentsUploadStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n      getDownloadDocumentStorages {\n        id\n      }\n    }\n  ": types.DocumentsUploadStorageStatusDocument,
    "\n    query googleDriveStorageIntegrationStatus {\n      googleDriveStorageIntegrationStatus {\n        authorizationRequired\n        authorizationUrl\n        folderId\n        folderName\n      }\n    }\n  ": types.GoogleDriveStorageIntegrationStatusDocument,
    "\n    query documentsStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n    }\n  ": types.DocumentsStorageStatusDocument,
    "\n  fragment PaginationPageInfo on PageInfo {\n    endCursor\n    hasNextPage\n    hasPreviousPage\n    startCursor\n  }\n": types.PaginationPageInfoFragmentDoc,
    "\n    query documentsPage($workspaceId: Int!, $first: Int!, $after: String) {\n      workspace(id: $workspaceId) {\n        documents(first: $first, after: $after) {\n          edges {\n            cursor\n            node {\n              id\n              name\n              timeUploaded\n              storageId\n              usedBy {\n                type\n                relatedEntityId\n                displayName\n              }\n            }\n          }\n          pageInfo {\n            ...PaginationPageInfo\n          }\n          totalCount\n        }\n      }\n    }\n  ": types.DocumentsPageDocument,
    "\n    query userProfileLogin {\n      userProfile {\n        i18n {\n          language\n          locale\n        }\n      }\n    }\n  ": types.UserProfileLoginDocument,
    "\n    query userProfile {\n      userProfile {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": types.UserProfileDocument,
    "\n    mutation changePassword($currentPassword: String!, $newPassword: String!) {\n      changePassword(currentPassword: $currentPassword, newPassword: $newPassword) {\n        success\n      }\n    }\n  ": types.ChangePasswordDocument,
    "\n    query documentsStorageConfig {\n      documentsStorageStatistics {\n        storageId\n        documentsCount\n      }\n      systemSettings {\n        localFileSystemDocumentsStorageEnabled\n      }\n    }\n  ": types.DocumentsStorageConfigDocument,
    "\n    mutation updateProfileStorage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": types.UpdateProfileStorageDocument,
    "\n    mutation updateProfileLanguage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  ": types.UpdateProfileLanguageDocument,
    "\n    mutation completeOAuth2Flow($code: String, $error: String, $state: String!) {\n      completeOAuth2Flow(code: $code, error: $error, state: $state) {\n        success\n        errorId\n      }\n    }\n  ": types.CompleteOAuth2FlowDocument,
    "\n    query workspacesPage($first: Int!, $after: String) {\n      workspaces(first: $first, after: $after) {\n        edges {\n          cursor\n          node {\n            id\n            name\n            defaultCurrency\n          }\n        }\n        pageInfo {\n          ...PaginationPageInfo\n        }\n        totalCount\n      }\n    }\n  ": types.WorkspacesPageDocument,
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
export function graphql(source: "\n    query downloadDocumentStorages {\n      getDownloadDocumentStorages {\n        id\n      }\n    }\n  "): (typeof documents)["\n    query downloadDocumentStorages {\n      getDownloadDocumentStorages {\n        id\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    query documentsUploadStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n      getDownloadDocumentStorages {\n        id\n      }\n    }\n  "): (typeof documents)["\n    query documentsUploadStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n      getDownloadDocumentStorages {\n        id\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    query googleDriveStorageIntegrationStatus {\n      googleDriveStorageIntegrationStatus {\n        authorizationRequired\n        authorizationUrl\n        folderId\n        folderName\n      }\n    }\n  "): (typeof documents)["\n    query googleDriveStorageIntegrationStatus {\n      googleDriveStorageIntegrationStatus {\n        authorizationRequired\n        authorizationUrl\n        folderId\n        folderName\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    query documentsStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n    }\n  "): (typeof documents)["\n    query documentsStorageStatus {\n      documentsStorageStatus {\n        active\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n  fragment PaginationPageInfo on PageInfo {\n    endCursor\n    hasNextPage\n    hasPreviousPage\n    startCursor\n  }\n"): (typeof documents)["\n  fragment PaginationPageInfo on PageInfo {\n    endCursor\n    hasNextPage\n    hasPreviousPage\n    startCursor\n  }\n"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    query documentsPage($workspaceId: Int!, $first: Int!, $after: String) {\n      workspace(id: $workspaceId) {\n        documents(first: $first, after: $after) {\n          edges {\n            cursor\n            node {\n              id\n              name\n              timeUploaded\n              storageId\n              usedBy {\n                type\n                relatedEntityId\n                displayName\n              }\n            }\n          }\n          pageInfo {\n            ...PaginationPageInfo\n          }\n          totalCount\n        }\n      }\n    }\n  "): (typeof documents)["\n    query documentsPage($workspaceId: Int!, $first: Int!, $after: String) {\n      workspace(id: $workspaceId) {\n        documents(first: $first, after: $after) {\n          edges {\n            cursor\n            node {\n              id\n              name\n              timeUploaded\n              storageId\n              usedBy {\n                type\n                relatedEntityId\n                displayName\n              }\n            }\n          }\n          pageInfo {\n            ...PaginationPageInfo\n          }\n          totalCount\n        }\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    query userProfileLogin {\n      userProfile {\n        i18n {\n          language\n          locale\n        }\n      }\n    }\n  "): (typeof documents)["\n    query userProfileLogin {\n      userProfile {\n        i18n {\n          language\n          locale\n        }\n      }\n    }\n  "];
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
export function graphql(source: "\n    query documentsStorageConfig {\n      documentsStorageStatistics {\n        storageId\n        documentsCount\n      }\n      systemSettings {\n        localFileSystemDocumentsStorageEnabled\n      }\n    }\n  "): (typeof documents)["\n    query documentsStorageConfig {\n      documentsStorageStatistics {\n        storageId\n        documentsCount\n      }\n      systemSettings {\n        localFileSystemDocumentsStorageEnabled\n      }\n    }\n  "];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "\n    mutation updateProfileStorage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  "): (typeof documents)["\n    mutation updateProfileStorage($documentsStorage: String, $locale: String!, $language: String!) {\n      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {\n        documentsStorage\n        i18n {\n          language\n          locale\n        }\n        userName\n      }\n    }\n  "];
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
export function graphql(source: "\n    query workspacesPage($first: Int!, $after: String) {\n      workspaces(first: $first, after: $after) {\n        edges {\n          cursor\n          node {\n            id\n            name\n            defaultCurrency\n          }\n        }\n        pageInfo {\n          ...PaginationPageInfo\n        }\n        totalCount\n      }\n    }\n  "): (typeof documents)["\n    query workspacesPage($first: Int!, $after: String) {\n      workspaces(first: $first, after: $after) {\n        edges {\n          cursor\n          node {\n            id\n            name\n            defaultCurrency\n          }\n        }\n        pageInfo {\n          ...PaginationPageInfo\n        }\n        totalCount\n      }\n    }\n  "];
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