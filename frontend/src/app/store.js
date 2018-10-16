import Vue from 'vue'
import Vuex from 'vuex'
import apiStore from '@/services/api-store'
import workspacesStore from './services/workspaces-store'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    api: apiStore,
    workspaces: workspacesStore
  }
})
