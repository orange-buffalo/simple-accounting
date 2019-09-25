import jwtDecode from 'jwt-decode'

let _apiStore = {
  namespaced: true,

  state: {
    jwtToken: null,
    isAdmin: false
  },

  mutations: {
    updateJwtToken(state, token) {
      state.jwtToken = token
      state.isAdmin = false
      if (token) {
        let decodedToken = jwtDecode(token)
        state.isAdmin = decodedToken.roles && decodedToken.roles.indexOf("ADMIN") >= 0
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
