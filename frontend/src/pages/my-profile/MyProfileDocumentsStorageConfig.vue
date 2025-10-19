<template>
  <SaForm
    v-model="formValues"
    :on-submit="submitStorageConfig"
    :external-loading="props.loading"
    :hide-buttons="true"
  >
    <h2>{{ $t.myProfile.documentsStorage.header() }}</h2>
    <div class="documents-storage-config" :id="`storage-config_${storageId}`">
      <SaFormSwitchSection
        :label="storageName"
        prop="enabled"
        :submit-on-change="true"
      />
      <slot v-if="formValues.enabled" />
    </div>
  </SaForm>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { ProfileDto, profileApi } from '@/services/api';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormSwitchSection from '@/components/form/SaFormSwitchSection.vue';
  import { $t } from '@/services/i18n';

  const props = defineProps<{
    storageName: string,
    storageId: string,
    profile: ProfileDto,
    loading: boolean,
  }>();

  const emit = defineEmits<{
    (e: 'profile-updated', profile: ProfileDto): void,
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

  const submitStorageConfig = async () => {
    const updatedProfile: ProfileDto = {
      ...props.profile,
      documentsStorage: formValues.value.enabled ? props.storageId : undefined,
    };
    await profileApi.updateProfile({
      updateProfileRequestDto: updatedProfile,
    });
    emit('profile-updated', updatedProfile);
  };
</script>

<style lang="scss">
  .documents-storage-config {
    margin-bottom: 20px;
  }
</style>
