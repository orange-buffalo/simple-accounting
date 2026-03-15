<template>
  <div class="sa-documents-storage-section">
    <h2>{{ $t.myProfile.documentsStorage.header() }}</h2>

    <div
      v-if="loading"
      class="sa-documents-storage-section__loading"
    >
      <div class="sa-documents-storage-section__loading-placeholder" />
      <div class="sa-documents-storage-section__loading-placeholder" />
    </div>

    <template v-else>
      <div
        class="sa-documents-storage-section__storage"
        id="storage-config_google-drive"
      >
        <div class="sa-documents-storage-section__storage-header">
          <span class="sa-documents-storage-section__storage-name">
            {{ $t.myProfile.documentsStorage.googleDrive() }}
          </span>
          <SaStatusLabel
            v-if="isGoogleDriveUsedForUploads"
            status="success"
          >
            {{ $t.myProfile.documentsStorage.usedForUploads() }}
          </SaStatusLabel>
          <ElButton
            v-else
            link
            class="sa-documents-storage-section__use-action"
            @click="useForUploads('google-drive')"
          >
            {{ $t.myProfile.documentsStorage.useForUploads() }}
          </ElButton>
        </div>
        <div
          v-if="showGoogleDriveDetails"
          class="sa-documents-storage-section__storage-details"
        >
          <p
            v-if="!isGoogleDriveUsedForUploads && googleDriveDocumentsCount > 0"
            class="sa-documents-storage-section__storage-info"
          >
            {{ $t.myProfile.documentsStorage.googleDriveDocumentsNotice(googleDriveDocumentsCount) }}
          </p>
          <SaGoogleDriveIntegrationSetup />
        </div>
      </div>

      <div class="sa-documents-storage-section__divider" />

      <div
        class="sa-documents-storage-section__storage"
        id="storage-config_local-fs"
      >
        <div class="sa-documents-storage-section__storage-header">
          <span class="sa-documents-storage-section__storage-name">
            {{ $t.myProfile.documentsStorage.localStorage() }}
          </span>
          <template v-if="localStorageEnabled">
            <SaStatusLabel
              v-if="isLocalFsUsedForUploads"
              status="success"
            >
              {{ $t.myProfile.documentsStorage.usedForUploads() }}
            </SaStatusLabel>
            <ElButton
              v-else
              link
              class="sa-documents-storage-section__use-action"
              @click="useForUploads('local-fs')"
            >
              {{ $t.myProfile.documentsStorage.useForUploads() }}
            </ElButton>
          </template>
          <SaStatusLabel
            v-else
            status="regular"
          >
            {{ $t.myProfile.documentsStorage.localStorageDisabledStatus() }}
          </SaStatusLabel>
        </div>
        <p
          v-if="!localStorageEnabled"
          class="sa-documents-storage-section__storage-info"
        >
          {{ $t.myProfile.documentsStorage.localStorageDisabledDetails() }}
        </p>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import SaGoogleDriveIntegrationSetup
    from '@/components/documents/storage/SaGoogleDriveIntegrationSetup.vue';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useMultiQuery, useMutation } from '@/services/api/use-gql-api.ts';
  import type { UserProfileQuery } from '@/services/api/gql/graphql.ts';

  const props = defineProps<{
    profile?: UserProfileQuery['userProfile'],
    loading: boolean,
  }>();

  const emit = defineEmits<{
    (e: 'profile-updated', profile: UserProfileQuery['userProfile']): void,
  }>();

  const [storageDataLoading, storageData] = useMultiQuery(graphql(/* GraphQL */ `
    query documentsStorageConfig {
      documentsStorageStatistics {
        storageId
        documentsCount
      }
      systemSettings {
        localFileSystemDocumentsStorageEnabled
      }
    }
  `));

  const loading = computed(() => props.loading || storageDataLoading.value);

  const localStorageEnabled = computed(
    () => storageData.value?.systemSettings?.localFileSystemDocumentsStorageEnabled ?? false,
  );

  const isGoogleDriveUsedForUploads = computed(() => props.profile?.documentsStorage === 'google-drive');
  const isLocalFsUsedForUploads = computed(() => props.profile?.documentsStorage === 'local-fs');

  const googleDriveDocumentsCount = computed(() => {
    const stats = storageData.value?.documentsStorageStatistics ?? [];
    const stat = stats.find(s => s.storageId === 'google-drive');
    return stat?.documentsCount ?? 0;
  });

  const showGoogleDriveDetails = computed(() => {
    if (isGoogleDriveUsedForUploads.value) return true;
    return googleDriveDocumentsCount.value > 0;
  });

  const updateProfileMutation = useMutation(graphql(/* GraphQL */ `
    mutation updateProfileStorage($documentsStorage: String, $locale: String!, $language: String!) {
      updateProfile(documentsStorage: $documentsStorage, locale: $locale, language: $language) {
        documentsStorage
        i18n {
          language
          locale
        }
        userName
      }
    }
  `), 'updateProfile');

  const useForUploads = async (storageId: string) => {
    if (!props.profile) return;
    const updatedProfile = await updateProfileMutation({
      documentsStorage: storageId,
      locale: props.profile.i18n.locale,
      language: props.profile.i18n.language,
    });
    emit('profile-updated', updatedProfile);
  };
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-documents-storage-section {
    margin-bottom: 20px;

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

    &__divider {
      border-top: 1px solid $secondary-grey;
      margin: 16px 0;
    }

    &__storage {
      margin-bottom: 4px;
    }

    &__storage-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    &__storage-name {
      font-weight: 600;
      color: $primary-text-color;
    }

    &__use-action {
      padding: 0;
    }

    &__storage-details {
      margin-top: 8px;
    }

    &__storage-info {
      font-size: 90%;
      color: $secondary-text-color;
      margin: 8px 0;
    }
  }
</style>
