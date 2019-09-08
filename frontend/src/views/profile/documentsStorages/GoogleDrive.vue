<template>
  <div>
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
  import {pushNotifications} from '@/services/push-notifications'
  import {api} from '@/services/api'
  import {withMediumDateTimeFormatter} from '@/components/mixins/with-medium-datetime-formatter'

  export default {
    name: 'GoogleDrive',

    mixins: [withMediumDateTimeFormatter],

    data: function () {
      return {
        status: {}
      }
    },

    created: async function () {
      pushNotifications.subscribe('storage.google-drive.auth', this.onGoogleDriveAuthorization)
      let statusResponse = await api.get('/storage/google-drive/status')
      this.status = statusResponse.data
    },

    destroyed: function () {
      pushNotifications.unsubscribe('storage.google-drive.auth', this.onGoogleDriveAuthorization)
    },

    computed: {
      integrationStatus: function () {
        if (this.status.folderId) {
          return `Authorization successfully completed.`
        } else {
          return "Authorization required"
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
        if (this.status.authorizationUrl) {
          let popupWidth = Math.max(screen.width / 2, 600)
          let params = [
            'height=' + (screen.height - 100),
            'width=' + popupWidth
          ].join(',')
          this.gdrivePopup = window.open(this.status.authorizationUrl, 'popup_window', params)
          this.gdrivePopup.moveTo((screen.width - popupWidth) / 2, 50);
        }
      }
    }
  }
</script>


