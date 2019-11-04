<template>
  <div class="overview-item-details-section-actions">
    <slot />
  </div>
</template>

<script>
  export default {
    name: 'OverviewItemDetailsSectionActions',
  };
</script>

<style lang="scss">
  .overview-item-details-section-actions {
    padding-bottom: 10px;
    display: flex;
    justify-content: flex-end;

    & > * {
      margin-left: 15px;
    }
  }
</style>
