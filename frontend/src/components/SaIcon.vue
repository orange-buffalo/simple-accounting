<template>
  <span class="sa-icon">
    <Component
      :is="loadedIcon"
      class="sa-icon__svg"
      @click="$emit('click')"
    />
  </span>
</template>

<script>
  import { ref, watch } from '@vue/composition-api';

  async function loadIcon(iconName) {
    const iconModule = await import(/* webpackMode: "eager" */ `@/icons/svg/${iconName}.svg`);
    return iconModule.default;
  }

  export default {
    props: {
      icon: {
        type: String,
        required: true,
      },
    },

    setup(props) {
      const loadedIcon = ref(null);

      watch(() => props.icon, async (icon) => {
        loadedIcon.value = await loadIcon(icon);
      }, { immediate: true });

      return {
        loadedIcon,
      };
    },
  };
</script>

<style lang="scss">
  .sa-icon {
    display: inline-flex;
    width: 16px;
    height: 16px;

    &__svg {
      width: 100%;
      height: 100%;
      color: inherit;
      fill: currentColor;
      stroke: none;
    }
  }
</style>
