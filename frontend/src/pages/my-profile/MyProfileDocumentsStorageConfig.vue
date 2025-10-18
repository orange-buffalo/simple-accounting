<template>
  <div class="documents-storage-config" :id="`storage-config_${storageId}`">
    <div class="documents-storage-config__header">
      <ElSwitch
        v-model="enabled"
        @change="onEnabledChange"
        :loading="submitting"
        :disabled="props.loading"
      />
      <h4>{{ storageName }}</h4>
    </div>
    <slot v-if="enabled" />
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { ProfileDto, profileApi } from '@/services/api';

  const props = defineProps<{
    storageName: string,
    storageId: string,
    profile: ProfileDto,
    loading: boolean,
  }>();

  const enabled = ref(false);
  const submitting = ref(false);

  const setEnabled = () => {
    enabled.value = props.storageId === props.profile.documentsStorage;
  };
  setEnabled();

  watch(() => props.profile, setEnabled, { deep: true });

  const onEnabledChange = async () => {
    submitting.value = true;
    try {
      const updatedProfile: ProfileDto = {
        ...props.profile,
        documentsStorage: enabled.value ? props.storageId : undefined,
      };
      await profileApi.updateProfile({
        updateProfileRequestDto: updatedProfile,
      });
    } finally {
      submitting.value = false;
    }
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
