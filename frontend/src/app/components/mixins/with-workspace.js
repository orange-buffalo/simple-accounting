import {mapState} from 'vuex'

export const withWorkspace = {
  computed: {
    ...mapState('workspaces', {
      currentWorkspace: 'currentWorkspace',
      workspaces: 'workspaces'
    })
  }
}

export default withWorkspace