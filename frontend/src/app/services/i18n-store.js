import userApi from '@/app/services/user-api'
// import Cldr from 'cldrjs'

// let data = require("./i18n/en-AU.cldr-data")
// Cldr.load(data.default)
// var en = new Cldr("en-AU");
// console.log(en.attributes)
// console.log(en.get('/main/{bundle}/numbers/currencies/AUD'))
//
// var Globalize = require('globalize')
// Globalize.load(data.default)
//
// console.log(Globalize("en-AU").formatDate(new Date()))
// console.log(Globalize("en-AU").currencyFormatter('AUD')(3345.6))
// console.log(Globalize("en-AU").currencyFormatter('EUR')(3345.6))
// console.log(Globalize("en-AU").currencyFormatter('USD')(3345.6))

//

// data = require("./i18n/en.cldr-data")
// Cldr.load(data.default)
// en = new Cldr("en");
// console.log(en.attributes)
// console.log(en.get('/main/{bundle}/numbers/currencies/AUD'))
//
// Globalize.load(data.default)
//
// console.log(Globalize("en").formatDate(new Date()))
// console.log(Globalize("en").currencyFormatter('AUD')(3345.6))
// console.log(Globalize("en").currencyFormatter('EUR')(3345.6))
// console.log(Globalize("en").currencyFormatter('USD')(3345.6))

export const i18nStore = {
  namespaced: true,

  state: {
    cldr: null,
    globalize: null
  },

  mutations: {},

  actions: {
    loadLocaleData: function ({state}) {
      //todo move to a separate js, can be loaded without splitting
      // todo based on current locale
      import('cldrjs').then(cldr => {
        import('globalize').then(globalize => {
          import(/* webpackChunkName: "i18n" */ './i18n/en-AU.cldr-data').then(module => {
            cldr.default.load(module.default)
            globalize.default.load(module.default)
            state.cldr = new cldr.default("en-AU");
            state.globalize = globalize.default("en-AU");
          });
        });
      })
    }
  },

  getters: {
    getCurrencyInfo: state => currency => {
      let currencyInfo = state.cldr ? state.cldr.get(`/main/{bundle}/numbers/currencies/${currency}`) : {}
      return currencyInfo ? currencyInfo : {}
    },

    getCurrencyFormatter: state => currency => {
      return state.globalize
          ? state.globalize.currencyFormatter(currency)
          : () => ''
    },

    getMediumDateFormatter: state => {
      return state.globalize
          ? state.globalize.dateFormatter({date: 'medium'})
          : () => ''
    }
  }
}

export default i18nStore
