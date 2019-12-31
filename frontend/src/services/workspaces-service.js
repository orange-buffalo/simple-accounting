import { app } from '@/services/app-services';

// todo #6: move to default export?
export async function initWorkspace() {
  // todo #97: remove these stores
  await app.store.dispatch('workspaces/loadWorkspaces');
}
