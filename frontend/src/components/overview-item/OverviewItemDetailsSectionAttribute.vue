<template>
  <div class="overview-item-details-section-attribute">
    <div class="overview-item-details-section-attribute__label">
      {{ label }}
    </div>
    <div>
      <slot />
    </div>
  </div>
</template>

<script>
  export default {
    name: 'OverviewItemDetailsSectionAttribute',

    props: {
      label: {
        type: String,
        required: true,
      },
    },
  };
</script>

<style lang="scss">
  @import "@/styles/vars.scss";

  .overview-item-details-section-attribute {
    &__label {
      color: $secondary-text-color;
    }
  }
</style>
