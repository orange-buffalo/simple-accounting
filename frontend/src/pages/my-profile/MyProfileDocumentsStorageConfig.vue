<template>
  <SaForm
    v-model="formValues"
    :on-submit="submitStorageConfig"
    :external-loading="props.loading"
    :hide-buttons="true"
  >
    <div class="documents-storage-config" :id="`storage-config_${storageId}`">
      <div class="documents-storage-config__header">
        <ElSwitch
          v-model="formValues.enabled"
        />
        <h4>{{ storageName }}</h4>
      </div>
      <slot v-if="formValues.enabled" />
    </div>
  </SaForm>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { ProfileDto, profileApi } from '@/services/api';
  import SaForm from '@/components/form/SaForm.vue';
  import { useSaFormComponentsApi } from '@/components/form/sa-form-components-api';

  const props = defineProps<{
    storageName: string,
    storageId: string,
    profile: ProfileDto,
    loading: boolean,
  }>();

  type StorageConfigFormValues = {
    enabled: boolean,
  };

  const formValues = ref<StorageConfigFormValues>({
    enabled: props.storageId === props.profile.documentsStorage,
  });

  watch(() => props.profile, () => {
    formValues.value.enabled = props.storageId === props.profile.documentsStorage;
  }, { deep: true });

  // Auto-submit when enabled state changes
  const formApi = useSaFormComponentsApi();
  watch(() => formValues.value.enabled, async (newVal, oldVal) => {
    // Only submit if value actually changed (not initial load)
    if (oldVal !== undefined && newVal !== oldVal && formApi.submitForm) {
      await formApi.submitForm();
    }
  });

  const submitStorageConfig = async () => {
    const updatedProfile: ProfileDto = {
      ...props.profile,
      documentsStorage: formValues.value.enabled ? props.storageId : undefined,
    };
    await profileApi.updateProfile({
      updateProfileRequestDto: updatedProfile,
    });
  };
</script>

<style lang="scss">
  .documents-storage-config {
    margin-bottom: 20px;

    &__header {
      display: flex;
      align-items: center;
      margin-bottom: 10px;

      h4 {
        display: inline;
        margin: 0 0 0 10px;
      }
    }
  }
</style>
