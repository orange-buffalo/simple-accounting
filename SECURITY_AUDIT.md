# HTTP Security Audit

## Executive Summary

This source audit identified three high-priority security issues:

1. **Remediated:** Read-only workspace recipients could retrieve other recipients' workspace share tokens.
2. **Remediated:** Anonymous login requests could permanently grow an unbounded in-memory map.
3. **Remediated:** Access tokens could renew indefinitely, while logout and password changes did not revoke refresh
   credentials.

The document-storage orphan-file issue has also been remediated. Additional findings affect OAuth state handling,
password verification, information disclosure, and deployment hardening.

The findings are supported by static source analysis. They were not dynamically reproduced against a running deployment.

## Threat Model And Scope

The application is expected to run in a private network behind a reverse proxy. The audit therefore considered only
attacks reachable through HTTP or WebSocket interfaces.

In scope:

- Anonymous clients that can reach the reverse proxy.
- Authenticated ordinary users, shared-workspace recipients, and administrators interacting over HTTP.
- Authentication, authorization, GraphQL, document handling, OAuth integrations, browser rendering, and
  HTTP-triggered resource exhaustion.

Out of scope:

- Filesystem, database, container, process, or host access.
- Attacks requiring modification of deployed application files or runtime state.
- Direct access to services that are not exposed through the reverse proxy.

Private-network deployment reduces who can reach the application, but it does not prevent attacks by reachable clients.

## High-Severity Findings

### [x] SA-HTTP-001: Read-Only Workspace Recipients Can Retrieve Share Tokens

**Severity:** High

**Status:** Remediated

**Category:** Broken object/property authorization; bearer credential disclosure

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/workspaces/WorkspaceGqlDto.kt:370-392`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/workspaces/WorkspaceQuery.kt:16-26`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/workspaces/SaveSharedWorkspaceMutation.kt:21-31`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/workspaces/WorkspacesService.kt:25-42`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/workspaces/WorkspacesService.kt:78-94`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/directives/RequiredAuthDirectiveWiring.kt:55-60`

The `workspaceAccessTokens` resolver requires a regular application user, but it does not require ownership or `ADMIN`
access to the workspace. It returns actual bearer-token values in addition to token metadata.

A regular user with legitimate read-only shared access can obtain the workspace DTO through `workspace` or
`saveSharedWorkspace`, then request the token field:

```graphql
mutation {
  saveSharedWorkspace(token: "<recipient-token-A>") {
    workspaceAccessTokens(first: 100) {
      edges {
        node {
          token
          validTill
          revoked
        }
      }
    }
  }
}
```

The request is sent to `POST /api/graphql` with the recipient's ordinary account bearer token.

**Impact:**

- Disclosure of all sharing credentials for an accessible workspace.
- A recipient can capture a longer-lived token intended for another recipient.
- Access can continue after the recipient's original token expires or is revoked, provided another captured token remains
  valid.

This does not grant write access, workspace ownership, global administration, or access to unrelated workspaces. A
transient link-login identity cannot list the tokens directly; the vulnerable case is a shared recipient using a regular
application account.

**Recommendation:**

Enforce workspace `ADMIN` access inside the `workspaceAccessTokens` field resolver before querying. The field-level check
is necessary because the DTO is reachable from both queries and mutation responses.

**Resolution:**

The resolver now validates `WorkspaceAccessMode.ADMIN` before querying access tokens. A regression test verifies that a
regular user can save and read a shared workspace but cannot resolve its `workspaceAccessTokens` field:

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/workspaces/WorkspaceGqlDto.kt`
- `app/src/test/kotlin/io/orangebuffalo/simpleaccounting/business/api/workspaces/WorkspaceAccessTokensQueryTest.kt`

### [x] SA-HTTP-002: Anonymous Login Requests Permanently Grow An Unbounded Map

**Severity:** High

**Status:** Remediated

**Category:** HTTP-triggered denial of service

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/authentication/UserNamePasswordAuthenticationProvider.kt:20-39`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/CreateAccessTokenByCredentialsMutation.kt:35-78`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/authentication/AuthenticationService.kt:29-31`

Every credential-login request allocates a lock keyed by the supplied username:

```kotlin
authenticationLocks.computeIfAbsent(authentication.name) { ReentrantLock() }
```

The entries are never removed. Allocation occurs before checking whether the username exists. The anonymous login input
requires a nonblank username but does not impose a length limit.

**Attack:**

Repeatedly invoke `createAccessTokenByCredentials` with a different nonexistent username and any nonblank password. Each
failed request permanently retains the username and lock. Requests can be sequential; the attacker does not need to hold
connections open.

**Impact:**

Cumulative heap consumption can degrade or terminate the application. Per-account locking does not help because each
request can use a new username. Request-size limits do not prevent accumulation from many small requests.

The persistent allocation is established by source analysis. The number of requests needed to exhaust a deployed JVM was
not measured.

**Recommendation:**

- Replace the per-username map with bounded lock striping or another concurrency-safe bounded mechanism.
- Apply a username length constraint.
- Apply global and client-level authentication-request throttling at the application or reverse proxy.

**Resolution:**

The per-username map was replaced with 256 fixed lock stripes, preserving serialized authentication attempts while
eliminating attacker-controlled lock allocation. Login usernames are now limited to 255 characters, matching the database
model. Regression coverage verifies the GraphQL input length boundary:

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/authentication/UserNamePasswordAuthenticationProvider.kt`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/CreateAccessTokenByCredentialsMutation.kt`
- `app/src/test/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/CreateAccessTokenByCredentialsMutationTest.kt`

Global and client-level request throttling remains a deployment defense-in-depth recommendation.

### [x] SA-HTTP-003: Access Tokens Can Renew Indefinitely And Sessions Are Not Revoked

**Severity:** High

**Status:** Remediated

**Category:** Session management and stale authorization

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/RefreshAccessTokenMutation.kt:33-65`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/jwt/JwtService.kt:30-66`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/InvalidateRefreshTokenMutation.kt:15-27`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/remeberme/RefreshTokensService.kt:14-55`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/authentication/AuthenticationService.kt:106-125`

`refreshAccessToken` accepts the current bearer-authenticated principal without requiring a refresh credential. For a
regular user, it copies that principal and its roles into a new ten-minute JWT without loading current account state.

A holder of an unexpired access token can repeatedly submit:

```graphql
mutation {
  refreshAccessToken {
    accessToken
  }
}
```

Using each returned token before expiration makes the original credential indefinitely renewable. Removed administrator
privileges continue to be copied from stale JWT claims.

Logout only expires the browser cookie and does not delete the server-side refresh token. A retained refresh token remains
usable for its original 30-day lifetime, including after the account password changes.

**Impact:**

- A nominally ten-minute access token can become an indefinite session.
- Removed privileges may remain usable.
- Password changes do not terminate established access-token or refresh-token sessions.
- Logout does not revoke the refresh credential.

This requires possession of a valid access or refresh credential. It is not an anonymous authentication bypass. Restarting
the application rotates the process-local JWT signing key and breaks an existing access-token renewal chain, but this is
not a session-management control.

**WebSocket boundary:**

WebSocket authentication is validated at connection initialization and then retained without rechecking JWT expiration.
Long-lived connections can therefore remain authenticated past token expiration, depending on reverse-proxy connection
limits and connection survival.

Relevant evidence:

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/graphql/SaGraphQlServerConfig.kt:85-86`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/graphql/SaGraphQlServerConfig.kt:122-129`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/pushnotifications/PushNotificationsSubscription.kt:30-40`

**Recommendation:**

- Require a valid, revocable refresh credential for access-token renewal.
- Reload current account status and privileges during renewal.
- Revoke the presented refresh token during logout.
- Invalidate appropriate sessions after password and security changes.
- Introduce a session or security version that can invalidate existing JWTs.
- Terminate or reauthenticate WebSocket connections when their authentication expires or is revoked.

**Resolution:**

Regular and administrator access tokens can no longer authorize `refreshAccessToken`; renewal now requires a persisted
refresh-token cookie and reloads the user's current account state and privileges. Transient workspace-link sessions retain
renewal support because each renewal revalidates their revocable workspace access token. Logout deletes the presented
refresh token, and password changes atomically revoke all refresh tokens belonging to that user. Regression coverage
verifies bearer-only renewal rejection and refresh-token revocation on logout and password changes:

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/RefreshAccessTokenMutation.kt`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/InvalidateRefreshTokenMutation.kt`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/remeberme/RefreshTokensService.kt`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/authentication/AuthenticationService.kt`
- `app/src/test/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/RefreshAccessTokenMutationTest.kt`
- `app/src/test/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/InvalidateRefreshTokenMutationTest.kt`
- `app/src/test/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/ChangePasswordMutationTest.kt`

Already-issued regular access tokens remain valid until their ten-minute expiry. Immediate invalidation would require a
session or security-version check during JWT authentication. The WebSocket lifetime boundary described above also remains
open and should be addressed separately.

## Medium-Severity Findings

### [x] SA-HTTP-004: Malformed Upload Metadata Creates Persistent Orphan Files

**Severity:** Medium

**Status:** Remediated

**Category:** Authenticated persistent-storage exhaustion

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/documents/CreateDocumentUploadUrlMutation.kt:27-35`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/documents/DocumentsContentApi.kt:44-55`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/documents/DocumentsService.kt:36-51`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/documents/DocumentsService.kt:126-151`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/documents/storage/local/LocalFileSystemDocumentsStorage.kt:36-50`
- `app/src/main/resources/db/migration/V0001__Baseline.sql:86-95`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/integration/TokensRepository.kt:7-32`

The upload path writes the storage object before inserting document metadata. If the database insert fails, no compensating
storage deletion occurs.

A regular user with an owned workspace can obtain an upload token and submit a multipart file whose original filename has
more than 255 characters. Local storage replaces the basename with a short UUID and preserves only the extension, so the
filesystem write succeeds. The subsequent database insert fails because the original filename exceeds the 255-character
database column.

The same upload token can be reused during its two-minute lifetime because lookup does not consume it.

**Impact:**

Repeated requests create persistent files with no corresponding document row. These files are absent from document
statistics and cannot be deleted through normal document operations. The configured 50 MB multipart limit bounds each
request but not cumulative disk consumption.

**Related configuration-enforcement issue:**

Users can set `documentsStorage` to `local-fs` through the profile mutation. Storage selection and local storage operations
do not enforce the local-storage `enabled` flag:

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/profile/UpdateUserProfileMutation.kt:19-41`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/documents/DocumentsService.kt:53-56`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/documents/storage/local/LocalFileSystemDocumentsStorage.kt:19-43`

Exploitation still requires a writable configured directory.

**Recommendation:**

- Validate filename and content metadata before storage writes.
- Delete the stored object if metadata persistence fails.
- Consume or otherwise constrain upload-token reuse.
- Enforce storage availability and enablement server-side.
- Add aggregate per-user/workspace storage quotas and orphan reconciliation.

**Resolution:**

Filename and content-type lengths are now validated against their database column limits before content is sent to the
configured storage. Invalid multipart metadata receives HTTP 400. If document metadata persistence nevertheless fails
after a storage write, the service deletes the stored object and rethrows the original persistence exception. Regression
coverage verifies both oversized-filename rejection without a storage write and compensating deletion after a repository
failure:

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/documents/DocumentsService.kt`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/documents/DocumentsContentApi.kt`
- `app/src/test/kotlin/io/orangebuffalo/simpleaccounting/business/api/documents/DocumentsUploadApiTest.kt`
- `app/src/test/kotlin/io/orangebuffalo/simpleaccounting/business/documents/DocumentsServiceTest.kt`

Upload tokens remain reusable during their two-minute lifetime, and aggregate quotas and orphan reconciliation remain
defense-in-depth opportunities. The related local-storage enablement issue is not changed by this remediation because
blocking new writes while retaining access to existing documents requires a separate storage-lifecycle policy.

### SA-HTTP-005: Google Drive Status Queries Retain OAuth State For Two Days

**Severity:** Medium

**Category:** Authenticated heap and scheduler exhaustion

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/documentstorage/GoogleDriveStorageIntegrationStatusQuery.kt:15-25`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/documents/storage/gdrive/GoogleDriveDocumentsStorage.kt:169-189`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/oauth2/OAuth2ClientAuthorizationProvider.kt:51-68`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/oauth2/impl/InMemorySavedAuthorizationRequestRepository.kt:11-37`

When Drive authorization is required, every integration-status query builds a new authorization URL. Each URL generation
creates and retains a fresh authorization request in a `ConcurrentHashMap` for two days and schedules a separate expiration
callback.

An ordinary user without completed Drive authorization can repeatedly submit:

```graphql
query {
  googleDriveStorageIntegrationStatus {
    authorizationRequired
  }
}
```

Selecting only the boolean still causes construction of the complete status object and creation of the pending OAuth flow.
The user does not need to visit the URL, possess a Google account, or complete OAuth.

**Impact:**

Attacker-controlled accumulation of heap objects and scheduled tasks through an apparently read-only query. The retained
allocation is established by source analysis; the request count required to exhaust the deployed JVM was not measured.

**Recommendation:**

- Do not allocate authorization flows as a side effect of status retrieval.
- Reuse or replace an existing pending flow for the user.
- Reduce pending-flow lifetime where practical.
- Cap pending entries and scheduled work.

### SA-HTTP-006: Password Changes Bypass Login Guessing Controls

**Severity:** Medium, conditional

**Category:** Missing throttling on credential verification

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/ChangePasswordMutation.kt:21-43`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/authentication/AuthenticationService.kt:106-121`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/authentication/AuthenticationService.kt:29-36`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/authentication/AuthenticationService.kt:83-103`

The password-change operation verifies the supplied current password directly. Failed checks do not consult or update the
progressive lockout used by ordinary password login.

An attacker holding a victim's access token can repeatedly submit candidate current passwords through `changePassword`.
A successful guess changes the password to the attacker's selected value.

**Impact:**

A temporary session compromise can be converted into password control without the normal login-guessing restrictions.
The indefinite access-token renewal issue can extend the time available for guessing.

This finding requires a valid victim access token and a password-authenticated account. BCrypt cost and proxy controls
still constrain throughput. It is not anonymous password brute force.

**Recommendation:**

Apply shared credential-verification throttling to password changes and other step-up authentication operations.

### SA-HTTP-007: Administrator-Controlled OIDC Discovery Allows Server-Origin Requests

**Severity:** Medium if application administrators are not trusted with unrestricted server-network access

**Category:** Privileged SSRF capability

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/oauthproviders/DiscoverOidcProviderConfigurationQuery.kt:24-34`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/directives/EndpointUrlConstraint.kt:53-64`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/oauthproviders/OidcProviderDiscoveryService.kt:21-32`

OIDC discovery accepts an administrator-supplied absolute HTTP or HTTPS URL. Validation does not reject loopback,
private, link-local, or otherwise sensitive network addresses. It also permits URL fragments and query components.

The service appends `/.well-known/openid-configuration` and performs a server-side GET. A fragment can cause the appended
text to remain in the URI fragment, allowing the administrator to select another HTTP path. For example:

```text
http://127.0.0.1:9393/api/graphql/schema#
```

The response is materialized as a string without an application-defined response-size bound or explicit timeout.

**Impact:**

An application administrator can trigger server-origin HTTP(S) GET requests to destinations reachable by the application.
The implementation does not return arbitrary response bodies, so general internal-response exfiltration was not
established. An administrator-selected hostile endpoint can also return a large body and increase heap use.

This is not an anonymous or ordinary-user SSRF. If administrators are intentionally trusted with server-network access,
it is a privileged capability rather than a trust-boundary violation.

**Recommendation:**

- Define the intended administrator and network trust boundary.
- Reject fragments and unexpected query components in discovery base URLs.
- If required by that boundary, allowlist destinations and reject loopback, private, and link-local addresses after DNS
  resolution.
- Apply outbound network policy, response-size limits, and deadlines.

## Low-Severity Findings

### SA-HTTP-008: Edit Mutations Disclose Cross-Workspace Object Existence And Version Equality

**Severity:** Low

**Category:** Authorization-check ordering; information disclosure

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/customers/EditCustomerMutation.kt:34-38`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/categories/EditCategoryMutation.kt:41-45`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/generaltaxes/EditGeneralTaxMutation.kt:43-47`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/expenses/EditExpenseMutation.kt:65-69`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/incomes/EditIncomeMutation.kt:61-65`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/invoices/EditInvoiceMutation.kt:58-62`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/incometaxpayments/EditIncomeTaxPaymentMutation.kt:49-53`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/common/pesistence/AbstractEntity.kt:18-23`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/graphql/SaDataFetcherExceptionHandler.kt:52-61`

These edit mutations load the object and compare its submitted version before checking whether the caller has workspace
write access. Missing objects and version mismatches produce distinguishable GraphQL error types.

A regular user with candidate workspace and object IDs can submit a deliberately invalid version. An outdated-state error
confirms that the object exists in the supplied workspace; not-found indicates a different condition. Previously known
versions can also reveal whether an object changed after access was lost.

**Impact:**

The attacker can infer object existence, workspace association, and limited version-change information. The response does
not disclose financial fields or the current version, and the later authorization check still prevents unauthorized
writes.

**Recommendation:**

Authorize access to the workspace before retrieving or version-checking the object. `EditWorkspaceMutation` already uses
this safer ordering.

### SA-HTTP-009: Anonymous Authentication Responses Expose Account State

**Severity:** Low

**Category:** Username and account-state enumeration

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/UserAuthenticationMethodsQuery.kt:25-44`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/oauthproviders/OAuthUserAuthenticationService.kt:72-92`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/security/authentication/AuthenticationService.kt:29-59`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/CreateAccessTokenByCredentialsMutation.kt:36-56`

The anonymous authentication-method query can reveal linked OAuth providers for an activated account. Credential-login
errors distinguish some inactive, OAuth-linked, and locked states before password verification.

**Impact:**

Candidate usernames can be profiled for targeted phishing or account disruption. The method query alone does not
distinguish unknown users from ordinary password users. Some disclosure may be an intentional consequence of the
username-first login experience.

**Recommendation:**

Return uniform authentication errors where the user experience permits it. Rate-limit enumeration-capable endpoints and
avoid disclosing provider metadata until necessary.

### SA-HTTP-010: Anonymous OAuth Login Initiation Amplifies Temporary Database State

**Severity:** Low to Medium

**Category:** Anonymous temporary-state allocation

**Evidence:**

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/StartOAuthLoginMutation.kt:28-60`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/oauthproviders/OAuthUserAuthenticationService.kt:108-126`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/oauthproviders/OAuthUserAuthenticationService.kt:239-265`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/oauthproviders/OAuthAuthenticationRequestsRepository.kt:20-31`

An unauthenticated caller who knows an activated username and linked provider ID can repeatedly start OAuth login while
omitting the previous browser-binding cookie. Each accepted request performs cleanup work and inserts a new pending record.
The per-browser replacement optimization does not apply without the cookie.

**Impact:**

Repeated database work and growth in pending-flow records. Entries have a ten-minute lifetime, so this is bounded temporary
state rather than permanent growth.

**Recommendation:**

Apply initiation throttling and global/per-account pending-state limits while preserving protection against one browser
cancelling another browser's login.

## Browser And Privacy Findings

### SA-HTTP-011: Runtime Google Fonts Request On The Initial Loading Path

**Severity:** Low

**Category:** Privacy and supply-chain hardening

**Evidence:**

- `frontend/src/styles/loader.scss:4`
- `frontend/src/setup/loader.ts:1`
- `frontend/src/main.ts:1-6`
- `frontend/src/styles/fonts.scss:6-14`

The initial loader imports a stylesheet from Google Fonts even though local font assets are available. If browser egress
and CSP allow the request, this discloses request/network metadata to a third party and introduces an unnecessary runtime
availability and stylesheet-delivery dependency.

No evidence was found that access tokens or full sensitive URLs are necessarily disclosed through this request.

**Recommendation:**

Remove the external import and use the bundled font assets.

### SA-HTTP-012: Sensitive URLs Are Not Explicitly Removed From Browser History

**Severity:** Low, conditional

**Category:** Credential and authorization-code exposure hardening

**Evidence:**

- `frontend/src/setup/setup-router.ts:69-76`
- `frontend/src/pages/LoginByLink.vue:70-92`
- `frontend/src/services/use-navigation.ts:7-9`
- `frontend/src/pages/account-activation/AccountActivationPage.vue:106-137`
- `frontend/src/pages/oauth-callback/OAuthCallbackPage.vue:53-71`
- `frontend/src/pages/oauth-identity-callback/OAuthIdentityCallbackPage.vue:125-152`

Workspace sharing tokens, activation tokens, and OAuth callback parameters enter path or query components. Successful
navigation uses `router.push`, and some callback pages remain displayed. The sensitive URL is therefore not explicitly
replaced in browser history or address state.

**Impact:**

Exposure requires another channel such as browser history access or synchronization, copied URLs, screenshots, or proxy
logs. Those conditions are not independently established by this audit. OAuth codes and activation tokens may be unusable
after consumption, while workspace-sharing links remain bearer credentials until expiration or revocation.

**Recommendation:**

- Capture required parameters and replace the URL rather than adding another history entry.
- Apply an explicit restrictive referrer policy to sensitive landing pages.
- Redact sensitive paths, query strings, and referrers in reverse-proxy logging.

Client-side cleanup cannot prevent the initial request target from reaching the reverse proxy.

### SA-HTTP-013: Markdown Sanitization Does Not Restrict External Images And Links

**Severity:** Low, conditional

**Category:** Privacy and content-policy hardening

**Evidence:**

- `frontend/src/components/SaMarkdownOutput.vue:12-24`
- `frontend/src/components/notes-input/SaNotesInput.vue:12-16`
- `frontend/src/pages/expenses/ExpensesOverviewPanel.vue:252`
- `frontend/src/pages/incomes/IncomesOverviewPanel.vue:249`
- `frontend/src/pages/invoices/InvoicesOverviewPanel.vue:205`
- `frontend/src/pages/income-tax-payments/IncomeTaxPaymentsOverviewPanel.vue:82`

Markdown is parsed, sanitized through DOMPurify, and rendered through `v-html`. This ordering provides XSS protection, and
no sanitizer bypass was established. However, the component does not implement an application-specific policy for external
images and links.

An attacker who can cause authored content to be rendered, such as through a legitimately writable shared workspace or
social engineering, can include an external image that causes the victim's browser to make a request. External links can
also lead users away from the application.

**Impact:**

External images may disclose request metadata and attacker-chosen tracking identifiers. This does not inherently expose
application tokens, DOM content, or accounting records.

**Recommendation:**

If only basic formatting is needed, explicitly restrict supported tags and attributes. Disable external images or allow
only approved origins, constrain link protocols, and apply an intentional referrer policy.

### SA-HTTP-014: Misspelled `noreferrer` Attribute

**Severity:** Informational

**Evidence:**

- `frontend/src/components/documents/storage/SaGoogleDriveIntegrationSetup.vue:29-34`

The Google Drive folder link uses:

```html
rel="noopener noreferral"
```

`noreferral` does not enable `noreferrer`. `noopener` is present, so this is not a demonstrated reverse-tabnabbing issue.

**Recommendation:**

Use `rel="noopener noreferrer"`.

## Deployment And Defense-In-Depth Gaps

The following items are source-level hardening gaps. Their effective deployment state depends on the reverse proxy and was
not available for this audit.

### Cookies Do Not Explicitly Use `Secure`

Refresh-token and OAuth-binding cookies use `HttpOnly` and `SameSite=Strict` but omit `Secure`:

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/RefreshTokenCookieSupport.kt:14-21`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/business/api/auth/OAuthFlowBindingCookieSupport.kt:25-33`

TLS termination alone does not add the browser cookie attribute. Explicitly set `Secure` for HTTPS deployments. Actual
cleartext exposure was not established because the deployed proxy configuration was unavailable.

### No Application-Specific GraphQL Execution Budget Was Found

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/graphql/SaGraphQlServerConfig.kt:47-55`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/graphql/connections/ConnectionTypes.kt:12-15`

Pagination inputs are bounded to 1-500, but no explicit query-cost, alias, execution-time, or WebSocket operation/concurrency
budget was identified. Pagination does not bound the total work requested by a document containing many fields or aliases.

This is a hardening gap, not proof that the underlying framework has no parser or transport limits. Configure operation
cost/depth/alias limits, execution deadlines, and WebSocket connection and operation limits.

### Unexpected GraphQL Errors May Expose Exception Messages

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/graphql/SaDataFetcherExceptionHandler.kt:75-79`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/graphql/SaGraphQlServerConfig.kt:147-154`

Unexpected data-fetcher exceptions preserve the original exception message. No specific reachable secret disclosure was
established. Return generic public errors with correlation IDs and keep details in server logs.

### GraphQL Schema Is Publicly Available

- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/graphql/SaGraphQlServerConfig.kt:157-162`

The schema endpoint aids API reconnaissance but is not an authorization bypass. Restrict it if schema disclosure is not
required in production.

### Browser Policy Is Not Defined By The Application

- `frontend/index.html:3-17`
- `app/src/main/kotlin/io/orangebuffalo/simpleaccounting/infra/WebConfig.kt:35-53`
- `docs/Deployment.md:20-39`

No application-defined Content-Security-Policy or Referrer-Policy was identified. The effective reverse-proxy policy is
unknown. Verify deployed responses for:

- An appropriate Content-Security-Policy.
- A restrictive Referrer-Policy, especially on credential-bearing routes.
- HTTPS and HSTS.
- Frame protection.
- Suitable request-body and header limits.
- WebSocket connection and operation limits.

The production frontend also enables inline source maps in `frontend/vite.config.ts:40-43`. This is optional
information-minimization hardening rather than an independent vulnerability.

## Reviewed Areas Without A Confirmed Exploit

The following areas were reviewed without establishing an exploitable vulnerability under the stated threat model:

- JWT signatures and expiration are cryptographically verified; no unsigned-token or attacker-selected-key bypass was
  found.
- CSRF is disabled, but ordinary authenticated GraphQL operations require an explicit bearer token and refresh/OAuth
  cookies use `SameSite=Strict`. No cross-origin account-takeover chain was established from CSRF configuration alone.
- No permissive application CORS configuration was found. Reverse-proxy-added CORS headers remain unverified.
- Actuator health is permitted while other actuator paths are explicitly denied.
- Activation tokens are generated with high entropy, expire, and are deleted after successful activation.
- OAuth identity login verifies state, browser binding, expiration, one-time consumption, and returned external identity.
- OAuth callback URLs derive from configured public URLs rather than untrusted incoming `Host` headers.
- Central workspace authorization prevents arbitrary workspace-ID substitution and prevents read-only shared users from
  writing business data.
- Share-token creation and revocation require workspace administrative access.
- Related category, customer, invoice, document, and tax references are validated against the destination workspace.
- Reviewed JOOQ query construction uses typed predicates; no SQL injection or cursor-based workspace-filter bypass was
  established.
- DataLoader result mapping preserves stored workspace IDs and did not establish a cross-workspace IDOR.
- Explicit mutation argument mapping did not expose a mass-assignment privilege escalation.
- Document paths are generated from UUIDs, and no HTTP input was found that controls persisted storage locations. No
  arbitrary filesystem traversal was established.
- Document download URL issuance checks workspace access and document membership. Responses use attachment disposition.
- No server-side document rendering or archive-extraction pipeline was found.
- No ordinary-user arbitrary-host SSRF was established for Google Drive or Dropbox integrations.
- No URL-controlled frontend open redirect was established.
- No stored, reflected, or DOM XSS was established. Markdown follows the sequence `marked.parse`, DOMPurify sanitation,
  then `v-html` rendering.
- Access tokens are stored in frontend memory rather than local storage.
- GraphQL bearer authentication and WebSocket authentication are not placed in request URLs.
- OAuth callback `postMessage` handling validates the exact origin and popup window, then reloads authoritative server state.

These results are not guarantees of absence. They describe what the source review did and did not establish.

## Audit Limitations

The audit did not include:

- Live exploit reproduction.
- Load or resource-exhaustion measurement.
- Dependency advisory or software-composition scanning.
- Review of the deployed reverse-proxy configuration.
- Request-smuggling and proxy/backend parser-differential testing.
- Validation of effective CSP, CORS, HSTS, Referrer-Policy, rate limits, or request-size limits.
- External penetration testing against a running environment.

The current backend configuration uses Spring MVC/Servlet processing and virtual threads, despite older repository
documentation describing WebFlux.

## Recommended Remediation Order

1. [x] Require workspace `ADMIN` access when listing workspace access tokens.
2. [x] Replace the unbounded username-to-lock map with a bounded mechanism.
3. [x] Require revocable refresh credentials for renewal and revoke refresh credentials on logout and password change.
4. [x] Validate upload metadata before storage writes and compensate for failed persistence.
5. Remove status-query side effects and bound OAuth pending state.
6. Apply consistent throttling to every password-verification path.
7. Resolve lower-severity information-disclosure and browser-hardening findings.
8. Validate and document the reverse proxy's security policy and resource limits.
