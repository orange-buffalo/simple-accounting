<template>
  <div :class="classes">
    <slot v-if="loaded" />
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { setLocaleFromProfile } from '@/setup/setup-app';

  const props = withDefaults(defineProps<{
    fullScreen?: boolean;
    asPage?: boolean,
  }>(), {
    fullScreen: false,
    asPage: false,
  });

  const loaded = ref(false);

  async function loadI18n() {
    await setLocaleFromProfile('en-AU', 'en');
    loaded.value = true;
  }

  loadI18n();

  const classes = computed(() => ({
    'stories-app--full-width': !props.fullScreen && !props.asPage,
    'stories-app--as-page': props.asPage,
    'stories-app--full-screen': props.fullScreen,
  }));
</script>

<!--suppress CssUnknownTarget, SassScssResolvedByNameOnly -->
<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .sb-show-main.sb-main-padded {
    padding: 0;
  }

  .stories-app--full-width {
    width: 100%;
    box-sizing: border-box;
    padding: 1rem;
  }

  .stories-app--full-screen {
    width: 100%;
    height: 100vh;
  }

  .stories-app--as-page {
    width: 100%;
    min-height: 100vh;
    box-sizing: border-box;
    padding: 1rem;
    background-color: $primary-grey;
  }
</style>
