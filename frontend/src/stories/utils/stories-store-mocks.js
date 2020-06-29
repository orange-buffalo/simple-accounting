import { store } from '@/stories/utils/stories-app';

export const DEFAULT_STORIES_WORKSPACE_ID = 42;
export const DEFAULT_STORIES_WORKSPACE = {
  id: DEFAULT_STORIES_WORKSPACE_ID,
  defaultCurrency: 'AUD',
  editable: true,
};

export function mockWorkspace(workspace) {
  store.commit('workspaces/setCurrentWorkspace', workspace);
}

export function createStoreMockDecorator() {
  return (fn, { parameters }) => {
    let workspace = DEFAULT_STORIES_WORKSPACE;
    if (parameters && parameters.workspace) {
      workspace = {
        ...DEFAULT_STORIES_WORKSPACE,
        ...parameters.workspace,
      };
    }
    mockWorkspace(workspace);

    return {
      template: '<story/>',
    };
  };
}
