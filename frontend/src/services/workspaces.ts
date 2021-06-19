import { apiClient, WorkspaceDto } from '@/services/api';
import { useStorage } from '@/services/storage';

let currentWorkspace: WorkspaceDto | null;
let workspaces: WorkspaceDto[];
const storage = useStorage<number>('current-workspace');

function setCurrentWorkspace(workspace: WorkspaceDto) {
  currentWorkspace = workspace;
  storage.set(workspace.id!);
}

function createWorkspace(workspace: WorkspaceDto) {
  workspaces.push(workspace);
  setCurrentWorkspace(workspace);
}

async function loadWorkspaces() {
  const workspacesResponse = await apiClient.getWorkspaces();
  workspaces = workspacesResponse.data;

  if (workspaces.length > 0) {
    const previousWsId = storage.getOrNull();
    if (previousWsId != null) {
      currentWorkspace = workspaces.find((it) => it.id === previousWsId) || null;

      if (!currentWorkspace) {
        const sharedWorkspacesResponse = await apiClient.getSharedWorkspaces();
        const sharedWorkspaces = sharedWorkspacesResponse.data;

        currentWorkspace = sharedWorkspaces.find((it) => it.id === previousWsId) || null;
      }
    }

    if (!currentWorkspace) {
      [currentWorkspace] = workspaces;
    }

    storage.set(currentWorkspace.id!);
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
  const currentWorkspaceId = currentWorkspace!.id!;
  const currentWorkspaceApiUrl = (url: string) => `/workspaces/${currentWorkspaceId}/${url}`;
  const { defaultCurrency } = currentWorkspace!;
  return {
    currentWorkspace: currentWorkspace!,
    currentWorkspaceId,
    currentWorkspaceApiUrl,
    defaultCurrency,
  };
}

export async function initWorkspace() {
  await useWorkspaces()
    .loadWorkspaces();
}
