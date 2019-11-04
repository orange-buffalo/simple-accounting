import { mapState } from 'vuex';

const withWorkspaces = {
  computed: {
    ...mapState('workspaces', {
      currentWorkspace: 'currentWorkspace',
      workspaces: 'workspaces',
    }),

    defaultCurrency() {
      return this.currentWorkspace.defaultCurrency;
    },
  },
};

export default withWorkspaces;
