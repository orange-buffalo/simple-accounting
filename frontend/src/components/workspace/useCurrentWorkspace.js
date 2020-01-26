import { app } from '@/services/app-services';

export default function useCurrentWorkspace() {
  const { currentWorkspace } = app.store.state.workspaces;
  const currentWorkspaceId = currentWorkspace && currentWorkspace.id;
  const currentWorkspaceApiUrl = url => `/workspaces/${currentWorkspaceId}/${url}`;
  return {
    currentWorkspace,
    currentWorkspaceId,
    currentWorkspaceApiUrl,
  };
}
