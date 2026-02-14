import { FormItemContext } from 'element-plus';
import { InjectionKey, inject, provide, Ref } from 'vue';

export type FormValues = Exclude<object, Function | null>; // not a primitive and not a function

export type SaFormComponentsApi = {
  registerFormItem: (prop: string, item: FormItemContext) => void;
  unregisterFormItem: (prop: string) => void;
  formValues: Ref<FormValues>;
  submitForm: () => Promise<void>;
  loading: Ref<boolean>;
};

const SaFormComponentsApiKey = Symbol('SaFormComponentsApi') as InjectionKey<SaFormComponentsApi>;

export const provideSaFormComponentsApi = (api: SaFormComponentsApi) => {
  provide(SaFormComponentsApiKey, api);
};

export const useSaFormComponentsApi = (): SaFormComponentsApi => {
  const api = inject(SaFormComponentsApiKey);
  if (!api) {
    throw new Error(
      'SaFormComponentsApi not provided. Make sure to call ' +
        'useSaFormComponentsApi inside a component nested in SaForm.',
    );
  }
  return api;
};
