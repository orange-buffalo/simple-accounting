import {mapState} from 'vuex'

export const withWorkspaces = {
  computed: {
    ...mapState('workspaces', {
      currentWorkspace: 'currentWorkspace',
      workspaces: 'workspaces'
    }),

    defaultCurrency: function () {
      return this.currentWorkspace.defaultCurrency
    }
  }
}

export default withWorkspaces