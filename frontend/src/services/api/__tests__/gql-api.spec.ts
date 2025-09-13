import {
  afterEach, beforeEach, describe, expect, test, vi,
} from 'vitest';
import 'whatwg-fetch';
import fetchMock from 'fetch-mock';
import { refreshAccessToken } from '@/services/api/gql-api-client';

// eslint-disable-next-line vue/max-len
const TOKEN = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY0NTV9.Zd2q76NaV27zZxMYxSJbDjzCjf4eAD4_aa16iQ4C-ABXZDzNAQWHCoajHGY3-7aOQnSSPo1uZxskY9B8dcHlfkr_lsEQHJ6I4yBYueYDC_V6MZmi3tVwBAeftrIhXs900ioxo0D2cLl7MAcMNGlQjrTDz62SrIrz30JnBOGnHbcK088rkbw5nLbdyUT0PA0w6EgDntJjtJS0OS7EHLpixFtenQR7LPKj-c7KdZybjShFAuw9L8cW5onKZb3S7AOzxwPcSGM2uKo2nc0EQ3Zo48gTtfieSBDCgpi0rymmDPpaq1yNB0U21A8n59DA9YDFf2Kaaf5ZjFAxvZ_Ul9a3Wg';

fetchMock.mockGlobal();

describe('GraphQL API Client', () => {
  const apiCallPath = '/api/graphql';

  test('refreshAccessToken mutation returns token on success', async () => {
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

  test('refreshAccessToken returns null when access token is null', async () => {
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

  test('refreshAccessToken returns null on network errors', async () => {
    fetchMock.post(apiCallPath, {
      throws: new Error('Network error'),
    });

    const result = await refreshAccessToken();
    expect(result).toBe(null);
  });

  test('refreshAccessToken returns null on GraphQL errors', async () => {
    fetchMock.post(apiCallPath, {
      errors: [
        {
          message: 'Something went wrong',
        },
      ],
    });

    const result = await refreshAccessToken();
    expect(result).toBe(null);
  });

  afterEach(() => {
    fetchMock.removeRoutes();
    fetchMock.clearHistory();
  });
});