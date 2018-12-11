export const withMediumDateTimeFormatter = {
  created: function () {
    this.ensureMediumDateTimeFormatter()
  },

  computed: {
    mediumDateTimeFormatter: function () {
      return this.$store.state.i18n.mediumDateTimeFormatter
    }
  },

  methods: {
    ensureMediumDateTimeFormatter: function () {
      this.$store.dispatch('i18n/ensureMediumDateTimeFormatter')
    }
  }
}

export default withMediumDateTimeFormatter