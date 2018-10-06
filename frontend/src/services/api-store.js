import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

let _apiStore = new Vuex.Store({
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
})

export default _apiStore
export const store = _apiStore
