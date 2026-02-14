<template>
  <div class="sa-input-loader">
    <div
      v-if="loading"
      class="sa-input-loader__indicator"
    />

    <SaBasicErrorMessage v-else-if="error" />

    <slot v-else />
  </div>
</template>

<script lang="ts" setup>
import SaBasicErrorMessage from '@/components/SaBasicErrorMessage.vue';

defineProps<{
  loading: boolean;
  error?: boolean;
}>();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-input-loader {
    &__indicator {
      height: $sa-input-height;
      @include loading-placeholder;
      @include input-width;
    }
  }
</style>
