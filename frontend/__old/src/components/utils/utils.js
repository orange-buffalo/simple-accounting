import { MessageBox } from 'element-ui';
import { ref } from '@vue/composition-api';
import Vue from 'vue';

export function findByIdOrEmpty(list, targetItemId) {
  const result = list
    .find((it) => (it.id === targetItemId) || (!it.id && !targetItemId));
  return result || {};
}



export function safeAssign(target, source) {
  Object.keys(source)
    .forEach((key) => Vue.set(target, key, source[key]));
}
