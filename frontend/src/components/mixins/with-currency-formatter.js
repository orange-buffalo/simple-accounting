export const withCurrencyFormatter = {
  created: function () {
    this.ensureCurrencyFormatter(this.currency)
  },

  computed: {
    currencyFormatter: function () {
      return this.$store.getters['i18n/getCurrencyFormatter'](this.currency)
    }
  },

  methods: {
    ensureCurrencyFormatter: function (currency) {
      if (currency){
        this.$store.dispatch('i18n/ensureCurrencyFormatter', currency)
      }
    }
  },

  watch: {
    currency: function (val) {
      this.ensureCurrencyFormatter(val)
    }
  }
}

export default withCurrencyFormatter