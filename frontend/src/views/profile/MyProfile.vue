<template>
  <div>
    <div class="sa-page-header">
      <h1>My Profile</h1>
    </div>

    <div class="sa-form">
      <div>
        <h2>Documents Storage</h2>

        <documents-storage-config storage-name="Google Drive"
                                  storage-id="google-drive"
                                  :user-documents-storage="profile.documentsStorage"
                                  @storage-enabled="onStorageEnabled"
                                  @storage-disabled="onStorageDisabled">
          <google-drive/>
        </documents-storage-config>
      </div>
    </div>

  </div>
</template>

<script>
  import GoogleDrive from '@/views/profile/documentsStorages/GoogleDrive'
  import DocumentsStorageConfig from '@/views/profile/documentsStorages/DocumentsStorageConfig'
  import {api} from '@/services/api'

  export default {
    name: 'MyProfile',

    components: {
      DocumentsStorageConfig,
      GoogleDrive
    },

    data: function () {
      return {
        profile: {
          documentsStorage: null,
          userName: null
        }
      }
    },

    created: async function () {
      let profileResponse = await api.get(`/profile`);
      this.profile = profileResponse.data;
    },

    methods: {
      onStorageEnabled: function (storageId) {
        this.profile.documentsStorage = storageId
        this._updateProfile()
      },

      onStorageDisabled: function () {
        this.profile.documentsStorage = null
        this._updateProfile()
      },

      _updateProfile: function () {
        api.put("/profile", this.profile)
      }
    }
  }
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

