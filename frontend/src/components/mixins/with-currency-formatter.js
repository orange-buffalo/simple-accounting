export const withCurrencyFormatter = {
  created() {
    this.ensureCurrencyFormatter(this.currency);
  },

  computed: {
    currencyFormatter() {
      return this.$store.getters['i18n/getCurrencyFormatter'](this.currency);
    },
  },

  methods: {
    ensureCurrencyFormatter(currency) {
      if (currency) {
        this.$store.dispatch('i18n/ensureCurrencyFormatter', currency);
      }
    },
  },

  watch: {
    currency(val) {
      this.ensureCurrencyFormatter(val);
    },
  },
};

export default withCurrencyFormatter;
