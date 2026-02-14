<template>
  <span class="sa-icon" :data-icon="icon" @click="$emit('click')" :style="style">
    <Component :is="loadedIcon" />
  </span>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { iconByName } from '@/icons';

const props = withDefaults(
  defineProps<{
    icon: string;
    size?: number;
  }>(),
  {
    size: 16,
  },
);

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
