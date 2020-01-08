import httpMock from 'xhr-mock';
import { advanceTo, clear } from 'jest-date-mock';

// eslint-disable-next-line max-len
const TOKEN = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2Iiwicm9sZXMiOlsiVVNFUiJdLCJ0cmFuc2llbnQiOmZhbHNlLCJleHAiOjE1NzgxMTY0NTV9.Zd2q76NaV27zZxMYxSJbDjzCjf4eAD4_aa16iQ4C-ABXZDzNAQWHCoajHGY3-7aOQnSSPo1uZxskY9B8dcHlfkr_lsEQHJ6I4yBYueYDC_V6MZmi3tVwBAeftrIhXs900ioxo0D2cLl7MAcMNGlQjrTDz62SrIrz30JnBOGnHbcK088rkbw5nLbdyUT0PA0w6EgDntJjtJS0OS7EHLpixFtenQR7LPKj-c7KdZybjShFAuw9L8cW5onKZb3S7AOzxwPcSGM2uKo2nc0EQ3Zo48gTtfieSBDCgpi0rymmDPpiq1yNB0U21A8n59DA9YDFf2Kaaf5ZjFAxvZ_Ul9a3Wg';
const API_TIME = new Date('2020-01-04T00:00:00');

describe('api service', () => {
  let api;
  let LOGIN_REQUIRED_EVENT;
  let eventBusDispatchMock;

  beforeEach(() => {
    httpMock.setup();
    jest.resetModules();

    advanceTo(API_TIME);

    jest.mock('eventbusjs', () => ({
      dispatch: jest.fn(),
    }));

    ({ dispatch: eventBusDispatchMock } = require('eventbusjs'));

    ({
      api,
      LOGIN_REQUIRED_EVENT,
    } = require('@/services/api'));
  });

  afterEach(() => {
    httpMock.reset();
    clear();
  });

  it('adds a token to headers when present', async () => {
    expect.assertions(3);

    httpMock.post('/api/auth/token', (req, res) => {
      expect(req.header('Authorization'))
        .toBeNil();
      return res.status(200)
        .body({ token: TOKEN });
    });

    httpMock.get('/api/api-call', (req, res) => {
      expect(req.header('Authorization'))
        .toBe(`Bearer ${TOKEN}`);
      return res.status(200)
        .body('');
    });

    await api.tryAutoLogin();

    expect(api.getToken())
      .toBe(TOKEN);

    await api.get('/api-call');
  });

  it('does not set Authorization token when token is not defined', async () => {
    expect.assertions(1);

    httpMock.get('/api/api-call', (req, res) => {
      expect(req.header('Authorization'))
        .toBeNil();
      return res.status(200)
        .body('');
    });

    await api.get('/api-call');
  });

  it('fires login event when 401 is received', (done) => {
    httpMock.get('/api/api-call', (req, res) => res.status(401)
      .body(''));
    httpMock.post('/api/auth/token', (req, res) => res.status(401)
      .body(''));

    api.get('/api-call')
      .then(() => {
        done(Error('should not call success callback'));
      })
      .catch(() => {
        expect(eventBusDispatchMock.mock.calls.length)
          .toBe(1);
        expect(eventBusDispatchMock.mock.calls[0][0])
          .toBe(LOGIN_REQUIRED_EVENT);
        done();
      });
  });

  it('does not fire any events on successful responses', async () => {
    httpMock.get('/api/api-call', (req, res) => res.status(200)
      .body(''));

    await api.get('/api-call');

    expect(eventBusDispatchMock.mock.calls.length)
      .toBe(0);
  });
});
