import {
  afterEach, beforeEach, describe, expect, test, vi,
} from 'vitest';
import type { CallHistoryFilter, UserRouteConfig } from 'fetch-mock';
import 'whatwg-fetch';
import fetchMock from 'fetch-mock';
import { Client } from '@urql/vue';
import {
  ApiAuthError,
  ApiBusinessError,
  ApiFieldLevelValidationError,
  ApiRequestCancelledError,
  ApiTimeoutError,
  ClientApiError,
  FatalApiError,
  ResourceNotFoundError,
} from '@/services/api/api-errors';
import type { Auth, InvalidInputErrorDto, SaApiErrorDto } from '@/services/api';
import type { RequestConfigReturn, RequestConfigParams } from '@/services/api/api-utils';

// eslint-disable-next-line vue/max-len
const TOKEN = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY0NTV9.Zd2q76NaV27zZxMYxSJbDjzCjf4eAD4_aa16iQ4C-ABXZDzNAQWHCoajHGY3-7aOQnSSPo1uZxskY9B8dcHlfkr_lsEQHJ6I4yBYueYDC_V6MZmi3tVwBAeftrIhXs900ioxo0D2cLl7MAcMNGlQjrTDz62SrIrz30JnBOGnHbcK088rkbw5nLbdyUT0PA0w6EgDntJjtJS0OS7EHLpixFtenQR7LPKj-c7KdZybjShFAuw9L8cW5onKZb3S7AOzxwPcSGM2uKo2nc0EQ3Zo48gTtfieSBDCgpi0rymmDPpiq1yNB0U21A8n59DA9YDFf2Kaaf5ZjFAxvZ_Ul9a3Wg';
const API_TIME = new Date('2020-01-04T00:00:00');

fetchMock.mockGlobal();

type TestBusinessErrorDto = SaApiErrorDto & {
  someData: string;
}

describe('GraphQL API Client', () => {
  let loadingStartedEventMock: () => void;
  let loadingFinishedEventMock: () => void;
  let loginRequiredEventMock: () => void;
  let useAuth: () => Auth;
  let gqlClient: Client;
  let useRequestConfig: (params: RequestConfigParams) => RequestConfigReturn;

  const assertRegularRequestEvents = () => {
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loadingStartedEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledOnce();
  };

  const apiCall = async () => gqlClient
    .mutation(`
      mutation {
        fakeMutation {
          resultProperty
        }
      }
    `, {})
    .toPromise();
  const apiCallPath = '/api/graphql';

  test('does not set Authorization token when not logged in', async () => {
    fetchMock.post(apiCallPath, {});

    await apiCall();

    const options = safeGetCallOptions();
    expect(new Headers(options.headers).get('Authorization'))
      .toBeNull();
  });

  test('adds a token to headers after successful autologin', async () => {
    fetchMock.post('/api/auth/token', {
      token: TOKEN,
    });

    fetchMock.get(apiCallPath, {
      userName: 'testUser',
    }, {
      headers: {
        Authorization: `Bearer ${TOKEN}`,
      },
    });

    const autologin = await useAuth()
      .tryAutoLogin();

    expect(autologin)
      .equal(true);
    expect(useAuth()
      .getToken())
      .toBe(TOKEN);

    const response = await apiCall();
    expect(response)
      .toBeDefined();
    expect(response.userName)
      .toEqual('testUser');
  });

  test('tries autologin when 401 is received for a request', async () => {
    fetchMock.get(apiCallPath, ({ options }) => {
      if (!options || !options.headers) throw new Error();
      return new Headers(options.headers).get('Authorization')
        ? {
          status: 200,
          body: {
            userName: 'someUser',
          },
        } : {
          status: 401,
        };
    });
    fetchMock.post('/api/auth/token', {
      token: TOKEN,
    });

    const { userName } = await apiCall();

    expect(userName)
      .eq('someUser');
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
    const calls = fetchMock.callHistory.calls();
    expect(calls)
      .length(3);
    const paths = calls.map((call) => call.args[0]);
    expect(paths)
      .toEqual([apiCallPath, '/api/auth/token', apiCallPath]);
  });

  test('throws ApiAuthError and triggers events when 401 is received', async () => {
    fetchMock.get(apiCallPath, {
      status: 401,
    });
    fetchMock.post('/api/auth/token', {
      status: 401,
    });

    const error = await expectToFailWith<ApiAuthError>(async () => {
      await apiCall();
    }, 'ApiAuthError');
    expect(error.response.status)
      .toBe(401);

    expect(loginRequiredEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingStartedEventMock)
      .toHaveBeenCalledTimes(2);
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledTimes(2);
  });

  test('fires events on successful responses', async () => {
    fetchMock.get(apiCallPath, {});

    await apiCall();

    assertRegularRequestEvents();
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

    const events = await import('@/services/events');
    loadingStartedEventMock = events.LOADING_STARTED_EVENT.emit;
    loadingFinishedEventMock = events.LOADING_FINISHED_EVENT.emit;
    loginRequiredEventMock = events.LOGIN_REQUIRED_EVENT.emit;
    ({
      useAuth,
      useRequestConfig,
    } = await import('@/services/api'));
    ({ gqlClient } = await import('@/services/api/gql-api-client'));
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.resetAllMocks();
    vi.resetModules();
    fetchMock.removeRoutes();
    fetchMock.clearHistory();
  });
});

function safeGetCallOptions(filter?: CallHistoryFilter, options?: UserRouteConfig): RequestInit {
  const calls = fetchMock.callHistory.calls(filter, options);
  expect(calls)
    .to
    .have
    .length(1);
  const call = calls[0];
  expect(call)
    .toBeDefined();
  const callOptions = call.options;
  expect(callOptions)
    .toBeDefined();
  // eslint-disable-next-line
  return callOptions!;
}

async function expectToFailWith<T>(
  executionSpec: () => Promise<void>,
  expectedErrorName: string,
): Promise<T> {
  try {
    await executionSpec();
  } catch (e) {
    expect(e)
      .toHaveProperty('name', expectedErrorName);
    return e as T;
  }
  expect('API call expected to fail', 'API call expected to fail')
      .toBeNull();
}
