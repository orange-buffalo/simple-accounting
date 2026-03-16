<template>
  <div
    class="sa-documents-storage-item"
    :id="`storage-config_${storageId}`"
  >
    <div class="sa-documents-storage-item__header">
      <span class="sa-documents-storage-item__name">
        {{ name }}
      </span>
      <SaStatusLabel
        v-if="usedForUploads"
        status="success"
      >
        {{ $t.myProfile.documentsStorage.usedForUploads() }}
      </SaStatusLabel>
      <ElButton
        v-else-if="available"
        link
        class="sa-documents-storage-item__use-action"
        @click="emit('use-for-uploads')"
      >
        {{ $t.myProfile.documentsStorage.useForUploads() }}
      </ElButton>
      <slot
        v-else
        name="unavailable-status"
      />
    </div>
    <slot name="details" />
  </div>
</template>

<script lang="ts" setup>
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { $t } from '@/services/i18n';

  withDefaults(defineProps<{
    storageId: string,
    name: string,
    usedForUploads: boolean,
    available?: boolean,
  }>(), {
    available: true,
  });

  const emit = defineEmits<{
    (e: 'use-for-uploads'): void,
  }>();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .sa-documents-storage-item {
    margin-bottom: 4px;

    &__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    &__name {
      font-weight: 600;
      color: $primary-text-color;
    }

    &__use-action {
      padding: 0;
    }
  }
</style>
