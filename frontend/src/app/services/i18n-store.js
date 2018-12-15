import {assign} from 'lodash'

let globalize
let cldr

function emptyFormatter() {
  return ''
}

function ensureFormatter(formatter, globalizeFormatterProvider) {
  if (!formatter || formatter === emptyFormatter) {
    return globalize ? globalizeFormatterProvider() : emptyFormatter
  } else {
    return formatter
  }
}

export const i18nStore = {
  namespaced: true,

  state: {
    currencyFormatters: {},
    mediumDateFormatter: null,
    mediumDateTimeFormatter: null,
    currencyInfo: []
  },

  // todo perhaps use mutation instead of direct manipulation with state to have tooling support
  mutations: {},

  actions: {
    loadLocaleData: function ({state, dispatch}) {
      //todo move to a separate js, can be loaded without splitting
      // todo based on current locale
      import('cldrjs').then(cldrjs => {
        import('globalize').then(globalizejs => {
          import('./i18n/en-AU.cldr-data').then(module => {

            cldrjs.default.load(module.default)
            globalizejs.default.load(module.default)
            cldr = new cldrjs.default("en-AU");
            globalize = globalizejs.default("en-AU");

            if (state.mediumDateFormatter) {
              dispatch('ensureMediumDateFormatter')
            }

            if (state.mediumDateTimeFormatter) {
              dispatch('ensureMediumDateTimeFormatter')
            }

            for (let currency in state.currencyFormatters) {
              if (state.currencyFormatters.hasOwnProperty(currency)) {
                dispatch('ensureCurrencyFormatter', currency)
              }
            }

            state.currencyInfo = cldr.get(`/main/{bundle}/numbers/currencies`)
          });
        });
      })
    },

    ensureCurrencyFormatter: function ({state}, currency) {
      let currencyFormatter = {}
      currencyFormatter[currency] = ensureFormatter(
          state.currencyFormatters[currency],
          () => globalize.currencyFormatter(currency)
      )
      state.currencyFormatters = assign({}, state.currencyFormatters, currencyFormatter)
    },

    ensureMediumDateFormatter: function ({state}) {
      state.mediumDateFormatter = ensureFormatter(
          state.mediumDateFormatter,
          () => globalize.dateFormatter({date: 'medium'})
      )
    },

    ensureMediumDateTimeFormatter: function ({state}) {
      state.mediumDateTimeFormatter = ensureFormatter(
          state.mediumDateTimeFormatter,
          () => globalize.dateFormatter({skeleton: "yMMMdhm"})
      )
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
