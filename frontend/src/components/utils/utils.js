import { MessageBox } from 'element-ui';
import { ref } from '@vue/composition-api';
import Vue from 'vue';

export function findByIdOrEmpty(list, targetItemId) {
  const result = list
    .find((it) => (it.id === targetItemId) || (it.id == null && targetItemId == null));
  return result || {};
}

export function useLoading() {
  const loading = ref(false);

  const withLoading = async (closure) => {
    loading.value = true;
    try {
      await closure();
    } finally {
      loading.value = false;
    }
  };

  const withLoadingProducer = (closure) => () => withLoading(closure);

  return {
    loading,
    withLoading,
    withLoadingProducer,
  };
}

export function safeAssign(target, source) {
  Object.keys(source)
    .forEach((key) => Vue.set(target, key, source[key]));
}

export function useForm(submitAction) {
  const form = ref(null);

  const submitForm = async () => {
    try {
      await form.value.validate();
    } catch (e) {
      return;
    }

    await submitAction();
  };

  return {
    form,
    submitForm,
  };
}

export function useConfirmation() {
  const executeAfterConfirmation = async (message, options, closure) => {
    try {
      await MessageBox.confirm(message, options);
    } catch (e) {
      return;
    }
    await closure();
  };

  return {
    executeAfterConfirmation,
  };
}

export const ID_ROUTER_PARAM_PROCESSOR = (route) => ({ id: Number(route.params.id) });
