<template>
  <div :class="classes">
    <slot v-if="loaded" />
  </div>
</template>

<script lang="ts">
  import { ref, computed, defineComponent } from 'vue';
  import { setLocaleFromProfile } from '@/setup/setup-app';

  export default defineComponent({
    props: {
      fullScreen: {
        type: Boolean,
        default: false,
      },
    },

    setup(props) {
      const loaded = ref(false);

      async function loadI18n() {
        await setLocaleFromProfile('en-AU', 'en');
        loaded.value = true;
      }

      loadI18n();

      const classes = computed(() => ({
        'stories-app--full-width': !props.fullScreen,
        'stories-app--full-screen': props.fullScreen,
      }));

      return {
        loaded,
        classes,
      };
    },
  });
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .stories-app--full-width {
    width: 100%;
  }

  .stories-app--full-screen {
    width: 100%;
    height: 100vh;
  }
</style>
