<template>
  <div>
    <div class="sa-page-header">
      <h1>My Profile</h1>
    </div>

    <SaForm
      v-if="loaded"
      :model="profile"
      ref="formRef"
    >
      <div>
        <!-- todo #204: space is not even -->
        <h2>Documents Storage</h2>

        <MyProfileDocumentsStorageConfig
          storage-name="Google Drive"
          storage-id="google-drive"
          :user-documents-storage="profile.documentsStorage"
          @storage-enabled="onStorageEnabled"
          @storage-disabled="onStorageDisabled"
        >
          <SaGoogleDriveIntegrationSetup />
        </MyProfileDocumentsStorageConfig>
      </div>

      <MyProfileLanguagePreferences
        :language="profile.i18n.language"
        @update:language="updateLanguage"
        :locale="profile.i18n.locale"
        @update:locale="updateLocale"
      />
    </SaForm>

    <MyProfileChangePassword />
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import MyProfileDocumentsStorageConfig from '@/pages/my-profile/MyProfileDocumentsStorageConfig.vue';
  import MyProfileLanguagePreferences from '@/pages/my-profile/MyProfileLanguagePreferences.vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaGoogleDriveIntegrationSetup from '@/components/documents/storage/SaGoogleDriveIntegrationSetup.vue';
  import type { ProfileDto } from '@/services/api';
  import { profileApi } from '@/services/api';
  import { useForm } from '@/components/form/use-form';
  import MyProfileChangePassword from '@/pages/my-profile/MyProfileChangePassword.vue';

  const profile = ref<ProfileDto>({
    userName: '',
    i18n: {
      language: '',
      locale: '',
    },
  });
  const loaded = ref(false);

  const updateProfile = async () => {
    await profileApi.updateProfile({
      updateProfileRequestDto: profile.value,
    });
  };

  const updateLanguage = async (language: string) => {
    profile.value.i18n.language = language;
    await updateProfile();
  };

  const updateLocale = async (locale: string) => {
    profile.value.i18n.locale = locale;
    await updateProfile();
  };

  const { formRef } = useForm(async () => {
    profile.value = await profileApi.getProfile();
    loaded.value = true;
  }, async () => {
    // no op
  });

  const onStorageEnabled = async (storageId: string) => {
    profile.value.documentsStorage = storageId;
    await updateProfile();
  };

  const onStorageDisabled = async () => {
    profile.value.documentsStorage = undefined;
    await updateProfile();
  };
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
