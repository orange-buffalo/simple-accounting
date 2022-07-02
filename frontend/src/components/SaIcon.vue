<template>
  <span class="sa-icon" v-html="loadedIcon" @click="$emit('click')" />
</template>

<script lang="ts" setup>
  import { shallowRef, watch } from 'vue';
  import { iconByName } from '@/icons';

  const props = defineProps<{
    icon: string;
  }>();

  const loadedIcon = shallowRef<string | null>(null);

  watch(() => props.icon, async (icon) => {
    loadedIcon.value = iconByName(icon);
  }, { immediate: true });
</script>

<style lang="scss">
  .sa-icon {
    display: inline-flex;
    width: 16px;
    height: 16px;

    svg {
      width: 100%;
      height: 100%;
      color: inherit;
      fill: currentColor;
      stroke: none;
    }
  }
</style>
