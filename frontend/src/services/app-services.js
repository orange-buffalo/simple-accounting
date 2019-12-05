import Lockr from 'lockr';

export function setupApp(store) {
  // todo #97: remove these stores
  store.dispatch('app/loadCurrencies');
  return store.dispatch('workspaces/loadWorkspaces');
}

Lockr.prefix = 'simple-accounting.';
export const lockr = Lockr;
