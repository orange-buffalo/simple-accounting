<template>
  <ElAlert
    type="error"
    :closable="false"
    class="sa-failed-documents-storage-message"
  >
    <template #title>
      <SaIcon icon="error" />
      <span>{{ $t.saFailedDocumentsStorageMessage.title() }}</span>
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
  import SaIcon from '@/components/SaIcon.vue';
  import SaI18n from '@/components/SaI18n.vue';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';

  const { navigateByViewName } = useNavigation();

  const navigateToProfileSettings = () => {
    navigateByViewName('my-profile');
  };
</script>

<style lang="scss">
  .sa-failed-documents-storage-message {
    .el-alert__title {
      align-items: flex-start;
      gap: 8px;
      line-height: 1.5;

      .sa-icon {
        flex-shrink: 0;
        margin: calc((1.5em - 20px) / 2) 0 0;
      }
    }

    .el-alert__description {
      padding-inline-start: 28px;
      line-height: 1.5;
    }

    .el-alert__description .el-button.is-link {
      vertical-align: middle;
      padding: 0;
      height: auto;
      font-size: inherit;
    }
  }
</style>
