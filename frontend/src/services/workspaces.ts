import type { WorkspaceDto } from '@/services/api';
import { workspacesApi } from '@/services/api';
import { WORKSPACE_CHANGED_EVENT } from '@/services/events';
import { useStorage } from '@/services/storage';

let currentWorkspace: WorkspaceDto | null;
let workspaces: WorkspaceDto[];
const storage = useStorage<number>('current-workspace');

function setCurrentWorkspace(workspace: WorkspaceDto) {
  if (workspace.id == null) throw new Error('Invalid workspace provided');
  currentWorkspace = workspace;
  storage.set(workspace.id);
  WORKSPACE_CHANGED_EVENT.emit(currentWorkspace);
}

function createWorkspace(workspace: WorkspaceDto) {
  workspaces.push(workspace);
  setCurrentWorkspace(workspace);
}

/**
 * Loads workspaces from the API. In case not workspaces are available, returns `false`.
 * Otherwise, sets the current workspace and return `true`.
 */
async function loadWorkspaces(): Promise<boolean> {
  workspaces = await workspacesApi.getWorkspaces();

  if (workspaces.length > 0) {
    const previousWsId = storage.getOrNull();
    if (previousWsId != null) {
      currentWorkspace = workspaces.find((it) => it.id === previousWsId) || null;

      if (!currentWorkspace) {
        const sharedWorkspaces = await workspacesApi.getSharedWorkspaces();
        currentWorkspace = sharedWorkspaces.find((it) => it.id === previousWsId) || null;
      }
    }

    if (!currentWorkspace) {
      [currentWorkspace] = workspaces;
    }

    if (currentWorkspace.id == null) throw new Error('Invalid workspace');

    storage.set(currentWorkspace.id);

    return true;
  }

  return false;
}

export function useWorkspaces() {
  return {
    createWorkspace,
    loadWorkspaces,
    setCurrentWorkspace,
  };
}

export function useCurrentWorkspace() {
  if (currentWorkspace == null || currentWorkspace.id == null) throw new Error('Workspace has not been set');
  const currentWorkspaceId = currentWorkspace.id;
  const { defaultCurrency } = currentWorkspace;
  return {
    currentWorkspace,
    currentWorkspaceId,
    defaultCurrency,
  };
}

/**
 * @deprecated - can return false if workspace has not been set, needs to be handled correctly;
 * use loadWorkspaces instead
 */
export async function initWorkspace(): Promise<boolean> {
  return useWorkspaces().loadWorkspaces();
}
