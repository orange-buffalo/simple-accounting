import fetchMockClient, { RouteResponse, UserRouteConfig } from 'fetch-mock';
import { DEFAULT_STORIES_WORKSPACE_ID } from '@/__storybook__/decorators/stories-workspace-mocks';
import type { ApiPage } from '@/services/api';

export const fetchMock = fetchMockClient;

export const defaultWorkspacePath = (path: string) => `/api/workspaces/${DEFAULT_STORIES_WORKSPACE_ID}${path}`;

export function pathOnlyMatcher(path: string) {
  return `path:${path}`;
}

export function onGetToDefaultWorkspacePath(
  path: string,
  response: RouteResponse,
  options?: UserRouteConfig,
) {
  fetchMock.get(pathOnlyMatcher(defaultWorkspacePath(path)), response, options);
}

export const neverEndingGetRequest: UserRouteConfig = {
  delay: 999999,
};

export const neverEndingPostRequest: UserRouteConfig = {
  delay: 999999,
};

export function pageResponseRaw<T>(items: T[]): ApiPage<T> {
  return {
    pageNumber: 1,
    pageSize: items.length,
    totalElements: items.length,
    data: items,
  };
}

export function pageResponse<T>(...items: T[]): ApiPage<T> {
  return pageResponseRaw(items);
}

export function mockFailedStorageStatus() {
  fetchMock.get('/api/profile/documents-storage', { active: false });
}

export function mockSuccessStorageStatus() {
  fetchMock.get('/api/profile/documents-storage', { active: true });
}

export function mockLoadingStorageStatus() {
  fetchMock.get('/api/profile/documents-storage', {}, neverEndingGetRequest);
}

export function mockDefaultWorkspaceCurrenciesShortlist() {
  fetchMock.get(defaultWorkspacePath('/statistics/currencies-shortlist'), ['USD', 'EUR']);
}
