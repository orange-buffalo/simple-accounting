import {
  inject, InjectionKey, provide, Ref,
} from 'vue';

export type SaFormItemComponentsApi = {
  formItemValue: Ref<unknown | null>;
}

const SaFormItemComponentsApiKey = Symbol('SaFormItemComponentsApi') as InjectionKey<SaFormItemComponentsApi>;

export const provideSaFormItemComponentsApi = (api: SaFormItemComponentsApi) => {
  provide(SaFormItemComponentsApiKey, api);
};

export const useSaFormItemComponentsApi = (): SaFormItemComponentsApi => {
  const api = inject(SaFormItemComponentsApiKey);
  if (!api) {
    throw new Error('SaFormItemComponentsApi not provided. Make sure to call '
      + 'useSaFormItemComponentsApi inside a component nested in SaFormItem.');
  }
  return api;
};
