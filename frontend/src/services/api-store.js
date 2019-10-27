import jwtDecode from 'jwt-decode';

const _apiStore = {
  namespaced: true,

  state: {
    jwtToken: null,
    isAdmin: false,
    isTransient: false,
  },

  mutations: {
    updateJwtToken(state, token) {
      state.jwtToken = token;
      state.isAdmin = false;
      if (token) {
        const decodedToken = jwtDecode(token);
        state.isAdmin = decodedToken.roles && decodedToken.roles.indexOf('ADMIN') >= 0;
        state.isTransient = decodedToken.transient;
      }
    },
  },

  getters: {
    isLoggedIn: state => state.jwtToken != null,
  },
};

export default _apiStore;
export const store = _apiStore;
