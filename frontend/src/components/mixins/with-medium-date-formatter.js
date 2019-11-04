const withMediumDateFormatter = {
  created() {
    this.ensureMediumDateFormatter();
  },

  computed: {
    mediumDateFormatter() {
      return this.$store.state.i18n.mediumDateFormatter;
    },
  },

  methods: {
    ensureMediumDateFormatter() {
      this.$store.dispatch('i18n/ensureMediumDateFormatter');
    },
  },
};

export default withMediumDateFormatter;
