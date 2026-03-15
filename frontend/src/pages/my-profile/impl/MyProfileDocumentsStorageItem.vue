<template>
  <div
    class="sa-documents-storage-item"
    :id="`storage-config_${storageId}`"
  >
    <div class="sa-documents-storage-item__header">
      <span class="sa-documents-storage-item__name">
        {{ name }}
      </span>
      <slot name="status" />
    </div>
    <slot name="details" />
  </div>
</template>

<script lang="ts" setup>
  defineProps<{
    storageId: string,
    name: string,
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
  }
</style>
