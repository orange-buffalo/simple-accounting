import { Message } from 'element-ui';
import { ref } from '@vue/composition-api';
import i18n from '@/services/i18n';
import { useForm } from '@/components/utils/utils';

export default function useDocumentsUpload(loading) {
  const onDocumentsUploadFailure = () => {
    // eslint-disable-next-line no-param-reassign
    loading.value = false;
    Message({
      showClose: true,
      message: $t.value.useDocumentsUpload.documentsUploadFailure(),
      type: 'error',
    });
  };

  const documentsUpload = ref(null);

  const { form, submitForm } = useForm(() => {
    // eslint-disable-next-line no-param-reassign
    loading.value = true;
    documentsUpload.value.submitUploads();
  });

  return {
    onDocumentsUploadFailure,
    documentsUpload,
    form,
    submitForm,
  };
}
