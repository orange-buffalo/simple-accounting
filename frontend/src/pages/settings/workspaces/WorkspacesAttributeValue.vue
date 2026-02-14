<template>
  <div class="sa-attribute-value">
    <div class="sa-attribute-value__label">
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
  .sa-attribute-value {

    &__label {
      font-weight: bold;
    }
  }
</style>
