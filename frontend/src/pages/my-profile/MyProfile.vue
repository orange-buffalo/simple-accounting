<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.myProfile.pageHeader() }}</h1>
    </div>

    <MyProfileDocumentsStorage
      v-if="!isAdmin()"
      :profile="profile"
      :loading="loading"
      @profile-updated="onProfileUpdated"
    />

    <MyProfileLanguagePreferences
      :profile="profile"
      :loading="loading"
      @profile-updated="onProfileUpdated"
    />

    <MyProfileChangePassword />
  </div>
</template>

<script lang="ts" setup>
  import MyProfileDocumentsStorage from '@/pages/my-profile/MyProfileDocumentsStorage.vue';
  import MyProfileLanguagePreferences from '@/pages/my-profile/MyProfileLanguagePreferences.vue';
  import { useAuth } from '@/services/api';
  import MyProfileChangePassword from '@/pages/my-profile/MyProfileChangePassword.vue';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useQuery } from '@/services/api/use-gql-api.ts';
  import type { UserProfileQuery } from '@/services/api/gql/graphql.ts';

  const { isAdmin } = useAuth();

  const [loading, profile] = useQuery(graphql(/* GraphQL */ `
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
  `), 'userProfile');

  const onProfileUpdated = (updatedProfile: UserProfileQuery['userProfile']) => {
    profile.value = updatedProfile;
  };
</script>
