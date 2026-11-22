import {
  inject, InjectionKey, provide, Ref,
} from 'vue';
import { FormItemContext } from 'element-plus';
import { FieldError } from '@/services/api/api-errors.ts';

export type FormValues = Exclude<object, Function | null>; // not a primitive and not a function

export type AdaptApiFieldError = (error: FieldError) => FieldError;

export type SaFormItemRegistration = {
  context: FormItemContext;
  validate: () => string | undefined;
  adaptApiError?: AdaptApiFieldError;
};

export type SaFormComponentsApi = {
  registerFormItem: (prop: string, item: SaFormItemRegistration) => void;
  unregisterFormItem: (prop: string) => void;
  formValues: Ref<FormValues>;
  submitForm: () => Promise<void>;
  loading: Ref<boolean>;
  registerDocumentsUpload: (id: string, submitFn: () => Promise<void>) => void;
  unregisterDocumentsUpload: (id: string) => void;
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
