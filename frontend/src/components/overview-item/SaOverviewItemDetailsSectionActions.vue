<template>
  <div class="overview-item-details-section-actions">
    <slot />
  </div>
</template>

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
