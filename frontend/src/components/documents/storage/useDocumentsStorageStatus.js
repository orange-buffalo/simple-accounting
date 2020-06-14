import { ref } from '@vue/composition-api';
import { api } from '@/services/api';

export default function useDocumentsStorageStatus() {
  const documentsStorageStatus = ref({
    loading: true,
  });

  async function loadDocumentsStorageStatus() {
    const apiResponse = await api.get('/profile/documents-storage');
    documentsStorageStatus.value = {
      loading: false,
      ...apiResponse.data,
    };
  }

  loadDocumentsStorageStatus();

  return {
    documentsStorageStatus,
  };
}
