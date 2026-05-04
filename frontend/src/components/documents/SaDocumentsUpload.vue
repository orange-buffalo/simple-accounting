<template>
  <div class="sa-documents-upload">
    <div
      v-if="uiState.initialLoading"
      class="sa-documents-upload__loading-placeholder"
    />

    <SaFailedDocumentsStorageMessage
      v-else-if="!uiState.storageActive"
    />

    <SaDocumentUpload
      v-for="documentAggregate in documentsAggregates"
      v-else
      :key="documentAggregate.key"
      ref="uploadControls"
      :document-id="documentAggregate.document.id"
      :document-name="documentAggregate.document.name"
      :document-size-in-bytes="documentAggregate.document.sizeInBytes ?? undefined"
      class="sa-documents-upload__document"
      @upload-completed="documentAggregate.onUploadComplete($event)"
      @upload-failed="documentAggregate.onUploadFailure()"
      @document-selected="documentAggregate.onFileSelection()"
      @document-removed="onDocumentRemoval(documentAggregate.key)"
    />
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import SaDocumentUpload, { type UploadedDocumentDto } from '@/components/documents/SaDocumentUpload.vue';
  import SaFailedDocumentsStorageMessage from '@/components/documents/storage/SaFailedDocumentsStorageMessage.vue';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useQuery } from '@/services/api/use-gql-api';
  import { useFragment } from '@/services/api/gql/fragment-masking';
  import { DocumentDataFragment, type DocumentDataFragmentType } from '@/components/documents/documents-gql-types';

  type DocumentAggregateDocument = {
    id?: number;
    name?: string;
    sizeInBytes?: number | null;
  };

  type DocumentAggregateState = 'empty' | 'pending' | 'upload-failed' | 'upload-completed';

  class DocumentAggregate {
    public document!: DocumentAggregateDocument;

    public state!: DocumentAggregateState;

    public key: string;

    private readonly onDocumentAggregateChange: () => void;

    constructor(
      onChange: () => void,
      document?: DocumentAggregateDocument,
    ) {
      this.document = document || {};
      this.key = document?.id ? document.id.toString() : Math.random()
        .toString(36)
        .slice(2);
      this.state = document?.id ? 'upload-completed' : 'empty';
      this.onDocumentAggregateChange = onChange;
    }

    onUploadComplete(document: UploadedDocumentDto) {
      this.document = document;
      this.state = 'upload-completed';
      this.onDocumentAggregateChange();
    }

    onUploadFailure() {
      this.state = 'upload-failed';
      this.onDocumentAggregateChange();
    }

    onFileSelection() {
      this.state = 'pending';
      this.onDocumentAggregateChange();
    }
  }

  const props = defineProps<{
    documents: ReadonlyArray<DocumentDataFragmentType>,
    loadingOnCreate?: boolean
  }>();

  const emit = defineEmits<{
    (e: 'uploads-failed'): void,
    (e: 'update:documentsIds', documentIds: number[]): void,
    (e: 'uploads-completed'): void,
  }>();

  const [storageQueryLoading, storageStatus] = useQuery(graphql(/* GraphQL */ `
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

  const uploadStorageActive = computed(
    () => storageStatus.value?.active ?? false,
  );

  const downloadStoragesLoading = ref(false);
  const downloadStoragesLoaded = ref(false);
  const downloadStorageIds = ref(new Set<string>());

  const loadDownloadStorages = async () => {
    if (downloadStoragesLoading.value || downloadStoragesLoaded.value) {
      return;
    }

    downloadStoragesLoading.value = true;
    try {
      const downloadStorages = await loadDownloadStoragesQuery({});
      downloadStorageIds.value = new Set(downloadStorages.map((s) => s.id));
      downloadStoragesLoaded.value = true;
    } finally {
      downloadStoragesLoading.value = false;
    }
  };

  const documentsAggregates = ref<DocumentAggregate[]>([]);
  const hasUnsupportedStorages = computed(() => {
    if (
      storageQueryLoading.value
      || downloadStoragesLoading.value
      || !downloadStoragesLoaded.value
      || props.documents.length === 0
    ) {
      return false;
    }

    const resolved = props.documents.map((d) => useFragment(DocumentDataFragment, d));
    return resolved.some(
      (doc) => !downloadStorageIds.value.has(doc.storageId),
    );
  });

  const addEmptyDocumentAggregateIfNecessary = () => {
    const emptyUpload = documentsAggregates.value
      .find((it) => it.state === 'empty');
    if (!emptyUpload) {
      // eslint-disable-next-line no-use-before-define
      documentsAggregates.value.push(new DocumentAggregate(onDocumentAggregateChange));
    }
  };

  const onDocumentRemoval = (documentAggregateKey: string) => {
    documentsAggregates.value = documentsAggregates.value.filter((it) => it.key !== documentAggregateKey);
  };

  let pendingSubmit: {
    resolve: () => void,
    reject: (error: Error) => void,
  } | undefined;

  const resolvePendingSubmit = () => {
    if (pendingSubmit) {
      pendingSubmit.resolve();
      pendingSubmit = undefined;
    }
  };

  const rejectPendingSubmit = (error: Error) => {
    if (pendingSubmit) {
      pendingSubmit.reject(error);
      pendingSubmit = undefined;
    }
  };

  const onDocumentAggregateChange = () => {
    addEmptyDocumentAggregateIfNecessary();

    const pendingUpload = documentsAggregates.value
      .find((it) => it.state === 'pending');
    if (pendingUpload) {
      return;
    }

    const failedUpload = documentsAggregates.value
      .find((it) => it.state === 'upload-failed');
    if (failedUpload) {
      emit('uploads-failed');
      rejectPendingSubmit(new Error('Documents upload failed'));
      return;
    }

    const documentsIds = documentsAggregates.value
      .filter((it) => it.state === 'upload-completed')
      .map((it) => {
        if (it.document.id === undefined) throw new Error('Inconsistent state');
        return it.document.id;
      });
    emit('update:documentsIds', documentsIds);
    emit('uploads-completed');
    resolvePendingSubmit();
  };

  const uploadControls = ref<Array<typeof SaDocumentUpload> | undefined>(undefined);
  const submitUploads = async () => {
    if (pendingSubmit) {
      return;
    }

    const submitPromise = new Promise<void>((resolve, reject) => {
      pendingSubmit = { resolve, reject };
    });

    if (uploadControls.value !== undefined) {
      await Promise.all(uploadControls.value.map((upload) => upload.submitUpload()));
    }
    onDocumentAggregateChange();
    await submitPromise;
  };
  defineExpose({
    submitUploads,
  });

  watch(() => props.documents, () => {
    if (props.documents.length) {
      const resolved = props.documents.map((d) => useFragment(DocumentDataFragment, d));
      documentsAggregates.value = [...resolved]
        .sort((a, b) => a.name.localeCompare(b.name))
        .map((document) => new DocumentAggregate(
          onDocumentAggregateChange,
          document,
        ));
      addEmptyDocumentAggregateIfNecessary();
    } else {
      documentsAggregates.value = [];
    }
    addEmptyDocumentAggregateIfNecessary();
  }, { immediate: true });

  const documentsReassigned = ref(false);
  watch(() => props.documents, () => {
    documentsReassigned.value = true;
  }, { immediate: false });

  watch([() => props.documents.length, uploadStorageActive], ([documentsCount, storageActive]) => {
    if (documentsCount > 0 && storageActive) {
      loadDownloadStorages();
    }
  }, { immediate: true });

  const checkingDownloadStorages = computed(
    () => props.documents.length > 0
      && uploadStorageActive.value
      && (downloadStoragesLoading.value || !downloadStoragesLoaded.value),
  );

  const uiState = computed(() => {
    const state = {
      initialLoading: false,
      storageActive: true,
    };

    if (storageQueryLoading.value || checkingDownloadStorages.value) {
      state.initialLoading = true;
    } else if (!uploadStorageActive.value) {
      state.storageActive = false;
    } else if (hasUnsupportedStorages.value) {
      state.storageActive = false;
    } else if (props.loadingOnCreate && !props.documents.length && !documentsReassigned.value) {
      state.initialLoading = true;
    }

    return state;
  });
</script>

<style lang="scss">
  @use "@/styles/mixins.scss" as *;

  .sa-documents-upload {
    &__document {
      margin-bottom: 15px;
      width: 100%;
    }

    &__loading-placeholder {
      height: 80px;
      @include loading-placeholder;
    }
  }
</style>
