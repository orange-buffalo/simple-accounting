<template>
  <div class="sa-documents-migration-page">
    <div class="sa-page-header">
      <h1>{{ $t.documentsMigration.pageHeader() }}</h1>
    </div>

    <div
      v-if="loading"
      class="sa-documents-migration-page__loading"
    >
      <div class="sa-documents-migration-page__loading-placeholder" />
      <div class="sa-documents-migration-page__loading-placeholder" />
    </div>

    <div
      v-else
      class="sa-documents-migration-page__content"
    >
      <p class="sa-documents-migration-page__description">
        {{ $t.documentsMigration.description(totalDocumentsOutsideUploadStorage) }}
      </p>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { graphql } from '@/services/api/gql';
  import { useMultiQuery } from '@/services/api/use-gql-api.ts';
  import { $t } from '@/services/i18n';

  const [loading, migrationData] = useMultiQuery(graphql(/* GraphQL */ `
    query documentsMigrationStorageStatistics {
      documentsStorageStatistics {
        storageId
        documentsCount
      }
      userProfile {
        documentsStorage
      }
    }
  `));

  const totalDocumentsOutsideUploadStorage = computed(() => {
    const uploadStorage = migrationData.value?.userProfile.documentsStorage;
    return migrationData.value?.documentsStorageStatistics
      .filter(stat => stat.storageId !== uploadStorage)
      .reduce((total, stat) => total + stat.documentsCount, 0) ?? 0;
  });
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-documents-migration-page {
    &__loading,
    &__content {
      padding: 20px;
      border: 1px solid $secondary-grey;
      background-color: $white;
      border-radius: 2px;
    }

    &__loading {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    &__loading-placeholder {
      height: 40px;
      @include loading-placeholder;
      border-radius: 5px;
    }

    &__description {
      color: $primary-text-color;
      margin: 0;
    }
  }
</style>
