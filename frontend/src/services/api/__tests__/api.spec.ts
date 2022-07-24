import {
  afterEach, beforeEach, describe, expect, test, vi,
} from 'vitest';
import type { InspectionFilter, InspectionOptions } from 'fetch-mock';
import fetchMock from 'fetch-mock';
import type {
  Auth, FetchError, ProfileApiControllerApi, ResponseError,
} from '@/services/api';
import type { RequestMetadata } from '@/services/api/api-client';
import type { CancellableRequest } from '@/services/api/api-utils';
import type { AdditionalRequestParameters } from '@/services/api/generated/runtime';

// eslint-disable-next-line max-len
const TOKEN = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY0NTV9.Zd2q76NaV27zZxMYxSJbDjzCjf4eAD4_aa16iQ4C-ABXZDzNAQWHCoajHGY3-7aOQnSSPo1uZxskY9B8dcHlfkr_lsEQHJ6I4yBYueYDC_V6MZmi3tVwBAeftrIhXs900ioxo0D2cLl7MAcMNGlQjrTDz62SrIrz30JnBOGnHbcK088rkbw5nLbdyUT0PA0w6EgDntJjtJS0OS7EHLpixFtenQR7LPKj-c7KdZybjShFAuw9L8cW5onKZb3S7AOzxwPcSGM2uKo2nc0EQ3Zo48gTtfieSBDCgpi0rymmDPpiq1yNB0U21A8n59DA9YDFf2Kaaf5ZjFAxvZ_Ul9a3Wg';
const API_TIME = new Date('2020-01-04T00:00:00');

describe('API Client', () => {
  let loadingStartedEventMock: () => void;
  let loadingFinishedEventMock: () => void;
  let loginRequiredEventMock: () => void;
  let apiFatalErrorEventMock: (error: ResponseError | FetchError) => void;
  let useAuth: () => Auth;
  let profileApi: ProfileApiControllerApi<RequestMetadata>;
  let skipGlobalErrorHandler: () => AdditionalRequestParameters<RequestMetadata>;
  let requestTimeout: (timeoutMs: number) => AdditionalRequestParameters<RequestMetadata>;
  let useCancellableRequest: () => CancellableRequest;
  let defaultRequestSettings: () => RequestInit;

  test('does not set Authorization token when not logged in', async () => {
    fetchMock.get('/api/profile', {});

    await profileApi.getProfile();

    const options = safeGetCallOptions();
    expect(new Headers(options.headers).get('Authorization'))
      .toBeNull();
  });

  test('adds a token to headers after successful autologin', async () => {
    fetchMock.post('/api/auth/token', {
      token: TOKEN,
    });

    fetchMock.get('/api/profile', {
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

    const response = await profileApi.getProfile();
    expect(response)
      .toBeDefined();
    expect(response.userName)
      .toEqual('testUser');
  });

  test('tries autologin when 401 is received for a request', async () => {
    fetchMock.get('/api/profile', (_, request) => {
      if (!request || !request.headers) throw new Error();
      return new Headers(request.headers).get('Authorization')
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

    const { userName } = await profileApi.getProfile();

    expect(userName)
      .eq('someUser');
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
    expect(apiFatalErrorEventMock)
      .toHaveBeenCalledTimes(0);
    const calls = fetchMock.calls();
    expect(calls)
      .length(3);
    expect(calls[0][0])
      .eq('/api/profile');
    expect(calls[1][0])
      .eq('/api/auth/token');
    expect(calls[2][0])
      .eq('/api/profile');
  });

  test('fires events when 401 is received', async () => {
    fetchMock.get('/api/profile', {
      status: 401,
    });
    fetchMock.post('/api/auth/token', {
      status: 401,
    });

    await expectToFailWithResponseStatus(async () => {
      await profileApi.getProfile();
    }, 401);

    expect(loginRequiredEventMock)
      .toHaveBeenCalledOnce();
    expect(apiFatalErrorEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loadingStartedEventMock)
      .toHaveBeenCalledTimes(2);
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledTimes(2);
  });

  test('fires events on successful responses', async () => {
    fetchMock.get('/api/profile', {});

    await profileApi.getProfile();

    expect(loginRequiredEventMock)
      .toBeCalledTimes(0);
    expect(apiFatalErrorEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loadingStartedEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledOnce();
  });

  test('fires events when 4xx or 5xx is received', async () => {
    fetchMock.get('/api/profile', {
      status: 500,
    });

    await expectToFailWithResponseStatus(async () => {
      await profileApi.getProfile();
    }, 500);

    expect(apiFatalErrorEventMock)
      .toHaveBeenCalledOnce();
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loadingStartedEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledOnce();
  });

  test('fires events when 4xx or 5xx is received and skipGlobalErrorHandler is set', async () => {
    fetchMock.get('/api/profile', {
      status: 500,
    });

    await expectToFailWithResponseStatus(async () => {
      await profileApi.getProfile(defaultRequestSettings(), skipGlobalErrorHandler());
    }, 500);

    expect(apiFatalErrorEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loadingStartedEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledOnce();
  });

  test('fires events when request fails', async () => {
    fetchMock.get('/api/profile', {
      throws: new Error('Request failed'),
    });

    try {
      await profileApi.getProfile();
      expect(null, 'API call expected to fail')
        .toBeDefined();
    } catch (e) {
      expect(e)
        .to
        .have
        .nested
        .property('cause.message', 'Request failed');
    }

    expect(apiFatalErrorEventMock)
      .toHaveBeenCalled();
    expect(loadingStartedEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledOnce();
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
  });

  test('fires events when request fails and skipGlobalErrorHandler is set', async () => {
    fetchMock.get('/api/profile', {
      throws: new Error('Request failed'),
    });

    try {
      await profileApi.getProfile(defaultRequestSettings(), skipGlobalErrorHandler());
      expect(null, 'API call expected to fail')
        .toBeDefined();
    } catch (e) {
      expect(e)
        .to
        .have
        .nested
        .property('cause.message', 'Request failed');
    }

    expect(apiFatalErrorEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loadingStartedEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledOnce();
  });

  test('fails with timeout', async () => {
    vi.useRealTimers();

    fetchMock.get('/api/profile', 200, {
      delay: 20000,
    });

    try {
      await profileApi.getProfile(defaultRequestSettings(), requestTimeout(200));
      expect(null, 'API call expected to fail')
        .toBeDefined();
    } catch (e) {
      expect(e)
        .to
        .have
        .nested
        .property('cause.message', 'The operation was aborted.');
    }

    expect(apiFatalErrorEventMock)
      .toHaveBeenCalledTimes(1);
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loadingStartedEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledOnce();
  });

  test('fails with timeout when custom abort signal is set', async () => {
    vi.useRealTimers();

    fetchMock.get('/api/profile', 200, {
      delay: 20000,
    });

    try {
      await profileApi.getProfile({
        signal: new AbortController().signal,
      }, requestTimeout(200));
      expect(null, 'API call expected to fail')
        .toBeDefined();
    } catch (e) {
      expect(e)
        .to
        .have
        .nested
        .property('cause.message', 'The operation was aborted.');
    }
  });

  test('should support request cancellation', async () => {
    vi.useRealTimers();

    fetchMock.get('/api/profile', 200, {
      delay: 20000,
    });

    const {
      cancellableRequestConfig,
      cancelRequest,
    } = useCancellableRequest();

    setTimeout(cancelRequest, 500);

    try {
      await profileApi.getProfile(cancellableRequestConfig);
      expect(null, 'API call expected to fail')
        .toBeDefined();
    } catch (e) {
      expect(e)
        .to
        .have
        .nested
        .property('cause.message', 'The operation was aborted.');
    }

    expect(apiFatalErrorEventMock)
      .toHaveBeenCalledTimes(1);
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loadingStartedEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledOnce();
  });

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.setSystemTime(API_TIME);

    vi.mock('@/services/events', () => ({
      LOGIN_REQUIRED_EVENT: {
        emit: vi.fn<[], void>(),
      },
      LOADING_STARTED_EVENT: {
        emit: vi.fn<[], void>(),
      },
      LOADING_FINISHED_EVENT: {
        emit: vi.fn<[], void>(),
      },
      API_FATAL_ERROR_EVENT: {
        emit: vi.fn<[], void>(),
      },
    }));

    const events = await import('@/services/events');
    loadingStartedEventMock = events.LOADING_STARTED_EVENT.emit;
    loadingFinishedEventMock = events.LOADING_FINISHED_EVENT.emit;
    loginRequiredEventMock = events.LOGIN_REQUIRED_EVENT.emit;
    apiFatalErrorEventMock = events.API_FATAL_ERROR_EVENT.emit;
    ({
      useAuth,
      profileApi,
      skipGlobalErrorHandler,
      requestTimeout,
      useCancellableRequest,
      defaultRequestSettings,
    } = await import('@/services/api'));
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.resetAllMocks();
    vi.resetModules();
    fetchMock.restore();
  });
});

function safeGetCallOptions(filter?: InspectionFilter, options?: InspectionOptions): RequestInit {
  const calls = fetchMock.calls(filter, options);
  expect(calls)
    .to
    .have
    .length(1);
  const call = calls[0];
  expect(call)
    .toBeDefined();
  const [, callOptions] = call;
  expect(callOptions)
    .toBeDefined();
  // eslint-disable-next-line
  return callOptions!;
}

async function expectToFailWithResponseStatus(spec: () => Promise<void>, status: number) {
  try {
    await spec();
    expect(null, 'API call expected to fail')
      .toBeDefined();
  } catch (e) {
    expect(e)
      .to
      .have
      .nested
      .property('response.status', status);
  }
}
