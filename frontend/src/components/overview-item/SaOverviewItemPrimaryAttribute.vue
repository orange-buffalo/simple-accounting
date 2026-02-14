<template>
  <ElTooltip
    :content="tooltip"
    :disabled="!tooltip"
    placement="bottom"
  >
    <span class="overview-item-primary-attribute">
      <SaIcon
        :icon="icon"
        class="overview-item-primary-attribute__icon"
      />
      <slot />
    </span>
  </ElTooltip>
</template>

<script lang="ts" setup>
import SaIcon from '@/components/SaIcon.vue';

defineProps<{
  icon: string;
  tooltip?: string;
}>();
</script>

<style lang="scss">
  .overview-item-primary-attribute {
    display: inline-flex;
    align-items: center;
    margin-right: 20px;

    &__icon {
      margin-right: 5px;
      width: 18px;
      height: 18px;
    }
  }
</style>
