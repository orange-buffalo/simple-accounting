<template>
  <span class="sa-icon" v-html="loadedIcon" @click="$emit('click')"/>
</template>

<script lang="ts">
  import { defineComponent, shallowRef, watch } from 'vue';
  import { iconByName } from '@/icons';

  export default defineComponent({
    props: {
      icon: {
        type: String,
        required: true,
      },
    },

    setup(props) {
      const loadedIcon = shallowRef<string | null>(null);

      watch(() => props.icon, async (icon) => {
        loadedIcon.value = iconByName(icon);
      }, { immediate: true });

      return {
        loadedIcon,
      };
    },
  });
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
