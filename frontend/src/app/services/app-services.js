import Lockr from 'lockr'

export const setupApp = function (store) {
  store.dispatch('app/loadCurrencies')
  return store.dispatch('workspaces/loadWorkspaces')
}

Lockr.prefix = 'simple-accounting'
export const lockr = Lockr