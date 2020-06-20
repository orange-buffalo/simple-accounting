import { store } from '@/stories/utils/stories-app';

export const DEFAULT_STORIES_WORKSPACE_ID = 42;
export const DEFAULT_STORIES_WORKSPACE = {
  id: DEFAULT_STORIES_WORKSPACE_ID,
};

export function mockWorkspace(workspace) {
  store.commit('workspaces/setCurrentWorkspace', workspace);
}

export function createStoreMockDecorator() {
  return (fn, { parameters }) => {
    const workspace = (parameters && parameters.workspace) || DEFAULT_STORIES_WORKSPACE;
    mockWorkspace(workspace);

    return {
      template: '<story/>',
    };
  };
}
