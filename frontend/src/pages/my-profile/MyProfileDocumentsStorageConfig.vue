<template>
  <div class="documents-storage-config">
    <div class="documents-storage-config__header">
      <ElSwitch
        v-model="enabled"
        @change="onEnabledChange"
      />
      <h4>{{ storageName }}</h4>
    </div>
    <slot v-if="enabled" />
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';

  const props = defineProps<{
    storageName: string,
    storageId: string,
    userDocumentsStorage: string,
  }>();

  const emit = defineEmits<{(e: 'storage-enabled', storageId: string): void,
                            (e: 'storage-disabled', storageId: string): void;
  }>();

  const enabled = ref(false);

  const setEnabled = () => {
    enabled.value = props.storageId === props.userDocumentsStorage;
  };
  setEnabled();

  watch(() => [props.storageId, props.userDocumentsStorage], setEnabled);

  const onEnabledChange = () => {
    if (enabled.value) {
      emit('storage-enabled', props.storageId);
    } else {
      emit('storage-disabled', props.storageId);
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
