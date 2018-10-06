import {assert} from 'chai'
import * as apiStoreModule from '@/services/api-store'
import api from '@/services/api'

require('jsdom-global')(null, {
  url: "http://localhost/"
})

describe('api service', () => {

  let server;
  let jwtTokenStub;

  beforeEach(() => {
    let sinon = require('sinon')
    server = sinon.useFakeServer();
    global.XMLHttpRequest = global.window.XMLHttpRequest
    jwtTokenStub = sinon.stub(apiStoreModule.store.state, 'jwtToken')
  });

  afterEach(() => {
    server.restore();
    global.XMLHttpRequest = global.window.XMLHttpRequest
    jwtTokenStub.restore()
  });

  function respond() {
    setTimeout(() => server.respond(), 0);
  }

  it('adds a token to headers when present', (done) => {
    jwtTokenStub.get(() => 'token')

    server.respondWith([200, {}, ""])

    api.get('/api-call')
        .then(() => {
          assert.equal(server.requests.length, 1)
          assert.include(server.requests[0].requestHeaders, {'Authorization': 'Bearer: token'})
          done()
        })
        .catch(done)

    respond()
  })

  it('does not set Authorization token when token is not defined', (done) => {
    jwtTokenStub.get(() => undefined)

    server.respondWith([200, {}, ""])

    api.get('/api-call')
        .then(() => {
          assert.equal(server.requests.length, 1)
          assert.notProperty(server.requests[0].requestHeaders, 'Authorization')
          done()
        })
        .catch(done)

    respond()
  })
})

