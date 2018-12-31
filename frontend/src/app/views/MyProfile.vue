<template>
  <div>
    <div class="sa-page-header">
      <h1>My Profile</h1>

      <div class="sa-header-options">
        <span>&nbsp;</span>
      </div>
    </div>

    <span>{{integrationStatus}}</span><br/>
    <template v-if="status.folderId">
      <a :href="`https://drive.google.com/drive/folders/${status.folderId}`"
         target="_blank">
        {{status.folderName}}
      </a><br/>
    </template>
    <el-button @click="initGoogleDriveAuthorization">Connect to Google Drive</el-button>
  </div>
</template>

<script>
  import {pushNotifications} from '@/app/services/push-notifications'
  import {api} from '@/services/api'
  import {withMediumDateTimeFormatter} from '@/app/components/mixins/with-medium-datetime-formatter'

  export default {
    name: 'MyProfile',

    mixins: [withMediumDateTimeFormatter],

    data: function () {
      return {
        status: {}
      }
    },

    created: async function () {
      pushNotifications.subscribe('storage.google-drive.auth', this.onGoogleDriveAuthorization)
      let statusResponse = await api.get('/user/storage/google-drive/status')
      this.status = statusResponse.data
    },

    destroyed: function () {
      pushNotifications.unsubscribe('storage.google-drive.auth', this.onGoogleDriveAuthorization)
    },

    computed: {
      integrationStatus: function () {
        if (this.status.timeAuthSucceeded) {
          return `Integration set up on ${this.mediumDateTimeFormatter(new Date(this.status.timeAuthSucceeded))}`
        } else if (this.status.timeAuthFailed) {
          return `Integration failed on ${this.mediumDateTimeFormatter(new Date(this.status.timeAuthFailed))}`
        } else if (this.status.timeAuthRequested) {
          return `Integration requested on ${this.mediumDateTimeFormatter(new Date(this.status.timeAuthRequested))} but not finished`
        } else {
          return "Not yet started"
        }
      }
    },

    methods: {
      onGoogleDriveAuthorization: function (data) {
        this.status = data
        if (this.gdrivePopup) {
          this.gdrivePopup.close();
        }
      },

      initGoogleDriveAuthorization: async function () {
        let authUrlResponse = await api.get('/user/storage/google-drive/auth')
        let authUrl = authUrlResponse.data
        if (authUrl) {
          let popupWidth = Math.max(screen.width / 2, 600)
          let params = [
            'height=' + (screen.height - 100),
            'width=' + popupWidth
          ].join(',')
          this.gdrivePopup = window.open(authUrlResponse.data, 'popup_window', params)
          this.gdrivePopup.moveTo((screen.width - popupWidth) / 2, 50);
        }
      }
    }
  }
</script>


