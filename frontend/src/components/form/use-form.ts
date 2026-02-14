import type { FormItemContext } from 'element-plus';
import { ElMessage } from 'element-plus';
import { onMounted, ref } from 'vue';
import type SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
import type SaLegacyForm from '@/components/form/SaLegacyForm.vue';
import { $t } from '@/services/i18n';

function useFormInternal(
  loadFormData: () => Promise<void>,
  saveFormData: () => Promise<void>,
  continueLoadingAfterSubmit: boolean,
) {
  const formRef = ref<typeof SaLegacyForm | undefined>();

  const form = () => {
    if (!formRef.value) throw new Error('Not initialized');
    return formRef.value;
  };

  const startLoading = () => {
    form().startLoading();
  };

  const stopLoading = () => {
    // use might navigate away from the form
    if (formRef.value) {
      formRef.value.stopLoading();
    }
  };

  const submitForm = async () => {
    startLoading();
    try {
      await form().validate();
    } catch (_) {
      stopLoading();
      return;
    }

    try {
      await saveFormData();
    } finally {
      if (!continueLoadingAfterSubmit) {
        stopLoading();
      }
    }
  };

  const executeWithFormBlocked = async (spec: () => Promise<unknown>) => {
    startLoading();
    try {
      await spec();
    } finally {
      stopLoading();
    }
  };

  // ensure ref is populated
  onMounted(async () => {
    await loadFormData();
    stopLoading();
  });

  return {
    formRef,
    submitForm,
    stopLoading,
    executeWithFormBlocked,
  };
}

/**
 * @deprecated Use new from API - onSubmit in SaForm. For legacy forms, use SaLegacyForm.
 */
export function useForm(loadFormData: () => Promise<void>, saveFormData: () => Promise<void>) {
  const { formRef, submitForm, executeWithFormBlocked } = useFormInternal(loadFormData, saveFormData, false);
  return {
    formRef,
    submitForm,
    executeWithFormBlocked,
  };
}

/**
 * @deprecated Use new from API - onSubmit in SaForm. For legacy forms, use SaLegacyForm.
 */
export function useFormWithDocumentsUpload(loadFormData: () => Promise<void>, saveFormData: () => Promise<void>) {
  const documentsUploadRef = ref<typeof SaDocumentsUpload | undefined>();

  const documentsUpload = () => {
    if (!documentsUploadRef.value) throw new Error('Not initialized');
    return documentsUploadRef.value;
  };

  const onFormSubmit = async () => {
    documentsUpload().submitUploads();
  };

  const { formRef, stopLoading, submitForm, executeWithFormBlocked } = useFormInternal(
    loadFormData,
    onFormSubmit,
    true,
  );

  const onDocumentsUploadFailure = () => {
    stopLoading();
    ElMessage({
      showClose: true,
      message: $t.value.useDocumentsUpload.documentsUploadFailure(),
      type: 'error',
    });
  };

  const onDocumentsUploadComplete = async () => {
    try {
      await saveFormData();
    } finally {
      // in case we navigated away from the form page
      if (formRef.value) {
        stopLoading();
      }
    }
  };

  return {
    formRef,
    submitForm,
    documentsUploadRef,
    onDocumentsUploadFailure,
    onDocumentsUploadComplete,
    executeWithFormBlocked,
  };
}

/**
 * Takes care about the boilerplate related to setting custom validation errors (e.g. from API response)
 * on a particular form item. See [docs](https://element-plus.org/en-US/component/form.html#formitem-exposes).
 * To use:
 *  - add `const myFieldValidation = useFormItemValidation();`
 *  - add `:ref="myFieldValidation.formItem"` to the form item
 *  - call `myFieldValidation.setValidationError('My error message');` to set the error
 *  - call `myFieldValidation.resetErrors();` to reset the error, e.g. in `@keyup` handler
 * @deprecated Use new from API - onSubmit in SaForm. For legacy forms, use SaLegacyForm.
 */
export function useFormItemValidation() {
  const formItem = ref<FormItemContext | null>(null);

  const resetErrors = () => {
    if (formItem.value && formItem.value.validateState === 'error') {
      formItem.value.validateState = '';
      // @ts-ignore
      formItem.value.validateMessage = '';
    }
  };

  const setValidationError = (validationMessage: string) => {
    if (formItem.value) {
      formItem.value.validateState = 'error';
      // @ts-ignore
      formItem.value.validateMessage = validationMessage;
    }
  };

  return {
    formItem,
    resetErrors,
    setValidationError,
  };
}
