<template>
  <ElTooltip
    :content="tooltip"
    :disabled="!tooltip"
    placement="bottom"
  >
    <SaIcon
      :icon="icon"
      class="overview-item-attribute-preview-icon"
    />
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
  .overview-item-attribute-preview-icon {
    margin-right: 5px;
    width: 18px;
    height: 18px;
  }
</style>
