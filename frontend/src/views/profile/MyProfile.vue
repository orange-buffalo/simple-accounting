<template>
  <div>
    <div class="sa-page-header">
      <h1>My Profile</h1>
    </div>

    <SaForm
      ref="profileForm"
      :model="profile"
    >
      <template #default>
        <div>
          <!-- todo #204: space is not even -->
          <h2>Documents Storage</h2>

          <DocumentsStorageConfig
            storage-name="Google Drive"
            storage-id="google-drive"
            :user-documents-storage="profile.documentsStorage"
            @storage-enabled="onStorageEnabled"
            @storage-disabled="onStorageDisabled"
          >
            <SaGoogleDriveIntegrationSetup />
          </DocumentsStorageConfig>
        </div>

        <MyProfileLanguagePreferences
          v-if="profile.i18n"
          v-bind.sync="profile.i18n"
        />
      </template>
    </SaForm>
  </div>
</template>

<script>
  import DocumentsStorageConfig from '@/views/profile/DocumentsStorageConfig';
  import { api } from '@/services/api-legacy';
  import MyProfileLanguagePreferences from '@/views/profile/MyProfileLanguagePreferences';
  import SaForm from '@/components/SaForm';
  import SaGoogleDriveIntegrationSetup from '@/components/documents/storage/SaGoogleDriveIntegrationSetup';

  export default {
    name: 'MyProfile',

    components: {
      SaGoogleDriveIntegrationSetup,
      SaForm,
      MyProfileLanguagePreferences,
      DocumentsStorageConfig,
    },

    data() {
      return {
        profile: {
          documentsStorage: null,
          userName: null,
        },
      };
    },

    watch: {
      'profile.i18n': {
        deep: true,
        async handler(newVal, oldVal) {
          if (oldVal) {
            this.updateProfile();
          }
        },
      },
    },

    async created() {
      const profileResponse = await api.get('/profile');
      this.profile = profileResponse.data;
    },

    methods: {
      onStorageEnabled(storageId) {
        this.profile.documentsStorage = storageId;
        this.updateProfile();
      },

      onStorageDisabled() {
        this.profile.documentsStorage = null;
        this.updateProfile();
      },

      async updateProfile() {
        await api.put('/profile', this.profile);
      },
    },
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
