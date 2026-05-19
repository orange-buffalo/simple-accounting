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
      <p
        v-if="requiredStorageUnavailable"
        class="sa-documents-migration-page__message"
      >
        {{ $t.documentsMigration.storageUnavailable() }}
      </p>

      <p
        v-else-if="allDocumentsInUploadStorage"
        class="sa-documents-migration-page__message"
      >
        {{ $t.documentsMigration.noMigrationRequired() }}
      </p>

      <div
        v-else-if="activeMigration"
        class="sa-documents-migration-page__active-migration"
      >
        <p class="sa-documents-migration-page__message">
          {{ $t.documentsMigration.migrationInProgress() }}
        </p>
        <ElProgress
          :percentage="migrationProgressPercentage"
          :format="formatMigrationProgress"
        />
      </div>

      <div
        v-else
        class="sa-documents-migration-page__ready"
      >
        <p class="sa-documents-migration-page__message">
          {{ $t.documentsMigration.description(totalDocumentsOutsideUploadStorage) }}
        </p>
        <div class="sa-documents-migration-page__start-panel">
          <ElButton
            type="primary"
            :loading="startingMigration"
            @click="startMigration"
          >
            {{ $t.documentsMigration.startMigration() }}
          </ElButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { graphql } from '@/services/api/gql';
  import { useMultiQuery, useMutation } from '@/services/api/use-gql-api.ts';
  import { $t } from '@/services/i18n';
  import type { StartDocumentsMigrationFromPageMutation } from '@/services/api/gql/graphql.ts';

  type DocumentsMigration = StartDocumentsMigrationFromPageMutation['startDocumentsMigration'];

  const [loading, migrationData] = useMultiQuery(graphql(/* GraphQL */ `
    query documentsMigrationStorageStatistics {
      documentsStorageStatistics {
        storageId
        documentsCount
      }
      documentsStorageStatus {
        active
      }
      getDownloadDocumentStorages {
        id
      }
      userProfile {
        documentsStorage
      }
      documentsMigrations(first: 1) {
        edges {
          node {
            id
            requestedDocumentsCount
            migratedDocumentsCount
            completedAt
          }
        }
      }
    }
  `));

  const executeStartDocumentsMigration = useMutation(graphql(/* GraphQL */ `
    mutation startDocumentsMigrationFromPage {
      startDocumentsMigration {
        id
        requestedDocumentsCount
        migratedDocumentsCount
        completedAt
      }
    }
  `), 'startDocumentsMigration');

  const startedMigration = ref<DocumentsMigration | null>(null);
  const startingMigration = ref(false);

  const latestMigration = computed(() => (
    startedMigration.value ?? migrationData.value?.documentsMigrations.edges[0]?.node ?? null
  ));

  const activeMigration = computed(() => {
    const migration = latestMigration.value;
    return migration != null && migration.completedAt == null;
  });

  const availableDownloadStorages = computed(() => new Set(
    migrationData.value?.getDownloadDocumentStorages.map(storage => storage.id) ?? [],
  ));

  const usedStorageIds = computed(() => (
    migrationData.value?.documentsStorageStatistics.map(stat => stat.storageId) ?? []
  ));

  const uploadStorage = computed(() => migrationData.value?.userProfile.documentsStorage ?? null);

  const requiredStorageUnavailable = computed(() => {
    if (uploadStorage.value == null || migrationData.value?.documentsStorageStatus.active !== true) {
      return true;
    }
    return usedStorageIds.value.some(storageId => !availableDownloadStorages.value.has(storageId));
  });

  const totalDocumentsOutsideUploadStorage = computed(() => {
    return migrationData.value?.documentsStorageStatistics
      .filter(stat => stat.storageId !== uploadStorage.value)
      .reduce((total, stat) => total + stat.documentsCount, 0) ?? 0;
  });

  const allDocumentsInUploadStorage = computed(() => totalDocumentsOutsideUploadStorage.value === 0);

  const migrationProgressPercentage = computed(() => {
    const migration = latestMigration.value;
    if (!migration || migration.requestedDocumentsCount === 0) return 100;
    return Math.floor((migration.migratedDocumentsCount / migration.requestedDocumentsCount) * 100);
  });

  const formatMigrationProgress = () => {
    const migration = latestMigration.value;
    if (!migration) return '';
    return $t.value.documentsMigration.migrationProgress(
      migration.migratedDocumentsCount,
      migration.requestedDocumentsCount,
    );
  };

  const startMigration = async () => {
    startingMigration.value = true;
    try {
      startedMigration.value = await executeStartDocumentsMigration({});
    } finally {
      startingMigration.value = false;
    }
  };
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

    &__message {
      color: $primary-text-color;
      margin: 0;
    }

    &__active-migration,
    &__ready {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    &__active-migration {
      .el-progress--line {
        width: 100%;
      }
    }

    &__start-panel {
      display: flex;
      justify-content: center;
      padding: 24px;
      border: 1px solid $secondary-grey;
      background-color: $white;
      border-radius: 2px;
    }
  }
</style>
