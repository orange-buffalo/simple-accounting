/* eslint-disable import/no-extraneous-dependencies */
import { Polly } from '@pollyjs/core';
import XHRAdapter from '@pollyjs/adapter-xhr';
import { store } from '@/stories/utils/stories-app';

Polly.register(XHRAdapter);

let polly;
let server;

resetApiMock();

function enrichRequestMock(requestMock) {
  // eslint-disable-next-line no-param-reassign
  requestMock.neverEndingRequest = function neverEndingRequest() {
    return this.intercept(async () => server.timeout(99999999));
  };

  // eslint-disable-next-line no-param-reassign
  requestMock.successJson = function successJson(jsonOrSupplier) {
    return this.intercept((req, res) => {
      let json = jsonOrSupplier;
      if (typeof jsonOrSupplier === 'function') {
        json = jsonOrSupplier();
      }
      res.status(200)
        .json(json);
    });
  };

  // eslint-disable-next-line no-param-reassign
  requestMock.reply = function reply(statusCode, json) {
    return this.intercept((req, res) => res.status(statusCode)
      .json(json));
  };

  return requestMock;
}

export async function responseDelay(timeout) {
  await server.timeout(timeout);
}

export function onGetToWorkspacePath(subPath) {
  return onGet(`api/workspaces/${store.state.workspaces.currentWorkspace.id}/${subPath}`);
}

export function onPostToWorkspacePath(subPath) {
  return onPost(`api/workspaces/${store.state.workspaces.currentWorkspace.id}/${subPath}`);
}

export function onGet(path) {
  return enrichRequestMock(server.get(path));
}

export function onPost(path) {
  return enrichRequestMock(server.post(path));
}

export function resetApiMock() {
  if (polly) {
    polly.disconnect();
    polly.stop();
  }
  polly = new Polly('Stories API Mock', {
    adapters: ['xhr'],
    logging: true,
  });
  server = polly.server;

  // support for hot reload
  server.get('/:id.hot-update.json')
    .passthrough();
}

export function createApiMockDecorator() {
  return () => {
    resetApiMock();
    return ({
      template: '<story/>',
    });
  };
}

export function apiPage(data) {
  return {
    pageNumber: 1,
    totalElements: data.length,
    pageSize: data.length,
    data,
  };
}
