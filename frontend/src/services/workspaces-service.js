import { app } from '@/services/app-services';

// todo #6: move to default export?
export async function initWorkspace() {
  // todo #97: remove these stores
  await app.store.dispatch('app/loadCurrencies');
  await app.store.dispatch('workspaces/loadWorkspaces');
  // todo #6 remove
  await app.store.dispatch('i18n/loadLocaleData');
}
