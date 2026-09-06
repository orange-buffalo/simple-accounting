<template>
  <SaPage :header="$t.myProfile.pageHeader()">

    <MyProfileDocumentsStorage
      v-if="!isAdmin()"
      :profile="profile ?? undefined"
      :loading="loading"
      @profile-updated="onProfileUpdated"
    />

    <MyProfileLanguagePreferences
      :profile="profile ?? undefined"
      :loading="loading"
      @profile-updated="onProfileUpdated"
    />

    <MyProfileOAuthProviders @links-changed="onOAuthLinksChanged" />

    <MyProfileChangePassword v-if="passwordLoginAvailable" />
  </SaPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaPage from '@/components/SaPage.vue';
  import MyProfileDocumentsStorage from '@/pages/my-profile/MyProfileDocumentsStorage.vue';
  import MyProfileLanguagePreferences from '@/pages/my-profile/MyProfileLanguagePreferences.vue';
  import { useAuth } from '@/services/api';
  import MyProfileChangePassword from '@/pages/my-profile/MyProfileChangePassword.vue';
  import MyProfileOAuthProviders from '@/pages/my-profile/MyProfileOAuthProviders.vue';
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

  // the password cannot be used to login once an identity is linked, so managing it is hidden
  const passwordLoginAvailable = ref(false);
  const onOAuthLinksChanged = (hasLinkedIdentities: boolean) => {
    passwordLoginAvailable.value = !hasLinkedIdentities;
  };
</script>
