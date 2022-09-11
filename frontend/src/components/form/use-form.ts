import { ref } from 'vue';
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
    form()
      .stopLoading();
  };

  const submitForm = async () => {
    startLoading();
    try {
      if (await form()
        .validate()) {
        await saveFormData();
      } else {
        stopLoading();
      }
    } finally {
      if (!continueLoadingAfterSubmit) {
        stopLoading();
      }
    }
  };

  loadFormData()
    .then(stopLoading);

  return {
    formRef,
    submitForm,
    stopLoading,
  };
}

export function useForm(
  loadFormData: () => Promise<void>,
  saveFormData: () => Promise<void>,
) {
  const {
    formRef,
    submitForm,
  } = useFormInternal(loadFormData, saveFormData, false);
  return {
    formRef,
    submitForm,
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
      stopLoading();
    }
  };

  return {
    formRef,
    submitForm,
    documentsUploadRef,
    onDocumentsUploadFailure,
    onDocumentsUploadComplete,
  };
}
