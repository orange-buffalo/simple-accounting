<template>
  <div :class="classes">
    <slot v-if="loaded" />
  </div>
</template>

<script>
  import { ref, computed } from '@vue/composition-api';
  import mainConfig from '@/setup/setup-app';

  export default {
    props: {
      fullWidth: {
        type: Boolean,
        default: false,
      },
    },
    setup(props) {
      const loaded = ref(false);

      async function loadI18n() {
        await mainConfig.app.i18n.setLocaleFromProfile({
          locale: 'en-AU',
          language: 'en',
        });
        loaded.value = true;
      }

      loadI18n();

      const classes = computed(() => ({
        'stories-app--full-width': props.fullWidth,
        'stories-app--full-centered': !props.fullWidth,
      }));

      return {
        loaded,
        classes,
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";

  .stories-app--full-width {
    width: 100%;
    height: 100vh;
    padding: 20px;
    box-sizing: border-box;
    background: $primary-grey;
  }

  .stories-app--full-centered {
    width: 100%;
    height: 100vh;
    margin-top: 50px;
    box-sizing: border-box;
    display: flex;
    justify-content: center;
    align-items: start;
  }
</style>
