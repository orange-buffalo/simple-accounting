export const setupApp = function (store) {
  store.dispatch('app/loadCurrencies')
  return store.dispatch('workspaces/loadWorkspaces')
}