import userApi from '@/services/user-api';

export const appStore = {
  namespaced: true,

  state: {
    currencies: [],
    lastView: null,
  },

  mutations: {
    setCurrencies(state, currencies) {
      state.currencies = currencies;
    },

    setLastView(state, view) {
      state.lastView = view;
    },
  },

  actions: {
    loadCurrencies({ commit }) {
      if (this.currenciesRequested) {
        return;
      }
      this.currenciesRequested = true;

      userApi.getCurrencies()
        .then(currencies => commit('setCurrencies', currencies));
    },
  },
};

export default appStore;
