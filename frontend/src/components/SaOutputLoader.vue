<template>
  <div class="sa-output-loader">
    <div
      v-if="loading"
      class="sa-output-loader__placeholder"
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
  @import "~@/styles/mixins.scss";

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
