import {
  describe, beforeEach, afterEach, test, expect, vi,
} from 'vitest';
import type { InspectionFilter, InspectionOptions } from 'fetch-mock';
import fetchMock from 'fetch-mock';
import type {
  Auth, ProfileApiControllerApi, ResponseError, FetchError,
} from '@/services/api';

// eslint-disable-next-line max-len
const TOKEN = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY0NTV9.Zd2q76NaV27zZxMYxSJbDjzCjf4eAD4_aa16iQ4C-ABXZDzNAQWHCoajHGY3-7aOQnSSPo1uZxskY9B8dcHlfkr_lsEQHJ6I4yBYueYDC_V6MZmi3tVwBAeftrIhXs900ioxo0D2cLl7MAcMNGlQjrTDz62SrIrz30JnBOGnHbcK088rkbw5nLbdyUT0PA0w6EgDntJjtJS0OS7EHLpixFtenQR7LPKj-c7KdZybjShFAuw9L8cW5onKZb3S7AOzxwPcSGM2uKo2nc0EQ3Zo48gTtfieSBDCgpi0rymmDPpiq1yNB0U21A8n59DA9YDFf2Kaaf5ZjFAxvZ_Ul9a3Wg';
const API_TIME = new Date('2020-01-04T00:00:00');

describe('API Client', () => {
  let loginRequiredEventMock: () => void;
  let apiFatalErrorEventMock: (error: ResponseError | FetchError) => void;
  let useAuth: () => Auth;
  let profileApi: ProfileApiControllerApi;

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

  test('does not set Authorization token when not logged in', async () => {
    fetchMock.get('/api/profile', {});

    await profileApi.getProfile();

    const options = safeGetCallOptions();
    expect(new Headers(options.headers).get('Authorization'))
      .toBeNull();
  });

  test('fires login event when 401 is received', async () => {
    fetchMock.get('/api/profile', {
      status: 401,
    });
    fetchMock.post('/api/auth/token', {
      status: 401,
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
        .property('response.status', 401);
    }
    expect(loginRequiredEventMock)
      .toHaveBeenCalled();
  });

  test('does not fire any events on successful responses', async () => {
    fetchMock.get('/api/profile', {});

    await profileApi.getProfile();

    expect(loginRequiredEventMock)
      .toBeCalledTimes(0);
  });

  test('fires api fatal error event when 4xx or 5xx is received', async () => {
    fetchMock.get('/api/profile', {
      status: 500,
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
        .property('response.status', 500);
    }
    expect(apiFatalErrorEventMock)
      .toHaveBeenCalled();
  });

  test('fires not api fatal error event when skipGlobalErrorHandler is set', async () => {
    fetchMock.get('/api/profile', {
      status: 500,
    });

    try {
      await profileApi.getProfile({}, { skipGlobalErrorHandler: true });
      expect(null, 'API call expected to fail')
        .toBeDefined();
    } catch (e) {
      expect(e)
        .to
        .have
        .nested
        .property('response.status', 500);
    }
    expect(apiFatalErrorEventMock)
      .toHaveBeenCalledTimes(0);
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
    loginRequiredEventMock = events.LOGIN_REQUIRED_EVENT.emit;
    apiFatalErrorEventMock = events.API_FATAL_ERROR_EVENT.emit;
    ({
      useAuth,
      profileApi,
    } = await import('@/services/api'));
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.resetAllMocks();
    vi.resetModules();
    fetchMock.restore();
  });
});

// eslint-disable-next-line no-undef
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
