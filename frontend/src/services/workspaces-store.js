import api from '@/services/api'
import {lockr} from '@/services/app-services'
import {isNil} from 'lodash'

let _workspacesStore = {
  namespaced: true,

  state: {
    workspaces: null,
    currentWorkspace: null
  },

  mutations: {
    setCurrentWorkspace(state, ws) {
      state.currentWorkspace = ws
      lockr.set('current-workspace', ws.id)
    },

    setWorkspaces(state, payload) {
      state.workspaces = payload
    },

    addWorkspace(state, ws) {
      state.workspaces.push(ws)
    }
  },

  actions: {
    createWorkspace({commit}, workspace) {
      commit('addWorkspace', workspace)
      commit('setCurrentWorkspace', workspace)
    },

    async loadWorkspaces({state, commit}) {
      let workspacesResponse = await api.get('/workspaces')
      let workspaces = workspacesResponse.data
      commit('setWorkspaces', workspaces)

      let currentWs;
      if (workspaces.length > 0) {
        let previousWsId = lockr.get('current-workspace')
        if (!isNil(previousWsId)) {
          currentWs = workspaces.find(it => it.id === previousWsId)

          if (!currentWs) {
            let sharedWorkspacesResponse = await api.get('/shared-workspaces')
            let sharedWorkspaces = sharedWorkspacesResponse.data

            currentWs = sharedWorkspaces.find(it => it.id === previousWsId)
          }
        }

        if (!currentWs) {
          currentWs = workspaces[0]
        }

        commit('setCurrentWorkspace', currentWs)
      }
    }
  }
}

export default _workspacesStore
export const workspacesStore = _workspacesStore
