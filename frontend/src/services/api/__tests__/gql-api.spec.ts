import {
  afterEach, beforeEach, describe, expect, test, vi,
} from 'vitest';
import 'whatwg-fetch';
import fetchMock from 'fetch-mock';
import { gqlClient, refreshAccessToken } from '@/services/api/gql-api-client';

// eslint-disable-next-line vue/max-len
const TOKEN = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY0NTV9.Zd2q76NaV27zZxMYxSJbDjzCjf4eAD4_aa16iQ4C-ABXZDzNAQWHCoajHGY3-7aOQnSSPo1uZxskY9B8dcHlfkr_lsEQHJ6I4yBYueYDC_V6MZmi3tVwBAeftrIhXs900ioxo0D2cLl7MAcMNGlQjrTDz62SrIrz30JnBOGnHbcK088rkbw5nLbdyUT0PA0w6EgDntJjtJS0OS7EHLpixFtenQR7LPKj-c7KdZybjShFAuw9L8cW5onKZb3S7AOzxwPcSGM2uKo2nc0EQ3Zo48gTtfieSBDCgpi0rymmDPpiq1yNB0U21A8n59DA9YDFf2Kaaf5ZjFAxvZ_Ul9a3Wg';
const API_TIME = new Date('2020-01-04T00:00:00');

fetchMock.mockGlobal();

const USER_PROFILE_QUERY = `
  query UserProfile {
    userProfile {
      userName
      i18n {
        language
        locale
      }
      documentsStorage
    }
  }
`;

describe('GraphQL API Client', () => {
  let loadingStartedEventMock: any;
  let loadingFinishedEventMock: any;
  let loginRequiredEventMock: any;

  const apiCall = async () => {
    return gqlClient.query(USER_PROFILE_QUERY, {}).toPromise();
  };

  const apiCallPath = '/api/graphql';

  test('does not set Authorization token when not logged in', async () => {
    fetchMock.post(apiCallPath, {
      data: {
        userProfile: {
          userName: 'testUser',
          i18n: {
            language: 'en',
            locale: 'en-US',
          },
          documentsStorage: null,
        },
      },
    });

    await apiCall();

    const calls = fetchMock.callHistory.calls();
    expect(calls).toHaveLength(1);
    const callOptions = calls[0].options;
    expect(new Headers(callOptions.headers).get('Authorization'))
      .toBeNull();
  });

  test('fires loading events on requests', async () => {
    fetchMock.post(apiCallPath, {
      data: {
        userProfile: {
          userName: 'testUser',
          i18n: {
            language: 'en',
            locale: 'en-US',
          },
          documentsStorage: null,
        },
      },
    });

    await apiCall();

    expect(loadingStartedEventMock).toHaveBeenCalled();
    expect(loadingFinishedEventMock).toHaveBeenCalled();
  });

  test('handles GraphQL authorization errors', async () => {
    fetchMock.post(apiCallPath, {
      errors: [
        {
          message: 'Not authorized',
          extensions: {
            errorType: 'NOT_AUTHORIZED',
          },
        },
      ],
    });

    const response = await apiCall();
    
    expect(response.error).toBeDefined();
    expect(loginRequiredEventMock).toHaveBeenCalled();
  });

  test('handles successful responses', async () => {
    fetchMock.post(apiCallPath, {
      data: {
        userProfile: {
          userName: 'testUser',
          i18n: {
            language: 'en',
            locale: 'en-US',
          },
          documentsStorage: null,
        },
      },
    });

    const response = await apiCall();

    expect(response.data).toBeDefined();
    expect(response.data?.userProfile?.userName).toEqual('testUser');
  });

  test('handles network errors', async () => {
    fetchMock.post(apiCallPath, {
      throws: new Error('Network error'),
    });

    const response = await apiCall();
    
    expect(response.error).toBeDefined();
    expect(response.error?.networkError).toBeDefined();
  });

  test('refreshAccessToken mutation works correctly', async () => {
    fetchMock.post(apiCallPath, {
      data: {
        refreshAccessToken: {
          accessToken: TOKEN,
        },
      },
    });

    const result = await refreshAccessToken();
    expect(result).toBe(TOKEN);
  });

  test('refreshAccessToken returns null on failure', async () => {
    fetchMock.post(apiCallPath, {
      data: {
        refreshAccessToken: {
          accessToken: null,
        },
      },
    });

    const result = await refreshAccessToken();
    expect(result).toBe(null);
  });

  test('refreshAccessToken handles network errors', async () => {
    fetchMock.post(apiCallPath, {
      throws: new Error('Network error'),
    });

    const result = await refreshAccessToken();
    expect(result).toBe(null);
  });

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.setSystemTime(API_TIME);

    // Mock events
    loadingStartedEventMock = vi.fn();
    loadingFinishedEventMock = vi.fn();
    loginRequiredEventMock = vi.fn();

    vi.doMock('@/services/events', () => ({
      LOGIN_REQUIRED_EVENT: {
        emit: loginRequiredEventMock,
      },
      LOADING_STARTED_EVENT: {
        emit: loadingStartedEventMock,
      },
      LOADING_FINISHED_EVENT: {
        emit: loadingFinishedEventMock,
      },
    }));

    // Mock auth functions
    vi.doMock('@/services/api/auth', () => ({
      getAuthorizationHeader: vi.fn(() => null),
    }));
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.resetAllMocks();
    vi.resetModules();
    fetchMock.removeRoutes();
    fetchMock.clearHistory();
  });
});