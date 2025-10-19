import {
  inject, InjectionKey, provide, Ref,
} from 'vue';
import { FormItemContext } from 'element-plus';

export type FormValues = Exclude<object, Function | null>; // not a primitive and not a function

export type SaFormComponentsApi = {
  registerFormItem: (prop: string, item: FormItemContext) => void;
  unregisterFormItem: (prop: string) => void;
  formValues: Ref<FormValues>;
  submitForm?: () => Promise<void>;
}

const SaFormComponentsApiKey = Symbol('SaFormComponentsApi') as InjectionKey<SaFormComponentsApi>;

export const provideSaFormComponentsApi = (api: SaFormComponentsApi) => {
  provide(SaFormComponentsApiKey, api);
};

export const useSaFormComponentsApi = (): SaFormComponentsApi => {
  const api = inject(SaFormComponentsApiKey);
  if (!api) {
    throw new Error('SaFormComponentsApi not provided. Make sure to call '
      + 'useSaFormComponentsApi inside a component nested in SaForm.');
  }
  return api;
};
