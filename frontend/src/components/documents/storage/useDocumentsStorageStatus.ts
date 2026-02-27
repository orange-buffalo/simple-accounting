import { ref } from 'vue';
import { graphql } from '@/services/api/gql';
import { gqlClient } from '@/services/api/gql-api-client.ts';

interface DocumentStorageStatusState {
  readonly loading: boolean,
  readonly active: boolean,
}

export default function useDocumentsStorageStatus() {
  const documentsStorageStatus = ref<DocumentStorageStatusState>({
    loading: true,
    active: false,
  });

  const documentsStorageStatusQuery = graphql(/* GraphQL */ `
    query documentsStorageStatus {
      documentsStorageStatus {
        active
      }
    }
  `);

  async function loadDocumentsStorageStatus() {
    const result = await gqlClient.query(documentsStorageStatusQuery, {});
    documentsStorageStatus.value = {
      loading: false,
      active: result.documentsStorageStatus.active,
    };
  }

  // noinspection JSIgnoredPromiseFromCall
  loadDocumentsStorageStatus();

  return {
    documentsStorageStatus,
  };
}
