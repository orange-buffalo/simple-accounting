import api from '@/services/api'

let _workspacesStore = {
  namespaced: true,

  state: {
    workspaces: null,
    currentWorkspace: null
  },

  mutations: {
    setWorkspaces(state, payload) {
      state.workspaces = payload
      if (payload.length > 0) {
        state.currentWorkspace = payload[0]
      }
    },

    createWorkspace(state, workspace) {
      state.workspaces.push(workspace)
      state.currentWorkspace = workspace
    }
  },

  actions: {
    loadWorkspaces({commit, state}) {
      return new Promise(resolve => {
        if (state.workspaces) {
          resolve()
        }
        else {
          api.get('/user/workspaces').then(response => {
            commit('setWorkspaces', response.data)
            resolve()
          })
        }
      })
    }
  }
}

export default _workspacesStore
export const workspacesStore = _workspacesStore
