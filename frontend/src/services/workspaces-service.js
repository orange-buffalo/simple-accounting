import { app } from '@/services/app-services';

// eslint-disable-next-line import/prefer-default-export
export async function initWorkspace() {
  // todo #97: remove this store and method
  await app.store.dispatch('workspaces/loadWorkspaces');
}
