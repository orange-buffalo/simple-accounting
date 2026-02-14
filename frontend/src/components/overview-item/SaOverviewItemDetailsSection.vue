<template>
  <div class="overview-item-details-section">
    <div class="overview-item-details-section__title">
      {{ title }}
    </div>
    <slot />
  </div>
</template>

<script lang="ts" setup>
defineProps<{
  title: string;
}>();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .overview-item-details-section {
    margin-bottom: 15px;

    &__title {
      color: $secondary-text-color;
      text-transform: uppercase;
      font-weight: bold;
      font-size: 90%;
    }
  }
</style>
