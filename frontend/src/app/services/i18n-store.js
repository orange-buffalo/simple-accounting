import userApi from '@/app/services/user-api'
import Cldr from 'cldrjs'

let data = require("./i18n/en-AU.cldr-data")
Cldr.load(data.default)
var en = new Cldr("en-AU");
console.log(en.attributes)
console.log(en.get('/main/{bundle}/numbers/currencies/AUD'))

export const i18nStore = {
  namespaced: true,

  state: {},

  mutations: {},

  actions: {}
}

export default i18nStore
