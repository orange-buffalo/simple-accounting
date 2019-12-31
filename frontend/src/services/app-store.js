export const appStore = {
  namespaced: true,

  state: {
    currencies: [],
    lastView: null,
  },

  mutations: {
    setLastView(state, view) {
      state.lastView = view;
    },
  },
};

export default appStore;
