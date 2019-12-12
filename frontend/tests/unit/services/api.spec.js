import { assert } from 'chai';
import * as eventBus from 'eventbusjs';
import { api, LOGIN_REQUIRED_EVENT } from '@/services/api';

require('jsdom-global')(null, {
  url: 'http://localhost/',
});

describe('api service', () => {
  let server;
  let dispatchStub;

  beforeEach(() => {
    const sinon = require('sinon');
    server = sinon.useFakeServer();
    global.XMLHttpRequest = global.window.XMLHttpRequest;
    dispatchStub = sinon.stub(eventBus, 'dispatch');
  });

  afterEach(() => {
    server.restore();
    global.XMLHttpRequest = global.window.XMLHttpRequest;
    dispatchStub.restore();
  });

  function respond() {
    setTimeout(() => server.respond(), 0);
  }

  it('adds a token to headers when present', (done) => {
    server.respondWith([200, {}, '']);

    api.get('/api-call')
      .then(() => {
        assert.equal(server.requests.length, 1);
        assert.include(server.requests[0].requestHeaders, { Authorization: 'Bearer token' });
        done();
      })
      .catch(done);

    respond();
  });

  it('does not set Authorization token when token is not defined', (done) => {
    server.respondWith([200, {}, '']);

    api.get('/api-call')
      .then(() => {
        assert.equal(server.requests.length, 1);
        assert.notProperty(server.requests[0].requestHeaders, 'Authorization');
        done();
      })
      .catch(done);

    respond();
  });

  it(`fires "${LOGIN_REQUIRED_EVENT}" event when 401 is received`, (done) => {
    server.respondWith([401, {}, '']);

    api.get('/api-call')
      .then(() => {
        done(Error('should not call success callback'));
      })
      .catch(() => {
        assert(dispatchStub.withArgs(LOGIN_REQUIRED_EVENT).calledOnce, 'expecting event about required login');
        done();
      });

    respond();
  });

  it('does not fire any events on successful responses', (done) => {
    server.respondWith([200, {}, '']);

    api.get('/api-call')
      .then(() => {
        assert(dispatchStub.notCalled, 'expecting no events');
        done();
      })
      .catch(done);

    respond();
  });
});
