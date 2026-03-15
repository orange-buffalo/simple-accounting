<template>
  <div class="sa-documents-storage-section">
    <h2>{{ $t.myProfile.documentsStorage.header() }}</h2>

    <p class="sa-documents-storage-section__description">
      {{ $t.myProfile.documentsStorage.description() }}
    </p>

    <div
      v-if="loading"
      class="sa-documents-storage-section__loading"
    >
      <div class="sa-documents-storage-section__loading-placeholder" />
      <div class="sa-documents-storage-section__loading-placeholder" />
    </div>

    <template v-else>
      <MyProfileDocumentsStorageItem
        storage-id="google-drive"
        :name="$t.myProfile.documentsStorage.googleDrive()"
      >
        <template #status>
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
        </template>
        <template #details>
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
        </template>
      </MyProfileDocumentsStorageItem>

      <div class="sa-documents-storage-section__divider" />

      <MyProfileDocumentsStorageItem
        storage-id="local-fs"
        :name="$t.myProfile.documentsStorage.localStorage()"
      >
        <template #status>
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
        </template>
        <template #details>
          <p
            v-if="!localStorageEnabled"
            class="sa-documents-storage-section__storage-info"
          >
            {{ $t.myProfile.documentsStorage.localStorageDisabledDetails() }}
          </p>
        </template>
      </MyProfileDocumentsStorageItem>
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
  import MyProfileDocumentsStorageItem from '@/pages/my-profile/impl/MyProfileDocumentsStorageItem.vue';

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
    padding: 20px;
    border: 1px solid $secondary-grey;
    background-color: $white;
    border-radius: 2px;
    overflow: hidden;
    margin-bottom: 30px;

    &__description {
      font-size: 90%;
      color: $secondary-text-color;
      margin: 0 0 16px;
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

    &__divider {
      border-top: 1px solid $secondary-grey;
      margin: 16px 0;
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
