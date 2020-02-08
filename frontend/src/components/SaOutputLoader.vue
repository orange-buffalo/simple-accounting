<template>
  <div>
    <div
      v-if="loading"
      class="sa-output-loader"
    >
      &nbsp;
    </div>

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
    display: inline-block;
    width: 70px;
    @include loading-placeholder;
  }
</style>
