<template>
  <div>
    <div class="sa-page-header">
      <h1>My Profile</h1>
    </div>

    <div v-if="!isAdmin()">
      <h2>{{ $t.myProfile.documentsStorage.header() }}</h2>

      <MyProfileDocumentsStorageConfig
        storage-name="Google Drive"
        storage-id="google-drive"
        :profile="profile"
        :loading="loading"
      >
        <SaGoogleDriveIntegrationSetup />
      </MyProfileDocumentsStorageConfig>
    </div>

    <MyProfileLanguagePreferences
      :profile="profile"
      :loading="loading"
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
</script>

<style lang="scss">
  .my-profile {
    &__documents-storage {
      margin-bottom: 20px;

      &__header {
        display: flex;
        align-items: center;
        margin-bottom: 10px;

        h3 {
          display: inline;
          margin: 0 0 0 10px;
        }
      }
    }
  }
</style>
