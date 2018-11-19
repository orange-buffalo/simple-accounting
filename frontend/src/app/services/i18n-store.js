let globalize
let cldr

export const i18nStore = {
  namespaced: true,

  state: {
    currencyFormatters: {},
    mediumDateFormatter: null,
    currencyInfo: []
  },

  mutations: {},

  actions: {
    loadLocaleData: function ({state}) {
      //todo move to a separate js, can be loaded without splitting
      // todo based on current locale
      import('cldrjs').then(cldrjs => {
        import('globalize').then(globalizejs => {
          import(/* webpackChunkName: "i18n" */ './i18n/en-AU.cldr-data').then(module => {
            cldrjs.default.load(module.default)
            globalizejs.default.load(module.default)
            cldr = new cldrjs.default("en-AU");
            globalize = globalizejs.default("en-AU");

            // todo re-create formatters and infos in new locale

            state.currencyInfo = cldr.get(`/main/{bundle}/numbers/currencies`)
          });
        });
      })
    },

    ensureCurrencyFormatter: function ({state}, currency) {
      if (!state.currencyFormatters[currency]) {
        state.currencyFormatters[currency] = globalize
            ? globalize.currencyFormatter(currency)
            : () => ''
      }
    },

    ensureMediumDateFormatter: function ({state}) {
      if (!state.mediumDateFormatter) {
        state.mediumDateFormatter = globalize
            ? globalize.dateFormatter({date: 'medium'})
            : () => ''
      }
    }
  },

  getters: {
    getCurrencyInfo: state => currency => {
      let currencyInfo = state.currencyInfo[currency]
      return currencyInfo ? currencyInfo : {}
    },

    getCurrencyFormatter: state => currency => {
      return state.currencyFormatters[currency]
    }
  }
}

export default i18nStore
