import {
  describe, beforeEach, afterEach, it, expect, vi,
} from 'vitest';
import 'vi-fetch/setup';
import { mockFetch, mockGet, mockPost } from 'vi-fetch';

// eslint-disable-next-line max-len
const TOKEN = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY0NTV9.Zd2q76NaV27zZxMYxSJbDjzCjf4eAD4_aa16iQ4C-ABXZDzNAQWHCoajHGY3-7aOQnSSPo1uZxskY9B8dcHlfkr_lsEQHJ6I4yBYueYDC_V6MZmi3tVwBAeftrIhXs900ioxo0D2cLl7MAcMNGlQjrTDz62SrIrz30JnBOGnHbcK088rkbw5nLbdyUT0PA0w6EgDntJjtJS0OS7EHLpixFtenQR7LPKj-c7KdZybjShFAuw9L8cW5onKZb3S7AOzxwPcSGM2uKo2nc0EQ3Zo48gTtfieSBDCgpi0rymmDPpiq1yNB0U21A8n59DA9YDFf2Kaaf5ZjFAxvZ_Ul9a3Wg';
const API_TIME = new Date('2020-01-04T00:00:00');

describe('API Client', () => {
  let loginRequiredEventMock: any;
  let apiFatalErrorEventMock: any;
  let useAuth: any;

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
      API_FATAL_ERROR_EVENT: {
        emit: vi.fn(),
      },
    }));

    const events = await import('@/services/events');
    loginRequiredEventMock = events.LOGIN_REQUIRED_EVENT.emit;
    apiFatalErrorEventMock = events.API_FATAL_ERROR_EVENT.emit;
    ({ useAuth } = await import('@/services/api'));
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.resetAllMocks();
    vi.resetModules();
    mockFetch.clearAll();
  });

  it('adds a token to headers after successful autologin', async () => {
    expect.assertions(3);

    const mock = mockPost('/api/auth/token')
      .willResolve({
        token: TOKEN,
      });

    // httpMock.get('/api-call', (req, res) => {
    //   expect(req.header('Authorization'))
    //     .toBe(`Bearer ${TOKEN}`);
    //   return res.status(200)
    //     .body('');
    // });

    await useAuth().tryAutoLogin();

    expect(useAuth().getToken())
      .toBe(TOKEN);

    const response = mock.getRouteCalls()[0][1];
    expect(response).toBeDefined();
    expect(new Headers(response!.headers).get('Authorization')).toBeNull();

    // await apiClient.get('/api-call');
  });

  // it('does not set Authorization token when not logged in', async () => {
  //   expect.assertions(1);
  //
  //   httpMock.get('/api-call', (req, res) => {
  //     expect(req.header('Authorization'))
  //       .toBeNull();
  //     return res.status(200)
  //       .body('');
  //   });
  //
  //   await apiClient.get('/api-call');
  // });
  //
  // it('fires login event when 401 is received', (done) => {
  //   expect.assertions(1);
  //
  //   httpMock.get('/api-call', (req, res) => res.status(401)
  //     .body(''));
  //   httpMock.post('/api/auth/token', (req, res) => res.status(401)
  //     .body(''));
  //
  //   apiClient.get('/api-call')
  //     .then(() => {
  //       done(Error('should not call success callback'));
  //     })
  //     .catch(() => {
  //       expect(loginRequiredEventMock.mock.calls.length)
  //         .toBe(1);
  //       done();
  //     });
  // });
  //
  // it('does not fire any events on successful responses', async () => {
  //   httpMock.get('/api-call', (req, res) => res.status(200)
  //     .body(''));
  //
  //   await apiClient.get('/api-call');
  //
  //   expect(loginRequiredEventMock.mock.calls.length)
  //     .toBe(0);
  // });
  //
  // it('fires api fatal error event when 4xx or 5xx is received', (done) => {
  //   expect.assertions(1);
  //
  //   httpMock.get('/api-call', (req, res) => res.status(500)
  //     .body(''));
  //
  //   apiClient.get('/api-call')
  //     .then(() => {
  //       done(Error('should not call success callback'));
  //     })
  //     .catch(() => {
  //       expect(apiFatalErrorEventMock.mock.calls.length)
  //         .toBe(1);
  //       done();
  //     });
  // });
  //
  // it('fires not api fatal error event when skipGlobalErrorHandler is set', (done) => {
  //   expect.assertions(1);
  //
  //   httpMock.get('/api-call', (req, res) => res.status(500)
  //     .body(''));
  //
  //   apiClient
  //     .get('/api-call', {
  //       skipGlobalErrorHandler: true,
  //     } as any)
  //     .then(() => {
  //       done(Error('should not call success callback'));
  //     })
  //     .catch(() => {
  //       expect(apiFatalErrorEventMock.mock.calls.length)
  //         .toBe(0);
  //       done();
  //     });
  // });
});
