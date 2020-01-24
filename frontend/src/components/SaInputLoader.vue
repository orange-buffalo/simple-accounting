<template>
  <div>
    <div
      v-if="loading"
      class="sa-input-loader"
    >
      <SaIcon
        icon="loading-indicator"
        class="sa-input-loader__spinner"
      />
    </div>

    <slot v-else />
  </div>
</template>

<script>
  import SaIcon from '@/components/SaIcon';

  export default {
    components: {
      SaIcon,
    },

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

  $spinner-size: $input-height * 0.7;

  .sa-input-loader {

    height: $input-height;
    display: flex;
    align-items: center;

    &__spinner {
      width: $spinner-size;
      height: $spinner-size;
      color: $secondary-grey;
    }
  }
</style>
