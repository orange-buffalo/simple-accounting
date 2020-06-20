<template>
  <div class="oauth-callback-page">
    <LogoLogin class="oauth-callback-page__logo" />
    <SaStatusLabel
      v-if="loading"
      status="regular"
      custom-icon="loading"
    >
      We are completing the authorization.. Please hold on.
    </SaStatusLabel>

    <SaStatusLabel
      v-else-if="success"
      status="success"
    >
      Authorization successfully completed! You can close this window now.
    </SaStatusLabel>

    <ElAlert
      v-else
      type="error"
      :closable="false"
    >
      <template #title>
        <SaIcon icon="error" />
        Authorization failed. Please try again or contact us.
      </template>
      <template #default>
        Error reference is '{{ errorId }}'
      </template>
    </ElAlert>
  </div>
</template>

<script>
  import { ref } from '@vue/composition-api';
  import { api } from '@/services/api';
  import LogoLogin from '@/assets/logo-login.svg';
  import SaIcon from '@/components/SaIcon';
  import SaStatusLabel from '@/components/SaStatusLabel';

  function useCallbackApi() {
    const loading = ref(true);
    const errorId = ref(null);
    const success = ref(false);

    async function executeCallback() {
      const params = new URLSearchParams(window.location.search);
      const request = {
        error: params.get('error'),
        state: params.get('state'),
        code: params.get('code'),
      };
      try {
        await api.post('/auth/oauth2/callback', request);
        success.value = true;
      } catch (error) {
        if (error.response) {
          errorId.value = error.response.data.errorId;
        } else {
          errorId.value = '<unknown>';
        }
      } finally {
        loading.value = false;
      }
    }

    executeCallback();

    return {
      loading,
      errorId,
      success,
    };
  }

  export default {
    components: {
      SaStatusLabel,
      SaIcon,
      LogoLogin,
    },

    setup() {
      return {
        ...useCallbackApi(),
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";
  @import "~@/styles/mixins.scss";

  .oauth-callback-page {
    display: flex;
    height: 100vh;
    width: 100%;
    background-color: white;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    &__logo {
      height: 150px;
      width: 150px;
      margin-bottom: 40px;
    }

    .el-alert {
      display: inline-flex;
      width: auto;
    }
  }
</style>
