let _apiStore = {
  namespaced: true,

  state: {
     jwtToken: null
  },

  mutations: {
    updateJwtToken(state, token) {
      state.jwtToken = token
    }
  },

  actions: {

  }
}

export default _apiStore
export const store = _apiStore
