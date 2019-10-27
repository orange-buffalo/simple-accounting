import { mapState } from 'vuex';

export const withWorkspaces = {
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
