<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.myProfile.pageHeader() }}</h1>
    </div>

    <MyProfileDocumentsStorageConfig
      v-if="!isAdmin()"
      :storage-name="$t.myProfile.documentsStorage.googleDrive()"
      storage-id="google-drive"
      :profile="profile"
      :loading="loading"
      @profile-updated="onProfileUpdated"
    >
      <SaGoogleDriveIntegrationSetup />
    </MyProfileDocumentsStorageConfig>

    <MyProfileLanguagePreferences
      :profile="profile"
      :loading="loading"
      @profile-updated="onProfileUpdated"
    />

    <MyProfileChangePassword />
  </div>
</template>

<script lang="ts" setup>
import SaGoogleDriveIntegrationSetup from '@/components/documents/storage/SaGoogleDriveIntegrationSetup.vue';
import MyProfileChangePassword from '@/pages/my-profile/MyProfileChangePassword.vue';
import MyProfileDocumentsStorageConfig from '@/pages/my-profile/MyProfileDocumentsStorageConfig.vue';
import MyProfileLanguagePreferences from '@/pages/my-profile/MyProfileLanguagePreferences.vue';
import { type ProfileDto, useAuth } from '@/services/api';
import { graphql } from '@/services/api/gql';
import { useQuery } from '@/services/api/use-gql-api.ts';
import { $t } from '@/services/i18n';

const { isAdmin } = useAuth();

const [loading, profile] = useQuery(
  graphql(/* GraphQL */ `
    query userProfile {
      userProfile {
        documentsStorage
        i18n {
          language
          locale
        }
        userName
      }
    }
  `),
  'userProfile',
);

const onProfileUpdated = (updatedProfile: ProfileDto) => {
  profile.value = updatedProfile;
};
</script>
