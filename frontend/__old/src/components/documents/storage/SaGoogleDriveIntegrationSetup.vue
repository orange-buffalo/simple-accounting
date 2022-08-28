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
          {{ $t.saGoogleDriveIntegrationSetup.unknown.details() }}
        </div>
        <div v-else-if="integrationStatus.successful">
          <I18n
            :tag="false"
            path="saGoogleDriveIntegrationSetup.successful.details"
          >
            <template #folderLink>
              <a
                :href="`https://drive.google.com/drive/folders/${integrationStatus.folderId}`"
                target="_blank"
              >
                {{ integrationStatus.folderName }}
              </a>
            </template>
          </I18n>
        </div>
        <div v-else-if="integrationStatus.authorizationRequired">
          <I18n
            :tag="false"
            path="saGoogleDriveIntegrationSetup.authorizationRequired.details.message"
          >
            <template #action>
              <br>
              <ElButton
                type="text"
                @click="startAuthorization"
              >
                {{ $t.saGoogleDriveIntegrationSetup.authorizationRequired.details.startAction() }}
              </ElButton>
            </template>
          </I18n>
        </div>
        <div v-else-if="integrationStatus.authorizationInProgress">
          {{ $t.saGoogleDriveIntegrationSetup.authorizationInProgress.details.line1() }}
          <br>
          {{ $t.saGoogleDriveIntegrationSetup.authorizationInProgress.details.line2() }}
        </div>
        <div v-else-if="integrationStatus.authorizationFailed">
          <I18n
            :tag="false"
            path="saGoogleDriveIntegrationSetup.authorizationFailed.details.message"
          >
            <template #action>
              <br>
              <ElButton
                type="text"
                @click="startAuthorization"
              >
                {{ $t.saGoogleDriveIntegrationSetup.authorizationFailed.details.retryAction() }}
              </ElButton>
            </template>
          </I18n>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  /* eslint-disable no-restricted-globals */

  import {
    computed, onMounted, onUnmounted, ref,
  } from '@vue/composition-api';
  import { api } from '@/services/api-legacy';
  import SaStatusLabel from '@/components/SaStatusLabel';
  import SaIcon from '@/components/SaIcon';
  import pushNotifications from '@/services/push-notifications';

  class IntegrationStatus {
    constructor() {
      this.$reset();
      this.unknown = true;
    }

    // noinspection JSUnusedGlobalSymbols
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
    const {
      folderId, folderName, authorizationRequired, authorizationUrl,
    } = response.data;
    if (authorizationRequired) {
      integrationStatus.value.onAuthorizationRequired(authorizationUrl);
    } else {
      integrationStatus.value.onAuthorizationSuccess(folderId, folderName);
    }
  }

  function useIntegrationStatus() {
    const integrationStatus = ref(new IntegrationStatus());
    loadIntegrationStatus(integrationStatus);
    return integrationStatus;
  }

  function useUiState(integrationStatus) {
    return computed(() => {
      let statusType = 'regular';
      let statusText = null;
      let statusCustomIcon = null;
      let iconName = null;
      if (integrationStatus.value.successful) {
        statusType = 'success';
        statusText = 'saGoogleDriveIntegrationSetup.successful.status';
        iconName = 'success';
      } else if (integrationStatus.value.unknown) {
        statusText = 'saGoogleDriveIntegrationSetup.unknown.status';
        statusCustomIcon = 'loading';
      } else if (integrationStatus.value.authorizationRequired) {
        statusText = 'saGoogleDriveIntegrationSetup.authorizationRequired.status';
        statusType = 'pending';
        iconName = 'warning-circle';
      } else if (integrationStatus.value.authorizationInProgress) {
        statusText = 'saGoogleDriveIntegrationSetup.authorizationInProgress.status';
        statusCustomIcon = 'loading';
      } else if (integrationStatus.value.authorizationFailed) {
        statusText = 'saGoogleDriveIntegrationSetup.authorizationFailed.status';
        statusType = 'failure';
        iconName = 'warning-circle';
      }
      return {
        statusType,
        statusText,
        statusCustomIcon,
        iconName,
      };
    });
  }

  function useDriveAuthorization(integrationStatus) {
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

    const onGoogleDriveAuthorization = ({
      folderId, folderName, authorizationRequired, authorizationUrl,
    }) => {
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
      startAuthorization,
    };
  }

  export default {
    components: {
      SaIcon,
      SaStatusLabel,
    },

    setup() {
      const integrationStatus = useIntegrationStatus();
      const uiState = useUiState(integrationStatus);
      const { startAuthorization } = useDriveAuthorization(integrationStatus);
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
