<template>
  <div class="overview-item-details-section">
    <div class="overview-item-details-section__title">
      {{ title }}
    </div>
    <slot />
  </div>
</template>

<script>
  export default {
    name: 'OverviewItemDetailsSection',

    props: {
      title: {
        type: String,
        required: true,
      },
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";

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
