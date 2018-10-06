import {assert} from 'chai'
import {initApi, api} from '@/services/api'

require('jsdom-global')(null, {
  url: "http://localhost/"
})

describe('api service', () => {

  let server;
  let storeStub = {
    state: {
      api: {
        jwtToken: undefined
      }
    }
  }

  beforeEach(() => {
    let sinon = require('sinon')
    server = sinon.useFakeServer();
    global.XMLHttpRequest = global.window.XMLHttpRequest
    initApi(storeStub)
  });

  afterEach(() => {
    server.restore();
    global.XMLHttpRequest = global.window.XMLHttpRequest
  });

  function respond() {
    setTimeout(() => server.respond(), 0);
  }

  it('adds a token to headers when present', (done) => {
    storeStub.state.api.jwtToken = 'token'

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
    storeStub.state.api.jwtToken = undefined

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

