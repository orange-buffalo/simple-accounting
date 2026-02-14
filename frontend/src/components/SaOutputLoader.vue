<template>
  <div class="sa-output-loader">
    <div
      v-if="loading"
      class="sa-output-loader__placeholder"
    />

    <slot v-else />
  </div>
</template>

<script lang="ts" setup>
defineProps({
  loading: {
    type: Boolean,
    required: true,
  },
});
</script>

<style lang="scss">
  @use "@/styles/mixins.scss" as *;

  .sa-output-loader {
    display: inline-flex;
    align-items: center;

    &__placeholder {
      display: inline-block;
      width: 70px;
      height: 1em;
      @include loading-placeholder;
    }
  }
</style>
