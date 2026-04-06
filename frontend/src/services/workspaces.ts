import { useStorage } from '@/services/storage';
import { WORKSPACE_CHANGED_EVENT } from '@/services/events';
import { graphql } from '@/services/api/gql';
import { gqlClient } from '@/services/api/gql-api-client.ts';

export interface WorkspaceInfo {
  id: number;
  name: string;
  defaultCurrency: string;
  editable: boolean;
}

let currentWorkspace: WorkspaceInfo | null;
let workspaces: WorkspaceInfo[];
const storage = useStorage<number>('current-workspace');

const allWorkspacesQuery = graphql(`
  query allWorkspaces($first: Int!) {
    workspaces(first: $first) {
      edges {
        node {
          id
          name
          defaultCurrency
        }
      }
    }
  }
`);

const workspaceByIdQuery = graphql(`
  query workspaceById($id: Long!) {
    workspace(id: $id) {
      id
      name
      defaultCurrency
    }
  }
`);

function setCurrentWorkspace(workspace: WorkspaceInfo) {
  if (workspace.id == null) throw new Error('Invalid workspace provided');
  currentWorkspace = workspace;
  storage.set(workspace.id);
  WORKSPACE_CHANGED_EVENT.emit(currentWorkspace);
}

function createWorkspace(workspace: WorkspaceInfo) {
  workspaces.push(workspace);
  setCurrentWorkspace(workspace);
}

/**
 * Loads workspaces from the API. In case not workspaces are available, returns `false`.
 * Otherwise, sets the current workspace and return `true`.
 */
async function loadWorkspaces(): Promise<boolean> {
  const data = await gqlClient.query(allWorkspacesQuery, { first: 500 });
  workspaces = data.workspaces.edges.map((e) => ({ ...e.node, editable: true }));

  if (workspaces.length > 0) {
    const previousWsId = storage.getOrNull();
    if (previousWsId != null) {
      currentWorkspace = workspaces.find((it) => it.id === previousWsId) || null;

      if (!currentWorkspace) {
        try {
          const wsData = await gqlClient.query(workspaceByIdQuery, { id: previousWsId });
          currentWorkspace = { ...wsData.workspace, editable: false };
        } catch {
          currentWorkspace = null;
        }
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
  return useWorkspaces()
    .loadWorkspaces();
}

