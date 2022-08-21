<template>
  <ElAlert
    type="error"
    :closable="false"
  >
    <template #title>
      <SaIcon icon="error" />
      {{ $t.saFailedDocumentsStorageMessage.title() }}
    </template>
    <template #default>
      <SaI18n :message="$t.saFailedDocumentsStorageMessage.message()">
        <ElButton
          type="text"
          @click="navigateToProfileSettings"
        >
          {{ $t.saFailedDocumentsStorageMessage.profileLink() }}
        </ElButton>
      </SaI18n>
    </template>
  </ElAlert>
</template>

<script lang="ts" setup>
  import SaIcon from '@/components/SaIcon.vue';
  import SaI18n from '@/components/SaI18n.vue';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';

  const { navigateByViewName } = useNavigation();

  const navigateToProfileSettings = () => {
    navigateByViewName('my-profile');
  };
</script>
