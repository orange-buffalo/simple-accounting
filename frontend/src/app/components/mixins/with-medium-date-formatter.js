export const withMediumDateFormatter = {
  created: function () {
    this.ensureMediumDateFormatter()
  },

  computed: {
    mediumDateFormatter: function () {
      return this.$store.state.i18n.mediumDateFormatter
    }
  },

  methods: {
    ensureMediumDateFormatter: function () {
      this.$store.dispatch('i18n/ensureMediumDateFormatter')
    }
  }
}

export default withMediumDateFormatter