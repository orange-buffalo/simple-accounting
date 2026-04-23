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
        :hide-icon="uiState.statusCustomIcon === undefined"
      >
        {{ uiState.statusText }}
      </SaStatusLabel>
      <div class="sa-gdrive-integration__status__details">
        <div v-if="integrationStatus.status === 'unknown'">
          {{ $t.saGoogleDriveIntegrationSetup.unknown.details() }}
        </div>
        <div v-else-if="integrationStatus.status === 'successful'">
          <SaI18n :message="$t.saGoogleDriveIntegrationSetup.successful.details()">
            <template #folderLink>
              <a
                :href="`https://drive.google.com/drive/folders/${integrationStatus.folderId}`"
                target="_blank"
                rel="noopener noreferral"
              >
                {{ integrationStatus.folderName }}
              </a>
            </template>
          </SaI18n>
        </div>
        <div v-else-if="integrationStatus.status === 'authorizationRequired'">
          <SaI18n :message="$t.saGoogleDriveIntegrationSetup.authorizationRequired.details.message()">
            <template #action>
              <br>
              <ElButton
                link
                @click="startAuthorization"
              >
                {{ $t.saGoogleDriveIntegrationSetup.authorizationRequired.details.startAction() }}
              </ElButton>
            </template>
          </SaI18n>
        </div>
        <div v-else-if="integrationStatus.status === 'authorizationInProgress'">
          {{ $t.saGoogleDriveIntegrationSetup.authorizationInProgress.details.line1() }}
          <br>
          {{ $t.saGoogleDriveIntegrationSetup.authorizationInProgress.details.line2() }}
        </div>
        <div v-else-if="integrationStatus.status === 'authorizationFailed'">
          <SaI18n :message="$t.saGoogleDriveIntegrationSetup.authorizationFailed.details.message()">
            <template #action>
              <br>
              <ElButton
                link
                @click="startAuthorization"
              >
                {{ $t.saGoogleDriveIntegrationSetup.authorizationFailed.details.retryAction() }}
              </ElButton>
            </template>
          </SaI18n>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import {
    computed, onMounted, onUnmounted, ref,
  } from 'vue';
  import SaStatusLabel, { type StatusLabelStatus } from '@/components/SaStatusLabel.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import SaI18n from '@/components/SaI18n.vue';
  import { subscribeToPushNotifications, unsubscribeFromPushNotifications } from '@/services/push-notifications';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';
  import type { GoogleDriveStorageIntegrationStatusResponse } from '@/services/api/gql/graphql';

  class IntegrationStatus {
    status: 'unknown' | 'authorizationInProgress' | 'authorizationRequired'
      | 'authorizationFailed' | 'successful' = 'unknown';

    folderId?: string;

    folderName?: string;

    authorizationUrl?: string;

    get loading() {
      return this.status === 'unknown' || this.status === 'authorizationInProgress';
    }

    onAuthorizationRequired(authorizationUrl?: string) {
      this.status = 'authorizationRequired';
      this.authorizationUrl = authorizationUrl;
    }

    onAuthorizationSuccess(folderId?: string, folderName?: string) {
      this.status = 'successful';
      this.folderId = folderId;
      this.folderName = folderName;
    }

    onAuthorizationStarted() {
      this.status = 'authorizationInProgress';
    }

    onAuthorizationFailed(authorizationUrl?: string) {
      this.status = 'authorizationFailed';
      this.authorizationUrl = authorizationUrl;
    }
  }

  const integrationStatus = ref(new IntegrationStatus());

  const loadIntegrationStatusQuery = useLazyQuery(graphql(/* GraphQL */ `
    query googleDriveStorageIntegrationStatus {
      googleDriveStorageIntegrationStatus {
        authorizationRequired
        authorizationUrl
        folderId
        folderName
      }
    }
  `), 'googleDriveStorageIntegrationStatus');

  async function loadIntegrationStatus() {
    const status = await loadIntegrationStatusQuery({});
    const {
      folderId,
      folderName,
      authorizationRequired,
      authorizationUrl,
    } = status;
    if (authorizationRequired) {
      integrationStatus.value.onAuthorizationRequired(authorizationUrl ?? undefined);
    } else {
      integrationStatus.value.onAuthorizationSuccess(folderId ?? undefined, folderName ?? undefined);
    }
  }

  loadIntegrationStatus();

  const uiState = computed(() => {
    let statusType: StatusLabelStatus = 'regular';
    let statusText = null;
    let statusCustomIcon;
    let iconName = null;
    if (integrationStatus.value.status === 'successful') {
      statusType = 'success';
      statusText = $t.value.saGoogleDriveIntegrationSetup.successful.status();
      iconName = 'success';
    } else if (integrationStatus.value.status === 'unknown') {
      statusText = $t.value.saGoogleDriveIntegrationSetup.unknown.status();
      statusCustomIcon = 'loading';
    } else if (integrationStatus.value.status === 'authorizationRequired') {
      statusText = $t.value.saGoogleDriveIntegrationSetup.authorizationRequired.status();
      statusType = 'pending';
      iconName = 'warning-circle';
    } else if (integrationStatus.value.status === 'authorizationInProgress') {
      statusText = $t.value.saGoogleDriveIntegrationSetup.authorizationInProgress.status();
      statusCustomIcon = 'loading';
    } else if (integrationStatus.value.status === 'authorizationFailed') {
      statusText = $t.value.saGoogleDriveIntegrationSetup.authorizationFailed.status();
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

  function useDriveAuthorization() {
    let gdrivePopup: Window | null;

    const startAuthorization = () => {
      const popupWidth = Math.max(screen.width / 2, 600);
      const params = [
        `height=${screen.height - 100}`,
        `width=${popupWidth}`,
      ].join(',');
      gdrivePopup = window.open(integrationStatus.value.authorizationUrl, 'popup_window', params);
      if (!gdrivePopup) throw new Error('Failed to open popup');
      gdrivePopup.moveTo((screen.width - popupWidth) / 2, 50);

      integrationStatus.value.onAuthorizationStarted();
    };

    const onGoogleDriveAuthorization = ({
      folderId,
      folderName,
      authorizationRequired,
      authorizationUrl,
    }: GoogleDriveStorageIntegrationStatusResponse) => {
      if (authorizationRequired) {
        integrationStatus.value.onAuthorizationFailed(authorizationUrl ?? undefined);
      } else {
        integrationStatus.value.onAuthorizationSuccess(folderId ?? undefined, folderName ?? undefined);
        if (gdrivePopup) {
          gdrivePopup.close();
        }
      }
    };

    // Fallback for when the WebSocket push notification is missed due to a race condition:
    // the component may mount after the push notification is emitted if Google Drive was
    // just enabled (subscription not yet established when the notification fires).
    const onOAuthPopupComplete = (event: MessageEvent) => {
      if (event.origin !== window.location.origin) return;
      if (!event.data || typeof event.data !== 'object') return;
      const message = event.data as { type?: string; success?: boolean };
      if (message.type === 'sa-oauth-complete' && message.success === true) {
        if (integrationStatus.value.status === 'authorizationInProgress') {
          loadIntegrationStatus();
        }
      }
    };

    onMounted(() => {
      subscribeToPushNotifications('storage.google-drive.auth', onGoogleDriveAuthorization);
      window.addEventListener('message', onOAuthPopupComplete);
    });
    onUnmounted(() => {
      unsubscribeFromPushNotifications('storage.google-drive.auth', onGoogleDriveAuthorization);
      window.removeEventListener('message', onOAuthPopupComplete);
    });

    return {
      startAuthorization,
    };
  }

  const { startAuthorization } = useDriveAuthorization();

</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .sa-gdrive-integration {
    display: flex;

    &__icon {
      width: 40px;
      height: 40px;
      position: relative;

      &--main {
        color: $primary-color-lighter-i;
        width: 100% !important;
        height: 100% !important;
      }

      &--sub {
        position: absolute;
        bottom: 0;
        right: 0;
        width: 20px !important;
        height: 20px !important;
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
