import Vue from 'vue';
import Vuex from 'vuex';
import { appStore } from '@/services/app-store';

export default function setupStore() {
  Vue.use(Vuex);

  return new Vuex.Store({
    modules: {
      app: appStore,
    },
  });
}
