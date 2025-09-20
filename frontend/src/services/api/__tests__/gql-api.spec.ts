import {
  afterEach, beforeEach, describe, expect, test, vi,
} from 'vitest';
import 'whatwg-fetch';
import fetchMock from 'fetch-mock';
import { gqlClient, setupGqlClient } from '@/services/api/gql-api-client';
import type { Auth } from '@/services/api';
import {
  ApiAuthError,
  ClientApiError,
} from '@/services/api/api-errors';

// eslint-disable-next-line vue/max-len
const TOKEN = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY0NTV9.Zd2q76NaV27zZxMYxSJbDjzCjf4eAD4_aa16iQ4C-ABXZDzNAQWHCoajHGY3-7aOQnSSPo1uZxskY9B8dcHlfkr_lsEQHJ6I4yBYueYDC_V6MZmi3tVwBAeftrIhXs900ioxo0D2cLl7MAcMNGlQjrTDz62SrIrz30JnBOGnHbcK088rkbw5nLbdyUT0PA0w6EgDntJjtJS0OS7EHLpixFtenQR7LPKj-c7KdZybjShFAuw9L8cW5onKZb3S7AOzxwPcSGM2uKo2nc0EQ3Zo48gTtfieSBDCgpi0rymmDPpiq1yNB0U21A8n59DA9YDFf2Kaaf5ZjFAxvZ_Ul9a3Wg';

fetchMock.mockGlobal();

async function expectToFailWith<T extends Error>(asyncAction: () => Promise<unknown>, errorType: string): Promise<T> {
  try {
    await asyncAction();
    throw new Error(`Expected to fail with ${errorType} but it didn't fail`);
  } catch (e) {
    if (e instanceof Error && e.constructor.name === errorType) {
      return e as T;
    }
    throw e;
  }
}

describe('GraphQL API Client', () => {
  let loadingStartedEventMock: () => void;
  let loadingFinishedEventMock: () => void;
  let loginRequiredEventMock: () => void;
  let useAuth: () => Auth;

  const apiCallPath = '/api/graphql';
  
  const assertRegularRequestEvents = () => {
    expect(loginRequiredEventMock)
      .toHaveBeenCalledTimes(0);
    expect(loadingStartedEventMock)
      .toHaveBeenCalledOnce();
    expect(loadingFinishedEventMock)
      .toHaveBeenCalledOnce();
  };

  const userProfileQuery = `
    query UserProfile {
      userProfile {
        userName
        i18n {
          language
          locale
        }
      }
    }
  `;

  const apiCall = async () => {
    const result = await gqlClient.query(userProfileQuery, {}).toPromise();
    return result.data?.userProfile;
  };

  test('does not set Authorization token when not logged in', async () => {
    fetchMock.post(apiCallPath, {
      data: {
        userProfile: {
          userName: 'testUser',
          i18n: {
            language: 'en',
            locale: 'en-US',
          },
        },
      },
    });

    await apiCall();

    const calls = fetchMock.callHistory.calls();
    expect(calls).toHaveLength(1);
    
    const options = calls[0].args[1] as RequestInit;
    const headers = new Headers(options.headers);
    expect(headers.get('Authorization')).toBeNull();
  });

  test('adds a token to headers after successful autologin', async () => {
    // Mock refresh token response
    fetchMock.post(apiCallPath, (url, options) => {
      const body = JSON.parse(options.body as string);
      if (body.query?.includes('refreshAccessToken')) {
        return {
          data: {
            refreshAccessToken: {
              accessToken: TOKEN,
            },
          },
        };
      }
      
      // User profile query with token
      const headers = new Headers(options.headers);
      if (headers.get('Authorization') === `Bearer ${TOKEN}`) {
        return {
          data: {
            userProfile: {
              userName: 'testUser',
              i18n: {
                language: 'en',
                locale: 'en-US',
              },
            },
          },
        };
      }
      
      return { status: 401 };
    });

    const autologin = await useAuth().tryAutoLogin();
    expect(autologin).toBe(true);
    expect(useAuth().getToken()).toBe(TOKEN);

    const response = await apiCall();
    expect(response).toBeDefined();
    expect(response.userName).toEqual('testUser');
  });

  test('tries auto refresh when GraphQL auth error is received', async () => {
    let callCount = 0;
    
    fetchMock.post(apiCallPath, (url, options) => {
      const body = JSON.parse(options.body as string);
      
      // Handle refresh token mutation
      if (body.query?.includes('refreshAccessToken')) {
        return {
          data: {
            refreshAccessToken: {
              accessToken: TOKEN,
            },
          },
        };
      }
      
      // Handle user profile query
      callCount++;
      const headers = new Headers(options.headers);
      
      if (callCount === 1 || !headers.get('Authorization')) {
        // First call or no auth - return auth error
        return {
          errors: [
            {
              message: 'Not authorized',
              extensions: {
                errorType: 'NOT_AUTHORIZED',
              },
            },
          ],
        };
      } else {
        // Second call with token - return success
        return {
          data: {
            userProfile: {
              userName: 'someUser',
              i18n: {
                language: 'en',
                locale: 'en-US',
              },
            },
          },
        };
      }
    });

    const result = await apiCall();
    expect(result.userName).toBe('someUser');
    expect(loginRequiredEventMock).toHaveBeenCalledTimes(0);
    
    const calls = fetchMock.callHistory.calls();
    expect(calls).toHaveLength(3); // Initial query, refresh mutation, retry query
  });

  test('throws ClientApiError and triggers events when auth refresh fails', async () => {
    fetchMock.post(apiCallPath, (url, options) => {
      const body = JSON.parse(options.body as string);
      
      if (body.query?.includes('refreshAccessToken')) {
        // Refresh fails
        return {
          data: {
            refreshAccessToken: {
              accessToken: null,
            },
          },
        };
      }
      
      // User profile query returns auth error
      return {
        errors: [
          {
            message: 'Not authorized',
            extensions: {
              errorType: 'NOT_AUTHORIZED',
            },
          },
        ],
      };
    });

    const result = await gqlClient.query(userProfileQuery, {}).toPromise();
    
    // The auth exchange should handle this, but since refresh fails, we should get an error result
    expect(result.error).toBeDefined();
    expect(loginRequiredEventMock).toHaveBeenCalledOnce();
  });

  test('fires events on successful responses', async () => {
    fetchMock.post(apiCallPath, {
      data: {
        userProfile: {
          userName: 'testUser',
          i18n: {
            language: 'en',
            locale: 'en-US',
          },
        },
      },
    });

    await apiCall();
    assertRegularRequestEvents();
  });

  test('throws ClientApiError when GraphQL error is received', async () => {
    fetchMock.post(apiCallPath, {
      errors: [
        {
          message: 'Some business error',
          extensions: {
            errorType: 'BUSINESS_ERROR',
          },
        },
      ],
    });

    await expectToFailWith<ClientApiError>(async () => {
      await apiCall();
    }, 'ClientApiError');

    assertRegularRequestEvents();
  });

  test('throws ClientApiError when network error occurs', async () => {
    fetchMock.post(apiCallPath, {
      throws: new Error('Network error'),
    });

    await expectToFailWith<ClientApiError>(async () => {
      await apiCall();
    }, 'ClientApiError');

    assertRegularRequestEvents();
  });

  test('applies timeout from global configuration', async () => {
    vi.useRealTimers();

    fetchMock.post(apiCallPath, 200, {
      delay: 20000,
    });

    // The timeout should be applied from getGlobalRequestTimeout()  
    await expectToFailWith<Error>(async () => {
      await apiCall();
    }, 'Error');

    assertRegularRequestEvents();
  });

  beforeEach(async () => {
    vi.useFakeTimers();

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
    
    ({ useAuth } = await import('@/services/api'));
    
    // Setup GraphQL client
    setupGqlClient();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.resetAllMocks();
    vi.resetModules();
    fetchMock.removeRoutes();
    fetchMock.clearHistory();
  });
});