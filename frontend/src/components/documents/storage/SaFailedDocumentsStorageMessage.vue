<template>
  <ElAlert
    type="error"
    :closable="false"
  >
    <template #title>
      <SaIcon icon="error" />
      {{ $t('saFailedDocumentsStorageMessage.title') }}
    </template>
    <template #default>
      <I18n
        path="saFailedDocumentsStorageMessage.message"
        :tag="false"
      >
        <ElButton
          type="text"
          @click="navigateToProfileSettings"
        >
          {{ $t('saFailedDocumentsStorageMessage.profileLink') }}
        </ElButton>
      </I18n>
    </template>
  </ElAlert>
</template>

<script>
  import SaIcon from '@/components/SaIcon';

  export default {
    components: { SaIcon },

    setup(props, { root }) {
      return {
        navigateToProfileSettings() {
          root.$router.push('my-profile');
        },
      };
    },
  };
</script>
