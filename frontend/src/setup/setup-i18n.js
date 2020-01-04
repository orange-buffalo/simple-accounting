import Vue from 'vue';
import VueI18n from 'vue-i18n';
import i18n from '@/services/i18n';

export default function setupI18n() {
  Vue.use(VueI18n);
  return i18n;
}
