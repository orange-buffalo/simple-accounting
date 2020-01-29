import { Message } from 'element-ui';
import { ref } from '@vue/composition-api';
import i18n from '@/services/i18n';
import { useForm } from '@/components/utils/utils';

export default function useDocumentsUpload() {
  const onDocumentsUploadFailure = () => {
    Message({
      showClose: true,
      message: i18n.t('useDocumentsUpload.documentsUploadFailure'),
      type: 'error',
    });
  };

  const documentsUpload = ref(null);
  const { form, submitForm } = useForm(() => documentsUpload.value.submitUploads());

  return {
    onDocumentsUploadFailure,
    documentsUpload,
    form,
    submitForm,
  };
}
