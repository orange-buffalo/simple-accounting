import { useWorkspaces } from '@/services/workspaces';
import type { WorkspaceDto } from '@/services/api';
import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';

export const DEFAULT_STORIES_WORKSPACE_ID = 42;
export const DEFAULT_STORIES_WORKSPACE: WorkspaceDto = {
  id: DEFAULT_STORIES_WORKSPACE_ID,
  defaultCurrency: 'AUD',
  editable: true,
  version: 0,
  taxEnabled: true,
  name: 'Workspace',
  multiCurrencyEnabled: true,
};

function mockWorkspace(workspace: WorkspaceDto) {
  const { setCurrentWorkspace } = useWorkspaces();
  setCurrentWorkspace(workspace);
}

export const createWorkspaceMockDecorator = decoratorFactory((parameters) => {
  let workspace = DEFAULT_STORIES_WORKSPACE;
  if (parameters.workspace) {
    workspace = {
      ...DEFAULT_STORIES_WORKSPACE,
      ...parameters.workspace,
    };
  }
  mockWorkspace(workspace);

  return {
    template: '<story/>',
  };
});
