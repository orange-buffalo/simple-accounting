import { ref } from 'vue';
import { profileApi } from '@/services/api';

interface DocumentStorageStatusState {
  readonly loading: boolean;
  readonly active: boolean;
}

export default function useDocumentsStorageStatus() {
  const documentsStorageStatus = ref<DocumentStorageStatusState>({
    loading: true,
    active: false,
  });

  async function loadDocumentsStorageStatus() {
    const storageStatus = await profileApi.getDocumentsStorageStatus();
    documentsStorageStatus.value = {
      loading: false,
      active: storageStatus.active,
    };
  }

  // noinspection JSIgnoredPromiseFromCall
  loadDocumentsStorageStatus();

  return {
    documentsStorageStatus,
  };
}
