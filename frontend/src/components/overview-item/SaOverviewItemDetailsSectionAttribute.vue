<template>
  <div class="overview-item-details-section-attribute">
    <div class="overview-item-details-section-attribute__label">
      {{ label }}
    </div>
    <div>
      <slot />
    </div>
  </div>
</template>

<script lang="ts" setup>
defineProps<{
  label: string;
}>();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .overview-item-details-section-attribute {
    &__label {
      color: $secondary-text-color;
    }
  }
</style>
