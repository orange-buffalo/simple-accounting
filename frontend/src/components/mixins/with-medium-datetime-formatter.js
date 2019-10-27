export const withMediumDateTimeFormatter = {
  created() {
    this.ensureMediumDateTimeFormatter();
  },

  computed: {
    mediumDateTimeFormatter() {
      return this.$store.state.i18n.mediumDateTimeFormatter;
    },

    mediumDateTimeFormatterFromString() {
      return dateTimeIsoString => this.mediumDateTimeFormatter(new Date(dateTimeIsoString));
    },
  },

  methods: {
    ensureMediumDateTimeFormatter() {
      this.$store.dispatch('i18n/ensureMediumDateTimeFormatter');
    },
  },
};

export default withMediumDateTimeFormatter;
