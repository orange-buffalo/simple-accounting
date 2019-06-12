import api from '@/services/api'
import {lockr} from '@/app/services/app-services'
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

    loadWorkspaces({state, commit}) {
      return new Promise(resolve => {
        api.get('/user/workspaces').then(response => {
          let workspaces = response.data
          commit('setWorkspaces', workspaces)

          let currentWs;
          if (workspaces.length > 0) {
            let previousWsId = lockr.get('current-workspace')
            if (!isNil(previousWsId)) {
              currentWs = workspaces.find(it => it.id === previousWsId)
            }

            if (!currentWs) {
              currentWs = workspaces[0]
            }

            commit('setCurrentWorkspace', currentWs)
          }

          resolve()
        })
      })
    }
  }
}

export default _workspacesStore
export const workspacesStore = _workspacesStore
