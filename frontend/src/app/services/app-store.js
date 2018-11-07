import userApi from '@/app/services/user-api'

export const appStore = {
  namespaced: true,

  state: {
    currencies: []
  },

  mutations: {
    setCurrencies(state, currencies) {
      state.currencies = currencies
    }
  },

  actions: {
    loadCurrencies({commit}) {
      if (this.currenciesRequested) {
        return
      }
      this.currenciesRequested = true

      userApi.getCurrencies()
          .then(currencies => commit('setCurrencies', currencies))
    }
  }
}

export default appStore
