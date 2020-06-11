<template>
  <div class="sa-gdrive-integration">
    <div class="sa-gdrive-integration__icon">
      <SaIcon
        icon="google-drive"
        class="sa-gdrive-integration__icon--main"
      />
      <SaIcon
        v-if="uiState.iconName != null"
        :icon="uiState.iconName"
        class="sa-gdrive-integration__icon--sub"
      />
    </div>
    <div class="sa-gdrive-integration__status">
      <SaStatusLabel
        :status="uiState.statusType"
        :custom-icon="uiState.statusCustomIcon"
        :hide-icon="uiState.statusCustomIcon == null"
      >
        {{ $t(uiState.statusText) }}
      </SaStatusLabel>
      <div class="sa-gdrive-integration__status__details">
        <div v-if="integrationStatus.unknown">
          {{ $t('saGoogleDriveIntegrationStatus.unknown.details') }}
        </div>
        <div v-else-if="integrationStatus.successful">
          <i18n :tag="false" path="saGoogleDriveIntegrationStatus.successful.details">
            <template #folderLink>
              <a :href="`https://drive.google.com/drive/folders/${integrationStatus.folderId}`"
                 target="_blank"
              >{{ integrationStatus.folderName }}</a>
            </template>
          </i18n>
        </div>
        <div v-else-if="integrationStatus.authorizationRequired">
          <i18n :tag="false" path="saGoogleDriveIntegrationStatus.authorizationRequired.details.message">
            <template #action>
              <br />
              <ElButton type="text" @click="startAuthorization">
                {{ $t('saGoogleDriveIntegrationStatus.authorizationRequired.details.startAction') }}
              </ElButton>
            </template>
          </i18n>
        </div>
        <div v-else-if="integrationStatus.authorizationInProgress">
          {{ $t('saGoogleDriveIntegrationStatus.authorizationInProgress.details.line1') }}<br />
          {{ $t('saGoogleDriveIntegrationStatus.authorizationInProgress.details.line2') }}
        </div>
        <div v-else-if="integrationStatus.authorizationFailed">
          <i18n :tag="false" path="saGoogleDriveIntegrationStatus.authorizationFailed.details.message">
            <template #action>
              <br />
              <ElButton type="text" @click="startAuthorization">
                {{ $t('saGoogleDriveIntegrationStatus.authorizationFailed.details.retryAction') }}
              </ElButton>
            </template>
          </i18n>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import { computed, onMounted, onUnmounted, ref } from '@vue/composition-api';
  import { api } from '@/services/api';
  import SaStatusLabel from '@/components/SaStatusLabel';
  import SaIcon from '@/components/SaIcon';
  import pushNotifications from '@/services/push-notifications';

  class IntegrationStatus {
    constructor() {
      this.$reset();
      this.unknown = true;
    }

    $reset() {
      this.unknown = false;
      this.authorizationInProgress = false;
      this.authorizationRequired = false;
      this.authorizationFailed = false;
      this.successful = false;
      this.folderId = null;
      this.folderName = null;
      this.authorizationUrl = null;
    }

    get loading() {
      return this.unknown || this.authorizationInProgress;
    }

    onAuthorizationRequired(authorizationUrl) {
      this.$reset();
      this.authorizationRequired = true;
      this.authorizationUrl = authorizationUrl;
    }

    onAuthorizationSuccess(folderId, folderName) {
      this.$reset();
      this.successful = true;
      this.folderId = folderId;
      this.folderName = folderName;
    }

    onAuthorizationStarted() {
      this.$reset();
      this.authorizationInProgress = true;
    }

    onAuthorizationFailed(authorizationUrl) {
      this.$reset();
      this.authorizationFailed = true;
      this.authorizationUrl = authorizationUrl;
    }
  }

  async function loadIntegrationStatus(integrationStatus) {
    const response = await api.get('/storage/google-drive/status');
    const { folderId, folderName, authorizationRequired, authorizationUrl } = response.data;
    if (authorizationRequired) {
      integrationStatus.value.onAuthorizationRequired(authorizationUrl);
    } else {
      integrationStatus.value.onAuthorizationSuccess(folderId, folderName);
    }
  }

  function calculateUiState(integrationStatus) {
    let statusType = 'regular';
    let statusText = null;
    let statusCustomIcon = null;
    let iconName = null;
    if (integrationStatus.value.successful) {
      statusType = 'success';
      statusText = 'saGoogleDriveIntegrationStatus.successful.status';
      iconName = 'success';
    } else if (integrationStatus.value.unknown) {
      statusText = 'saGoogleDriveIntegrationStatus.unknown.status';
      statusCustomIcon = 'google-drive-loading';
    } else if (integrationStatus.value.authorizationRequired) {
      statusText = 'saGoogleDriveIntegrationStatus.authorizationRequired.status';
      statusType = 'pending';
      iconName = 'warning-circle';
    } else if (integrationStatus.value.authorizationInProgress) {
      statusText = 'saGoogleDriveIntegrationStatus.authorizationInProgress.status';
      statusCustomIcon = 'google-drive-loading';
    } else if (integrationStatus.value.authorizationFailed) {
      statusText = 'saGoogleDriveIntegrationStatus.authorizationFailed.status';
      statusType = 'failure';
      iconName = 'warning-circle';
    }
    return {
      statusType,
      statusText,
      statusCustomIcon,
      iconName,
    };
  }

  export default {
    components: {
      SaIcon,
      SaStatusLabel,
    },

    props: {},

    setup() {
      const integrationStatus = ref(new IntegrationStatus());
      const uiState = computed(() => calculateUiState(integrationStatus));

      let gdrivePopup;

      const startAuthorization = () => {
        const popupWidth = Math.max(screen.width / 2, 600);
        const params = [
          `height=${screen.height - 100}`,
          `width=${popupWidth}`,
        ].join(',');
        gdrivePopup = window.open(integrationStatus.value.authorizationUrl, 'popup_window', params);
        gdrivePopup.moveTo((screen.width - popupWidth) / 2, 50);

        integrationStatus.value.onAuthorizationStarted();
      };

      loadIntegrationStatus(integrationStatus);

      const onGoogleDriveAuthorization = (data) => {
        const { folderId, folderName, authorizationRequired, authorizationUrl } = data;
        if (authorizationRequired) {
          integrationStatus.value.onAuthorizationFailed(authorizationUrl);
        } else {
          integrationStatus.value.onAuthorizationSuccess(folderId, folderName);
          if (gdrivePopup) {
            gdrivePopup.close();
          }
        }
      };

      onMounted(() => pushNotifications.subscribe('storage.google-drive.auth', onGoogleDriveAuthorization));
      onUnmounted(() => pushNotifications.unsubscribe('storage.google-drive.auth', onGoogleDriveAuthorization));

      return {
        integrationStatus,
        uiState,
        startAuthorization,
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";

  .sa-gdrive-integration {
    display: flex;

    &__icon {
      width: 40px;
      height: 40px;
      position: relative;

      &--main {
        color: $primary-color-lighter-i;
        width: 100%;
        height: 100%;
      }

      &--sub {
        position: absolute;
        bottom: 0;
        right: 0;
        width: 20px;
        height: 20px;
        background: white;
      }
    }

    &__status {
      margin-left: 10px;

      &__details {
        font-size: 90%;

        .el-button {
          padding: 0;
        }
      }
    }
  }
</style>
