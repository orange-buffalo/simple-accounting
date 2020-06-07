import { api } from '@/services/api';
// eslint-disable-next-line import/no-extraneous-dependencies
import MockAdapter from 'axios-mock-adapter';
import { store } from '@/stories/utils/stories-app';

let apiMock;

export function onGetToWorkspacePath(pathRegExp) {
  return apiMock.onGet(new RegExp(`/api/workspaces/${store.state.workspaces.currentWorkspace.id}/${pathRegExp}`));
}

export function resetApiMock() {
  apiMock.reset();
}

export function createApiMockDecorator() {
  apiMock = new MockAdapter(api);
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
