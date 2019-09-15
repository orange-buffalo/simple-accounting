export const withMediumDateTimeFormatter = {
  created: function () {
    this.ensureMediumDateTimeFormatter()
  },

  computed: {
    mediumDateTimeFormatter: function () {
      return this.$store.state.i18n.mediumDateTimeFormatter
    },

    mediumDateTimeFormatterFromString: function () {
      return (dateTimeIsoString) =>
          this.mediumDateTimeFormatter(new Date(dateTimeIsoString))
    }
  },

  methods: {
    ensureMediumDateTimeFormatter: function () {
      this.$store.dispatch('i18n/ensureMediumDateTimeFormatter')
    }
  }
}

export default withMediumDateTimeFormatter