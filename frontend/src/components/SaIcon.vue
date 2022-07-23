<template>
  <span class="sa-icon" @click="$emit('click')" :style="style">
    <Component :is="loadedIcon" />
  </span>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { iconByName } from '@/icons';

  const props = defineProps({
    icon: {
      type: String,
      required: true,
    },
    size: {
      type: Number,
      required: false,
      default: 16,
    },
  });

  const style = {
    width: `${props.size}px`,
    height: `${props.size}px`,
  };

  const loadedIcon = computed(() => iconByName(props.icon));

</script>

<style lang="scss">
  .sa-icon {
    display: inline-flex;

    svg {
      width: 100%;
      height: 100%;
      color: inherit;
      fill: currentColor;
      stroke: none;
    }
  }
</style>
