<template>
  <SaDocumentsUpload
    ref="documentsUploadRef"
    :documents="documents"
    :loading-on-create="loadingOnCreate"
    @update:documents-ids="onDocumentsIdsUpdate"
    @uploads-completed="onUploadsCompleted"
    @uploads-failed="onUploadsFailed"
  />
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, ref } from 'vue';
  import { $t } from '@/services/i18n';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
  import { type DocumentDataFragmentType } from '@/components/documents/documents-gql-types';
  import { useSaFormComponentsApi } from '@/components/form/sa-form-components-api.ts';

  const props = defineProps<{
    prop: string,
    documents: ReadonlyArray<DocumentDataFragmentType>,
    loadingOnCreate?: boolean,
  }>();

  const formApi = useSaFormComponentsApi();
  const documentsUploadRef = ref<InstanceType<typeof SaDocumentsUpload> | null>(null);

  let resolveUpload: (() => void) | null = null;
  let rejectUpload: ((err: Error) => void) | null = null;

  const onDocumentsIdsUpdate = (ids: number[]) => {
    (formApi.formValues.value as Record<string, unknown>)[props.prop] = ids;
  };

  const onUploadsCompleted = () => {
    resolveUpload?.();
    resolveUpload = null;
    rejectUpload = null;
  };

  const onUploadsFailed = () => {
    rejectUpload?.(new Error($t.value.useDocumentsUpload.documentsUploadFailure()));
    resolveUpload = null;
    rejectUpload = null;
  };

  const submitUploads = (): Promise<void> => {
    if (!documentsUploadRef.value) {
      return Promise.resolve();
    }
    return new Promise((resolve, reject) => {
      resolveUpload = resolve;
      rejectUpload = reject;
      documentsUploadRef.value!.submitUploads();
    });
  };

  onMounted(() => {
    formApi.registerDocumentsUpload(props.prop, submitUploads);
  });

  onUnmounted(() => {
    formApi.unregisterDocumentsUpload(props.prop);
  });
</script>
