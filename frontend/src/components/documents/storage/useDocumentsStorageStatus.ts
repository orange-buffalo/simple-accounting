import {
  computed, inject, onBeforeUnmount, provide, ref, type ComputedRef, type InjectionKey,
} from 'vue';
import { graphql } from '@/services/api/gql';
import { useLazyQuery } from '@/services/api/use-gql-api.ts';

interface DocumentStorageStatusState {
  readonly loading: boolean,
  readonly loaded: boolean,
  readonly active: boolean,
}

interface DownloadDocumentStoragesState {
  readonly loading: boolean,
  readonly loaded: boolean,
  readonly ids: ReadonlySet<string>,
}

interface DocumentsStorageStatusProvider {
  readonly uploadStorageStatus: ComputedRef<DocumentStorageStatusState>,
  readonly downloadStoragesStatus: ComputedRef<DownloadDocumentStoragesState>,
  ensureUploadStorageStatusLoaded: () => Promise<void>,
  ensureDownloadStoragesLoaded: () => Promise<void>,
  resetOnNextUse: () => void,
}

const DocumentsStorageStatusProviderKey = Symbol('DocumentsStorageStatusProvider') as
  InjectionKey<DocumentsStorageStatusProvider>;

let activeProvider: DocumentsStorageStatusProvider | undefined;

function requireDocumentsStorageStatusProvider(): DocumentsStorageStatusProvider {
  const provider = inject(DocumentsStorageStatusProviderKey);
  if (!provider) {
    throw new Error('Documents storage status provider is not available');
  }
  return provider;
}

function createDocumentsStorageStatusProvider(): DocumentsStorageStatusProvider {
  const loadUploadStorageStatusQuery = useLazyQuery(graphql(/* GraphQL */ `
    query documentsStorageStatus {
      documentsStorageStatus {
        active
      }
    }
  `), 'documentsStorageStatus');

  const loadDownloadStoragesQuery = useLazyQuery(graphql(/* GraphQL */ `
    query downloadDocumentStorages {
      getDownloadDocumentStorages {
        id
      }
    }
  `), 'getDownloadDocumentStorages');

  const generation = ref(0);
  let resetPending = false;

  const uploadStorageLoading = ref(false);
  const uploadStorageLoaded = ref(false);
  const uploadStorageActive = ref(false);
  let uploadStorageLoadingPromise: Promise<void> | undefined;

  const downloadStoragesLoading = ref(false);
  const downloadStoragesLoaded = ref(false);
  const downloadStorageIds = ref<ReadonlySet<string>>(new Set());
  let downloadStoragesLoadingPromise: Promise<void> | undefined;

  const uploadStorageStatus = computed<DocumentStorageStatusState>(() => ({
    loading: uploadStorageLoading.value,
    loaded: uploadStorageLoaded.value,
    active: uploadStorageActive.value,
  }));

  const downloadStoragesStatus = computed<DownloadDocumentStoragesState>(() => ({
    loading: downloadStoragesLoading.value,
    loaded: downloadStoragesLoaded.value,
    ids: downloadStorageIds.value,
  }));

  const reset = () => {
    generation.value += 1;

    uploadStorageLoading.value = false;
    uploadStorageLoaded.value = false;
    uploadStorageActive.value = false;
    uploadStorageLoadingPromise = undefined;

    downloadStoragesLoading.value = false;
    downloadStoragesLoaded.value = false;
    downloadStorageIds.value = new Set();
    downloadStoragesLoadingPromise = undefined;
  };

  const resetIfPending = () => {
    if (resetPending) {
      resetPending = false;
      reset();
    }
  };

  const ensureUploadStorageStatusLoaded = async () => {
    resetIfPending();

    if (uploadStorageLoaded.value) {
      return;
    }

    if (!uploadStorageLoadingPromise) {
      const loadingGeneration = generation.value;
      uploadStorageLoading.value = true;
      uploadStorageLoadingPromise = loadUploadStorageStatusQuery({})
        .then((storageStatus) => {
          if (generation.value === loadingGeneration) {
            uploadStorageActive.value = storageStatus.active;
            uploadStorageLoaded.value = true;
          }
        })
        .catch((error: unknown) => {
          if (generation.value === loadingGeneration) {
            uploadStorageActive.value = false;
            uploadStorageLoaded.value = true;
          }
          throw error;
        })
        .finally(() => {
          if (generation.value === loadingGeneration) {
            uploadStorageLoading.value = false;
            uploadStorageLoadingPromise = undefined;
          }
        });
    }

    await uploadStorageLoadingPromise;
  };

  const ensureDownloadStoragesLoaded = async () => {
    resetIfPending();

    if (downloadStoragesLoaded.value) {
      return;
    }

    if (!downloadStoragesLoadingPromise) {
      const loadingGeneration = generation.value;
      downloadStoragesLoading.value = true;
      downloadStoragesLoadingPromise = loadDownloadStoragesQuery({})
        .then((downloadStorages) => {
          if (generation.value === loadingGeneration) {
            downloadStorageIds.value = new Set(downloadStorages.map((storage) => storage.id));
            downloadStoragesLoaded.value = true;
          }
        })
        .catch((error: unknown) => {
          if (generation.value === loadingGeneration) {
            downloadStorageIds.value = new Set();
            downloadStoragesLoaded.value = true;
          }
          throw error;
        })
        .finally(() => {
          if (generation.value === loadingGeneration) {
            downloadStoragesLoading.value = false;
            downloadStoragesLoadingPromise = undefined;
          }
        });
    }

    await downloadStoragesLoadingPromise;
  };

  return {
    uploadStorageStatus,
    downloadStoragesStatus,
    ensureUploadStorageStatusLoaded,
    ensureDownloadStoragesLoaded,
    resetOnNextUse: () => {
      resetPending = true;
    },
  };
}

export function provideDocumentsStorageStatus() {
  const provider = createDocumentsStorageStatusProvider();
  provide(DocumentsStorageStatusProviderKey, provider);
  activeProvider = provider;

  onBeforeUnmount(() => {
    if (activeProvider === provider) {
      activeProvider = undefined;
    }
  });
}

export function resetDocumentsStorageStatus() {
  activeProvider?.resetOnNextUse();
}

export function useDocumentsUploadStorageStatus() {
  const provider = requireDocumentsStorageStatusProvider();
  provider.ensureUploadStorageStatusLoaded();

  return {
    documentsStorageStatus: provider.uploadStorageStatus,
  };
}

export function useDownloadDocumentStoragesStatus(loadImmediately = true) {
  const provider = requireDocumentsStorageStatusProvider();
  if (loadImmediately) {
    provider.ensureDownloadStoragesLoaded();
  }

  return {
    downloadStoragesStatus: provider.downloadStoragesStatus,
    ensureDownloadStoragesLoaded: provider.ensureDownloadStoragesLoaded,
  };
}

export default useDocumentsUploadStorageStatus;
