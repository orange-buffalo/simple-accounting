import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import type SaForm from '@/components/form/SaForm.vue';
import type SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
import { $t } from '@/services/i18n';

function useFormInternal(
  loadFormData: () => Promise<void>,
  saveFormData: () => Promise<void>,
  continueLoadingAfterSubmit: boolean,
) {
  const formRef = ref<typeof SaForm | undefined>();

  const form = () => {
    if (!formRef.value) throw new Error('Not initialized');
    return formRef.value;
  };

  const startLoading = () => {
    form()
      .startLoading();
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
      await form()
        .validate();
    } catch (e) {
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

export function useForm(
  loadFormData: () => Promise<void>,
  saveFormData: () => Promise<void>,
) {
  const {
    formRef,
    submitForm,
    executeWithFormBlocked,
  } = useFormInternal(loadFormData, saveFormData, false);
  return {
    formRef,
    submitForm,
    executeWithFormBlocked,
  };
}

export function useFormWithDocumentsUpload(
  loadFormData: () => Promise<void>,
  saveFormData: () => Promise<void>,
) {
  const documentsUploadRef = ref<typeof SaDocumentsUpload | undefined>();

  const documentsUpload = () => {
    if (!documentsUploadRef.value) throw new Error('Not initialized');
    return documentsUploadRef.value;
  };

  const onFormSubmit = async () => {
    documentsUpload()
      .submitUploads();
  };

  const {
    formRef,
    stopLoading,
    submitForm,
    executeWithFormBlocked,
  } = useFormInternal(loadFormData, onFormSubmit, true);

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
