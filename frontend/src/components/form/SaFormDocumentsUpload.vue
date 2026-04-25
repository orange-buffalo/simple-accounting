<template>
  <SaDocumentsUpload
    ref="documentsUploadRef"
    :documents="documents"
    @update:documents-ids="onDocumentsIdsUpdate"
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
  }>();

  const formApi = useSaFormComponentsApi();
  const documentsUploadRef = ref<InstanceType<typeof SaDocumentsUpload> | null>(null);

  const onDocumentsIdsUpdate = (ids: number[]) => {
    (formApi.formValues.value as Record<string, unknown>)[props.prop] = ids;
  };

  const onUploadsFailed = () => {
    throw new Error($t.value.useDocumentsUpload.documentsUploadFailure());
  };

  const submitUploads = async () => {
    if (documentsUploadRef.value) {
      await documentsUploadRef.value!.submitUploads();
    }  };

  onMounted(() => {
    formApi.registerDocumentsUpload(props.prop, submitUploads);
  });

  onUnmounted(() => {
    formApi.unregisterDocumentsUpload(props.prop);
  });
</script>
