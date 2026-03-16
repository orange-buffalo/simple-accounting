<template>
  <ElAlert
    type="error"
    :closable="false"
    class="sa-failed-documents-storage-message"
  >
    <template #title>
      <SaIcon icon="error" />
      {{ reason === 'unsupported-documents'
        ? $t.saFailedDocumentsStorageMessage.unsupportedDocuments.title()
        : $t.saFailedDocumentsStorageMessage.storageNotConfigured.title() }}
    </template>
    <template #default>
      <SaI18n
        v-if="reason === 'unsupported-documents'"
        :message="$t.saFailedDocumentsStorageMessage.unsupportedDocuments.message()"
      >
        <ElButton
          link
          @click="navigateToProfileSettings"
        >
          {{ $t.saFailedDocumentsStorageMessage.unsupportedDocuments.profileLink() }}
        </ElButton>
      </SaI18n>
      <SaI18n
        v-else
        :message="$t.saFailedDocumentsStorageMessage.storageNotConfigured.message()"
      >
        <ElButton
          link
          @click="navigateToProfileSettings"
        >
          {{ $t.saFailedDocumentsStorageMessage.storageNotConfigured.profileLink() }}
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

  defineProps<{
    reason: 'storage-not-configured' | 'unsupported-documents'
  }>();

  const { navigateByViewName } = useNavigation();

  const navigateToProfileSettings = () => {
    navigateByViewName('my-profile');
  };
</script>

<style lang="scss">
  .sa-failed-documents-storage-message {
    .el-alert__description .el-button.is-link {
      vertical-align: middle;
      padding: 0;
      height: auto;
      font-size: inherit;
    }
  }
</style>
