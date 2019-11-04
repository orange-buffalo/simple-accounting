<template>
  <div>
    <span>{{ integrationStatus }}</span><br>
    <template v-if="status.folderId">
      <a
        :href="`https://drive.google.com/drive/folders/${status.folderId}`"
        target="_blank"
      >
        {{ status.folderName }}
      </a><br>
    </template>
    <ElButton @click="initGoogleDriveAuthorization">
      Connect to Google Drive
    </ElButton>
  </div>
</template>

<script>
  import { pushNotifications } from '@/services/push-notifications';
  import { api } from '@/services/api';
  import { withMediumDateTimeFormatter } from '@/components/mixins/with-medium-datetime-formatter';

  export default {
    name: 'GoogleDrive',

    mixins: [withMediumDateTimeFormatter],

    data() {
      return {
        status: {},
      };
    },

    computed: {
      integrationStatus() {
        if (this.status.folderId) {
          return 'Authorization successfully completed.';
        }
        return 'Authorization required';
      },
    },

    async created() {
      pushNotifications.subscribe('storage.google-drive.auth', this.onGoogleDriveAuthorization);
      const statusResponse = await api.get('/storage/google-drive/status');
      this.status = statusResponse.data;
    },

    destroyed() {
      pushNotifications.unsubscribe('storage.google-drive.auth', this.onGoogleDriveAuthorization);
    },

    methods: {
      onGoogleDriveAuthorization(data) {
        this.status = data;
        if (this.gdrivePopup) {
          this.gdrivePopup.close();
        }
      },

      async initGoogleDriveAuthorization() {
        if (this.status.authorizationUrl) {
          const popupWidth = Math.max(screen.width / 2, 600);
          const params = [
            `height=${screen.height - 100}`,
            `width=${popupWidth}`,
          ].join(',');
          this.gdrivePopup = window.open(this.status.authorizationUrl, 'popup_window', params);
          this.gdrivePopup.moveTo((screen.width - popupWidth) / 2, 50);
        }
      },
    },
  };
</script>
