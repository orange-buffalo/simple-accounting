import { computed } from 'vue';
import { graphql } from '@/services/api/gql';
import { useQuery } from '@/services/api/use-gql-api.ts';

interface DocumentStorageStatusState {
  readonly loading: boolean,
  readonly active: boolean,
}

export default function useDocumentsStorageStatus() {
  const [loading, storageStatus] = useQuery(graphql(/* GraphQL */ `
    query documentsStorageStatus {
      documentsStorageStatus {
        active
      }
    }
  `), 'documentsStorageStatus');

  const documentsStorageStatus = computed<DocumentStorageStatusState>(() => ({
    loading: loading.value,
    active: storageStatus.value?.active ?? false,
  }));

  return {
    documentsStorageStatus,
  };
}
