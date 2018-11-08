import userApi from '@/app/services/user-api'
import Cldr from 'cldrjs'

let data = require("./i18n/en-AU.cldr-data")
Cldr.load(data.default)
var en = new Cldr("en-AU");
console.log(en.attributes)
console.log(en.get('/main/{bundle}/numbers/currencies/AUD'))

var Globalize = require('globalize')
Globalize.load(data.default)

console.log(Globalize("en-AU").formatDate(new Date()))
console.log(Globalize("en-AU").currencyFormatter('AUD')(3345.6))
console.log(Globalize("en-AU").currencyFormatter('EUR')(3345.6))
console.log(Globalize("en-AU").currencyFormatter('USD')(3345.6))

//

data = require("./i18n/en.cldr-data")
Cldr.load(data.default)
en = new Cldr("en");
console.log(en.attributes)
console.log(en.get('/main/{bundle}/numbers/currencies/AUD'))

Globalize.load(data.default)

console.log(Globalize("en").formatDate(new Date()))
console.log(Globalize("en").currencyFormatter('AUD')(3345.6))
console.log(Globalize("en").currencyFormatter('EUR')(3345.6))
console.log(Globalize("en").currencyFormatter('USD')(3345.6))

export const i18nStore = {
  namespaced: true,

  state: {},

  mutations: {},

  actions: {}
}

export default i18nStore
