import jwtDecode from 'jwt-decode'

let _apiStore = {
  namespaced: true,

  state: {
    jwtToken: null,
    isAdmin: false,
    isTransient: false
  },

  mutations: {
    updateJwtToken(state, token) {
      state.jwtToken = token
      state.isAdmin = false
      if (token) {
        let decodedToken = jwtDecode(token)
        state.isAdmin = decodedToken.roles && decodedToken.roles.indexOf("ADMIN") >= 0
        state.isTransient = decodedToken.transient
      }
    }
  },

  getters: {
    isLoggedIn: state => {
      return state.jwtToken != null
    }
  }
}

export default _apiStore
export const store = _apiStore
