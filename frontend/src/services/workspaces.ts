import type { WorkspaceDto } from '@/services/api';
import { workspacesApi } from '@/services/api';
import { useStorage } from '@/services/storage';
import { WORKSPACE_CHANGED_EVENT } from '@/services/events';

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

async function loadWorkspaces() {
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
  }
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
  const currentWorkspaceApiUrl = (url: string) => `/workspaces/${currentWorkspaceId}/${url}`;
  const { defaultCurrency } = currentWorkspace;
  return {
    currentWorkspace,
    currentWorkspaceId,
    /**
     * TODO: remove usage
     * @deprecated
     */
    currentWorkspaceApiUrl,
    defaultCurrency,
  };
}

export async function initWorkspace() {
  await useWorkspaces()
    .loadWorkspaces();
}
