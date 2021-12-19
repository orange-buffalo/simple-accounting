import { Polly, PollyServer, RouteHandler } from '@pollyjs/core';
import XHRAdapter from '@pollyjs/adapter-xhr';
import { ApiPage } from '@/services/api';
import { useCurrentWorkspace } from '@/services/workspaces';

Polly.register(XHRAdapter);

let polly: Polly | null;
let server: PollyServer;

resetApiMock();

interface EnhancedRouteHandler extends RouteHandler {
  neverEndingRequest: () => EnhancedRouteHandler,
  successJson: (jsonOrSupplier: any) => EnhancedRouteHandler,
  reply: (statusCode: number, json: any) => EnhancedRouteHandler,
}

function enrichRequestMock(requestMock: RouteHandler): EnhancedRouteHandler {
  const enhancedMock = requestMock as EnhancedRouteHandler;

  enhancedMock.neverEndingRequest = function neverEndingRequest() {
    return this.intercept(async () => server.timeout(99999999)) as EnhancedRouteHandler;
  };

  enhancedMock.successJson = function successJson(jsonOrSupplier: any): EnhancedRouteHandler {
    return this.intercept((req, res) => {
      let json = jsonOrSupplier;
      if (typeof jsonOrSupplier === 'function') {
        json = jsonOrSupplier();
      }
      res.status(200)
        .json(json);
    }) as EnhancedRouteHandler;
  };

  enhancedMock.reply = function reply(statusCode: number, json: any) {
    return this.intercept((req, res) => {
      res.status(statusCode)
        .json(json);
    }) as EnhancedRouteHandler;
  };

  return enhancedMock;
}

export async function responseDelay(timeout: number) {
  await server.timeout(timeout);
}

export function onGetToWorkspacePath(subPath: string) {
  const { currentWorkspaceId } = useCurrentWorkspace();
  return onGet(`api/workspaces/${currentWorkspaceId}/${subPath}`);
}

export function onPutToWorkspacePath(subPath: string) {
  const { currentWorkspaceId } = useCurrentWorkspace();
  return onPut(`api/workspaces/${currentWorkspaceId}/${subPath}`);
}

export function onPostToWorkspacePath(subPath: string) {
  const { currentWorkspaceId } = useCurrentWorkspace();
  return onPost(`api/workspaces/${currentWorkspaceId}/${subPath}`);
}

export function onGet(path: string) {
  return enrichRequestMock(server.get(path));
}

export function onPost(path: string) {
  return enrichRequestMock(server.post(path));
}

export function onPut(path: string) {
  return enrichRequestMock(server.put(path));
}

export function resetApiMock() {
  if (polly) {
    polly.disconnect();
    polly.stop();
  }
  polly = new Polly('Stories API Mock', {
    adapters: ['xhr'],
    logLevel: 'trace',
  });
  server = polly.server;

  // support for hot reload
  server.get('/:id.hot-update.json')
    .passthrough();

  // block initial router validation
  server.post('/api/auth/token')
    .intercept(async () => server.timeout(99999999));
}

export function createApiMockDecorator() {
  return () => {
    resetApiMock();
    return ({
      template: '<story/>',
    });
  };
}

export function apiPage<T>(data: Array<T>): ApiPage<T> {
  return {
    pageNumber: 1,
    totalElements: data.length,
    pageSize: data.length,
    data,
  };
}
