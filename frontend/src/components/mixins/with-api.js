import { mapState } from 'vuex';

export const withApi = {
  computed: {
    ...mapState({
      isCurrentUserTransient: state => state.api.isTransient,
    }),

    isCurrentUserRegular() {
      return !this.isCurrentUserTransient;
    },
  },
};

export default withApi;
