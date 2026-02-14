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
          link
          @click="navigateToProfileSettings"
        >
          {{ $t.saFailedDocumentsStorageMessage.profileLink() }}
        </ElButton>
      </SaI18n>
    </template>
  </ElAlert>
</template>

<script lang="ts" setup>
import SaI18n from '@/components/SaI18n.vue';
import SaIcon from '@/components/SaIcon.vue';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';

const { navigateByViewName } = useNavigation();

const navigateToProfileSettings = () => {
  navigateByViewName('my-profile');
};
</script>
