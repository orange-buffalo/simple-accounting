import fetchMock from 'fetch-mock';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import 'whatwg-fetch';
import type { Auth } from '@/services/api';
import { ApiAuthError, ApiBusinessError, ApiFieldLevelValidationError } from '@/services/api/api-errors.ts';
import { SaGrapQlErrorType, ValidationErrorCode } from '@/services/api/gql/graphql.ts';
import { GrapQlClient } from '@/services/api/gql-api-client.ts';

// eslint-disable-next-line vue/max-len
const TOKEN =
  'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY0NTV9.Zd2q76NaV27zZxMYxSJbDjzCjf4eAD4_aa16iQ4C-ABXZDzNAQWHCoajHGY3-7aOQnSSPo1uZxskY9B8dcHlfkr_lsEQHJ6I4yBYueYDC_V6MZmi3tVwBAeftrIhXs900ioxo0D2cLl7MAcMNGlQjrTDz62SrIrz30JnBOGnHbcK088rkbw5nLbdyUT0PA0w6EgDntJjtJS0OS7EHLpixFtenQR7LPKj-c7KdZybjShFAuw9L8cW5onKZb3S7AOzxwPcSGM2uKo2nc0EQ3Zo48gTtfieSBDCgpi0rymmDPpiq1yNB0U21A8n59DA9YDFf2Kaaf5ZjFAxvZ_Ul9a3Wg';
const TOKEN_EXP_EPOCH_SECONDS = 1578116455;
// eslint-disable-next-line vue/max-len
const NEW_TOKEN =
  'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY5NTV9.new-token-signature';
const API_TIME = new Date(0);
API_TIME.setUTCSeconds(TOKEN_EXP_EPOCH_SECONDS - 3 * 60); // 3 minutes before token expiration

type MockedRequestAssertions = (options: RequestInit) => void;
type MockedRequest = {
  requestAssertions: MockedRequestAssertions;
  responseBody: any;
  responseStatus?: number;
};

fetchMock.mockGlobal();

describe('GraphQL API Client', () => {
  let useAuth: () => Auth;
  let gqlClient: GrapQlClient;
  let mockedRequests: Array<MockedRequest> = [];
  const apiCallPath = '/api/graphql';

  test('does refresh access token if not yet set', async () => {
    mockRequest(refreshTokenAssertions(), refreshTokenResponse(TOKEN));
    mockRequest(apiQueryAssertions(TOKEN), successApiQueryResponse());

    const response = await executeApiCall();

    assertSuccessResponse(response);
    assertAuthHasToken(TOKEN);
  });

  test('uses existing access token if valid and does not refresh', async () => {
    await setApiToken(TOKEN);

    mockRequest(apiQueryAssertions(TOKEN), successApiQueryResponse());

    const response = await executeApiCall();

    assertSuccessResponse(response);
    assertAuthHasToken(TOKEN);
  });

  test('refreshes access token if close to expiration', async () => {
    // eslint-disable-next-line vue/max-len
    const EXPIRING_TOKEN =
      'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTYyOTV9.mock-signature-for-expiring-token';
    await setApiToken(EXPIRING_TOKEN);

    mockRequest(refreshTokenAssertions(), refreshTokenResponse(TOKEN));
    // should use the new token for the request
    mockRequest(apiQueryAssertions(TOKEN), successApiQueryResponse());

    const response = await executeApiCall();

    assertSuccessResponse(response);
    assertAuthHasToken(TOKEN);
  });

  test('refreshes access token if was valid but unauthorized error received', async () => {
    await setApiToken(TOKEN);

    // First request with original token returns unauthorized error
    mockRequest(apiQueryAssertions(TOKEN), unauthorizedApiQueryResponse());
    // Token refresh request returns new token
    mockRequest(refreshTokenAssertions(), refreshTokenResponse(NEW_TOKEN));
    // Retry the original request with new token
    mockRequest(apiQueryAssertions(NEW_TOKEN), successApiQueryResponse());

    const response = await executeApiCall();

    assertSuccessResponse(response);
    assertAuthHasToken(NEW_TOKEN);
  });

  test('mutation: smoke test for success path', async () => {
    mockRequest(refreshTokenAssertions(), refreshTokenResponse(TOKEN));
    mockRequest(apiMutationAssertions(TOKEN), successApiMutationResponse());

    const response = await executeApiMutation();

    assertSuccessMutationResponse(response);
    assertAuthHasToken(TOKEN);
  });

  test('mutation: smoke test for exception case', async () => {
    mockRequest(refreshTokenAssertions(), refreshTokenResponse(null));
    mockRequest(apiMutationAssertions(null), unauthorizedApiQueryResponse());

    await expectToFailWith<ApiAuthError>(async () => {
      await executeApiMutation();
    }, 'ApiAuthError');
  });

  test('executes the initial request even if refresh token returns no token', async () => {
    await setApiToken(null);

    // Refresh token request returns null/undefined token
    mockRequest(refreshTokenAssertions(), refreshTokenResponse(null));
    // Original request should be executed without auth header and return unauthorized
    mockRequest(apiQueryAssertions(null), unauthorizedApiQueryResponse());

    await expectToFailWith<ApiAuthError>(async () => {
      await executeApiCall();
    }, 'ApiAuthError');
  });

  test('throws ApiAuthError on 401 responses', async () => {
    await setApiToken(null);

    mockRequest(refreshTokenAssertions(), refreshTokenResponse(null));
    mockRequest(apiQueryAssertions(null), {}, 401);

    await expectToFailWith<ApiAuthError>(async () => {
      await executeApiCall();
    }, 'ApiAuthError');
  });

  test('throws ApiFieldLevelValidationError on validation failure', async () => {
    await setApiToken(TOKEN);

    mockRequest(
      apiMutationAssertions(TOKEN),
      validationFailureResponse([
        {
          path: 'currentPassword',
          error: ValidationErrorCode.MustNotBeBlank,
          message: 'must not be blank',
        },
      ]),
    );

    const error = await expectToFailWith<ApiFieldLevelValidationError>(async () => {
      await executeApiMutation();
    }, 'ApiFieldLevelValidationError');

    expect(error.fieldErrors).toHaveLength(1);
    expect(error.fieldErrors[0]).toEqual({
      field: 'currentPassword',
      error: ValidationErrorCode.MustNotBeBlank,
      message: 'must not be blank',
      params: undefined,
    });
  });

  test('throws ApiFieldLevelValidationError with params on validation failure', async () => {
    await setApiToken(TOKEN);

    mockRequest(
      apiMutationAssertions(TOKEN),
      validationFailureResponse([
        {
          path: 'password',
          error: ValidationErrorCode.SizeConstraintViolated,
          message: 'size must be between 6 and 100',
          params: [
            { name: 'min', value: '6' },
            { name: 'max', value: '100' },
          ],
        },
      ]),
    );

    const error = await expectToFailWith<ApiFieldLevelValidationError>(async () => {
      await executeApiMutation();
    }, 'ApiFieldLevelValidationError');

    expect(error.fieldErrors).toHaveLength(1);
    expect(error.fieldErrors[0]).toEqual({
      field: 'password',
      error: ValidationErrorCode.SizeConstraintViolated,
      message: 'size must be between 6 and 100',
      params: { min: '6', max: '100' },
    });
  });

  test('throws ApiFieldLevelValidationError with multiple validation errors', async () => {
    await setApiToken(TOKEN);

    mockRequest(
      apiMutationAssertions(TOKEN),
      validationFailureResponse([
        {
          path: 'currentPassword',
          error: ValidationErrorCode.MustNotBeBlank,
          message: 'must not be blank',
        },
        {
          path: 'newPassword',
          error: ValidationErrorCode.SizeConstraintViolated,
          message: 'size must be between 6 and 100',
          params: [
            { name: 'min', value: '6' },
            { name: 'max', value: '100' },
          ],
        },
      ]),
    );

    const error = await expectToFailWith<ApiFieldLevelValidationError>(async () => {
      await executeApiMutation();
    }, 'ApiFieldLevelValidationError');

    expect(error.fieldErrors).toHaveLength(2);
    expect(error.fieldErrors[0]).toEqual({
      field: 'currentPassword',
      error: ValidationErrorCode.MustNotBeBlank,
      message: 'must not be blank',
      params: undefined,
    });
    expect(error.fieldErrors[1]).toEqual({
      field: 'newPassword',
      error: ValidationErrorCode.SizeConstraintViolated,
      message: 'size must be between 6 and 100',
      params: { min: '6', max: '100' },
    });
  });

  test('throws ApiBusinessError on business error response', async () => {
    await setApiToken(TOKEN);

    mockRequest(
      apiMutationAssertions(TOKEN),
      businessErrorResponse(
        'CURRENT_PASSWORD_MISMATCH',
        "The provided current password does not match the user's actual password.",
      ),
    );

    const error = await expectToFailWith<ApiBusinessError>(async () => {
      await executeApiMutation();
    }, 'ApiBusinessError');

    expect(error.error).toEqual({
      error: 'CURRENT_PASSWORD_MISMATCH',
      message: "The provided current password does not match the user's actual password.",
    });
    expect(error.errorAs<{ error: string; message: string }>()).toEqual({
      error: 'CURRENT_PASSWORD_MISMATCH',
      message: "The provided current password does not match the user's actual password.",
    });
  });

  test('throws ApiBusinessError with minimal message on business error response', async () => {
    await setApiToken(TOKEN);

    mockRequest(apiMutationAssertions(TOKEN), businessErrorResponse('SOME_ERROR_CODE', 'Error message'));

    const error = await expectToFailWith<ApiBusinessError>(async () => {
      await executeApiMutation();
    }, 'ApiBusinessError');

    expect(error.error.error).toBe('SOME_ERROR_CODE');
    expect(error.error.message).toBe('Error message');
    expect(error.message).toBe('Business error: SOME_ERROR_CODE');
  });

  // TODO enable test when https://github.com/urql-graphql/urql/issues/3801 is fixed
  // test('throws ApiTimeoutError on timeout', async () => {
  //   vi.useRealTimers();
  //
  //   fetchMock.post(apiCallPath, 200, {
  //     delay: 6000,
  //   });
  //
  //   const error = await expectToFailWith<ApiTimeoutError>(async () => {
  //     await apiCall();
  //   }, 'ApiTimeoutError');
  //   expect(error.message)
  //     .toBe('Request timed out');
  //
  //   assertRegularRequestEvents();
  // });

  // test('throws with ApiRequestCancelledError when custom cancellation is requested', async () => {
  //   vi.useRealTimers();
  //
  //   fetchMock.get(apiCallPath, 200, {
  //     delay: 20000,
  //   });
  //
  //   const {
  //     requestConfig,
  //     cancelRequest,
  //   } = useRequestConfig({});
  //
  //   setTimeout(() => cancelRequest(), 500);
  //
  //   const error = await expectToFailWith<ApiRequestCancelledError>(async () => {
  //     await apiCall(requestConfig);
  //   }, 'ApiRequestCancelledError');
  //   expect(error.message)
  //     .toBe('Request was cancelled before it was completed');
  //
  //   assertRegularRequestEvents();
  // });

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.setSystemTime(API_TIME);

    vi.mock('@/services/events', () => ({
      LOGIN_REQUIRED_EVENT: {
        emit: vi.fn(),
      },
      LOADING_STARTED_EVENT: {
        emit: vi.fn(),
      },
      LOADING_FINISHED_EVENT: {
        emit: vi.fn(),
      },
    }));

    ({ useAuth } = await import('@/services/api'));
    ({ gqlClient } = await import('@/services/api/gql-api-client'));

    mockedRequests = [];
    let currentRequestIndex = 0;
    fetchMock.post(apiCallPath, ({ options }) => {
      if (currentRequestIndex >= mockedRequests.length) {
        console.error(
          `[mockedRequests] Received more requests (${currentRequestIndex + 1}) than expected. Current request body`,
          options.body,
        );
        throw new Error();
      }
      const mockedRequest = mockedRequests[currentRequestIndex];
      currentRequestIndex++;
      try {
        mockedRequest.requestAssertions(options);
      } catch (e) {
        console.error(`[mockedRequests] Request assertions failed on request #${currentRequestIndex}`, e);
        throw e;
      }
      return {
        status: mockedRequest.responseStatus ?? 200,
        body: mockedRequest.responseBody,
      };
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.resetAllMocks();
    vi.resetModules();
    fetchMock.removeRoutes();
    fetchMock.clearHistory();
  });

  function assertAuthHasToken(token: string | null) {
    const auth = useAuth();
    expect(auth.getToken()).toBe(token);
  }

  async function executeApiCall() {
    return await gqlClient.query(
      `
      query {
        fakeGetter {
          someProperty
        }
      }
    `,
      {},
    );
  }

  async function executeApiMutation() {
    return await gqlClient.mutation(
      `
      mutation {
        fakeMutation {
          success
        }
      }
    `,
      {},
    );
  }

  function mockRequest(requestAssertions: MockedRequestAssertions, responseBody: any, responseStatus?: number) {
    mockedRequests.push({
      requestAssertions,
      responseBody,
      responseStatus,
    });
  }

  async function setApiToken(token: string | null) {
    const { updateApiToken } = await import('@/services/api/auth');
    updateApiToken(token);
  }
});

function refreshTokenAssertions(): MockedRequestAssertions {
  return (options: RequestInit) => {
    expect(options.body).toBeTypeOf('string');
    const body = JSON.parse(options.body as string);
    expect(body).toEqual({
      operationName: 'refreshAccessToken',
      query: 'mutation refreshAccessToken {\n  refreshAccessToken {\n    accessToken\n  }\n}',
      variables: {},
    });
  };
}

function refreshTokenResponse(token: string | null): any {
  return {
    data: {
      refreshAccessToken: {
        accessToken: token,
      },
    },
  };
}

function unauthorizedApiQueryResponse(): any {
  return {
    errors: [
      {
        message: 'Unauthorized',
        extensions: {
          errorType: SaGrapQlErrorType.NotAuthorized,
        },
      },
    ],
  };
}

function apiQueryAssertions(expectedToken: string | null): MockedRequestAssertions {
  return apiRequestAssertions(expectedToken, {
    query: '{\n  fakeGetter {\n    someProperty\n  }\n}',
    variables: {},
  });
}

function successApiQueryResponse(): any {
  return {
    data: {
      fakeGetter: {
        someProperty: 'someValue',
      },
    },
  };
}

function assertSuccessResponse(response: any) {
  expect(response).toStrictEqual({
    fakeGetter: {
      someProperty: 'someValue',
    },
  });
}

function apiMutationAssertions(expectedToken: string | null): MockedRequestAssertions {
  return apiRequestAssertions(expectedToken, {
    query: 'mutation {\n  fakeMutation {\n    success\n  }\n}',
    variables: {},
  });
}

function apiRequestAssertions(expectedToken: string | null, expectedBody: any): MockedRequestAssertions {
  return (options) => {
    expect(options.body).toBeTypeOf('string');
    const body = JSON.parse(options.body as string);
    expect(body).toEqual(expectedBody);

    expect(options.headers).toBeDefined();
    const authHeader = new Headers(options.headers).get('Authorization');
    if (expectedToken) {
      expect(authHeader).toBe(`Bearer ${expectedToken}`);
    } else {
      expect(authHeader).toBeNull();
    }
  };
}

function successApiMutationResponse(): any {
  return {
    data: {
      fakeMutation: {
        success: true,
      },
    },
  };
}

function assertSuccessMutationResponse(response: any) {
  expect(response).toStrictEqual({
    fakeMutation: {
      success: true,
    },
  });
}

async function expectToFailWith<T>(executionSpec: () => Promise<void>, expectedErrorName: string): Promise<T> {
  try {
    await executionSpec();
  } catch (e) {
    console.debug('Caught error, checking if it is expected', e);
    expect(e).toHaveProperty('name', expectedErrorName);
    return e as T;
  }
  expect('API call expected to fail', 'API call expected to fail').toBeDefined();
}

function validationFailureResponse(
  validationErrors: Array<{
    path: string;
    error: ValidationErrorCode;
    message: string;
    params?: Array<{ name: string; value: string }>;
  }>,
): any {
  return {
    errors: [
      {
        message: 'Validation failed',
        extensions: {
          errorType: SaGrapQlErrorType.FieldValidationFailure,
          validationErrors,
        },
      },
    ],
  };
}

function businessErrorResponse(errorCode: string, message: string): any {
  return {
    errors: [
      {
        message,
        extensions: {
          errorType: SaGrapQlErrorType.BusinessError,
          errorCode,
        },
      },
    ],
  };
}
