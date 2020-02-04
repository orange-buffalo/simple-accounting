<template>
  <div>
    <div
      v-if="loading"
      class="sa-input-loader"
    />

    <slot v-else />
  </div>
</template>

<script>
  export default {
    props: {
      loading: {
        type: Boolean,
        required: true,
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
    height: $input-height;
    @include loading-placeholder;
    @include input-width;
  }
</style>
