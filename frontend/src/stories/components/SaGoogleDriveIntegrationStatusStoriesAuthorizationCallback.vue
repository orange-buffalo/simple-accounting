<template>
  <div>
    <SaGoogleDriveIntegrationStatus />
    <br>
    <br>
    <ElButton @click="sendSuccessEvent">
      Send Success Event
    </ElButton>
    <ElButton @click="sendFailureEvent">
      Send Failure Event
    </ElButton>
  </div>
</template>

<script>
  import SaGoogleDriveIntegrationStatus from '@/components/documents/storage/SaGoogleDriveIntegrationStatus';
  import pushNotifications from '@/services/push-notifications';

  export default {
    components: {
      SaGoogleDriveIntegrationStatus,
    },

    setup() {
      return {
        sendSuccessEvent() {
          pushNotifications.pushEvent('storage.google-drive.auth', {
            folderId: 'folderId',
            folderName: 'simple-accounting',
          });
        },

        sendFailureEvent() {
          pushNotifications.pushEvent('storage.google-drive.auth', {
            authorizationRequired: true,
            authorizationUrl: 'https://google.com',
          });
        },
      };
    },
  };
</script>
