import {mapState} from 'vuex'

export const withApi = {
  computed: {
    ...mapState({
      isCurrentUserTransient: state => state.api.isTransient
    }),

    isCurrentUserRegular: function () {
      return !this.isCurrentUserTransient
    }
  }
}

export default withApi