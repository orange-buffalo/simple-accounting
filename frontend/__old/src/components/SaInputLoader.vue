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

<script>
  import SaBasicErrorMessage from '@/components/SaBasicErrorMessage';

  export default {
    components: { SaBasicErrorMessage },
    props: {
      loading: {
        type: Boolean,
        required: true,
      },
      error: {
        type: Boolean,
        default: false,
      },
    },

    setup() {
      // no special behaviour
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";
  @import "~@/styles/mixins.scss";

  .sa-input-loader {
    &__indicator {
      height: $input-height;
      @include loading-placeholder;
      @include input-width;
    }
  }
</style>
