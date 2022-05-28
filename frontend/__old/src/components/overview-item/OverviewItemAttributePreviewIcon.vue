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

<script lang="ts">
  import { defineComponent } from '@vue/composition-api';
  import SaIcon from '@/components/SaIcon';

  export default defineComponent({
    components: {
      SaIcon,
    },

    props: {
      icon: {
        type: String,
        required: true,
      },
      tooltip: {
        type: String,
        default: null,
      },
    },
  });
</script>

<style lang="scss">
  .overview-item-attribute-preview-icon {
    margin-right: 5px;
    width: 18px;
    height: 18px;
  }
</style>
