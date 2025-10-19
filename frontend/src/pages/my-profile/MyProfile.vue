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
  import { onMounted, ref } from 'vue';
  import MyProfileDocumentsStorageConfig from '@/pages/my-profile/MyProfileDocumentsStorageConfig.vue';
  import MyProfileLanguagePreferences from '@/pages/my-profile/MyProfileLanguagePreferences.vue';
  import SaGoogleDriveIntegrationSetup from '@/components/documents/storage/SaGoogleDriveIntegrationSetup.vue';
  import { ProfileDto, useAuth, profileApi } from '@/services/api';
  import MyProfileChangePassword from '@/pages/my-profile/MyProfileChangePassword.vue';
  import { $t } from '@/services/i18n';

  const { isAdmin } = useAuth();

  const profile = ref<ProfileDto>({
    userName: '',
    i18n: {
      language: '',
      locale: '',
    },
  });
  const loading = ref(true);

  onMounted(async () => {
    profile.value = await profileApi.getProfile();
    loading.value = false;
  });

  const onProfileUpdated = (updatedProfile: ProfileDto) => {
    profile.value = updatedProfile;
  };
</script>
