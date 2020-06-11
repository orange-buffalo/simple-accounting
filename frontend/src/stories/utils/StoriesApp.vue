<template>
  <div>
    <slot v-if="loaded" />
  </div>
</template>

<script>
  import { ref } from '@vue/composition-api';
  import mainConfig from '@/setup/setup-app';

  export default {
    setup() {
      const loaded = ref(false);

      async function loadI18n() {
        await mainConfig.app.i18n.setLocaleFromProfile({
          locale: 'en',
          language: 'en',
        });
        loaded.value = true;
      }

      loadI18n();

      return {
        loaded,
      };
    },
  };
</script>
