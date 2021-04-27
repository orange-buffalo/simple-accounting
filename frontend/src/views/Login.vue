<template>
  <div class="login-page">
    <div class="login-page__signup">
      {{ $t('loginPage.announcement') }}
    </div>
    <div class="login-page__login">
      <LogoLogin class="login-page__login__logo" />

      <ElForm
        ref="form"
        class="login-page__login-form"
        :model="form"
        label-width="0px"
      >
        <ElFormItem prop="userName">
          <ElInput
            v-model="form.userName"
            :placeholder="$t('loginPage.userName.placeholder')"
          >
            <SaIcon
              slot="prefix"
              icon="login"
            />
          </ElInput>
        </ElFormItem>

        <ElFormItem prop="password">
          <ElInput
            v-model="form.password"
            type="password"
            :placeholder="$t('loginPage.password.placeholder')"
          >
            <SaIcon
              slot="prefix"
              icon="password"
            />
          </ElInput>
        </ElFormItem>

        <!--suppress HtmlDeprecatedAttribute -->
        <ElFormItem
          prop="rememberMe"
          align="center"
        >
          <ElCheckbox v-model="form.rememberMe">
            {{ $t('loginPage.rememberMe.label') }}
          </ElCheckbox>
        </ElFormItem>

        <ElButton
          type="primary"
          :disabled="!loginEnabled"
          @click="executeLogin"
        >
          <i
            v-if="loginInProgress"
            class="el-icon-loading"
          />
          <span v-else>{{ $t('loginPage.login') }}</span>
        </ElButton>

        <div class="login-page__login-error">
          {{ loginError }}
        </div>
      </ElForm>
    </div>
  </div>
</template>

<script>
  import {
    computed,
    reactive,
    ref,
    toRefs,
    watch,
  } from '@vue/composition-api';
  import { initWorkspace } from '@/services/workspaces-service';
  import { userApi } from '@/services/user-api';
  import { app } from '@/services/app-services';
  import i18n from '@/services/i18n';
  import LogoLogin from '@/assets/logo-login.svg';
  import SaIcon from '@/components/SaIcon';
  import useNavigation from '@/components/navigation/useNavigation';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import { useAuth } from '@/services/api';

  class AccountLockTimer {
    constructor(onTimerUpdate) {
      this.$remainingDurationInSec = ref(null);
      this.$onTimerUpdate = onTimerUpdate;
      this.$timerRef = null;
    }

    start(durationInSec) {
      this.$onTimerUpdate(durationInSec);
      this.$timerRef = setInterval(() => this.$handler(), 1000);
      this.$remainingDurationInSec.value = durationInSec;
    }

    isActive() {
      return this.$remainingDurationInSec.value != null;
    }

    cancel() {
      if (this.$timerRef) {
        clearInterval(this.$timerRef);
        this.$remainingDurationInSec.value = null;
      }
    }

    $handler() {
      this.$remainingDurationInSec.value -= 1;
      this.$onTimerUpdate(this.$remainingDurationInSec.value);
      if (this.$remainingDurationInSec.value === 0) {
        this.cancel();
      }
    }
  }

  export default {
    components: {
      SaIcon,
      LogoLogin,
    },

    setup(props, { emit }) {
      const form = reactive({
        userName: '',
        password: '',
        rememberMe: true,
      });

      const uiState = reactive({
        loginError: '',
        loginInProgress: false,
      });

      const accountLockTimer = new AccountLockTimer((lockDurationInSec) => {
        if (lockDurationInSec === 0) {
          uiState.loginError = null;
        } else {
          uiState.loginError = i18n.t('loginPage.loginError.accountLocked', [lockDurationInSec]);
        }
      });

      watch(() => [form.userName, form.password], () => {
        if (form.password || form.userName) {
          uiState.loginError = null;
        }
      }, { immediate: true });

      const loginEnabled = computed(() => form.userName && form.password && !accountLockTimer.isActive());

      const onLoginError = (apiResponse) => {
        if (apiResponse && apiResponse.error === 'AccountLocked') {
          accountLockTimer.start(apiResponse.lockExpiresInSec);
        } else if (apiResponse && apiResponse.error === 'LoginNotAvailable') {
          uiState.loginError = i18n.t('loginPage.loginError.underAttack');
        } else {
          uiState.loginError = i18n.t('loginPage.loginError.generalFailure');
        }
      };

      const { navigateByViewName } = useNavigation();

      const onAdminLogin = async () => {
        await navigateByViewName('users-overview');
      };

      const onUserLogin = async () => {
        await initWorkspace();

        const { currentWorkspace } = useCurrentWorkspace();

        if (!currentWorkspace) {
          await navigateByViewName('workspace-setup');
        } else if (app.store.state.app.lastView) {
          await navigateByViewName(app.store.state.app.lastView);
        } else {
          await navigateByViewName('dashboard');
        }
      };

      const { isLoggedIn, login, isAdmin } = useAuth();
      if (isLoggedIn) {
        emit('login');
      }

      const executeLogin = async () => {
        uiState.loginError = null;
        uiState.loginInProgress = true;
        accountLockTimer.cancel();
        try {
          await login({ ...form });
          const profile = await userApi.getProfile();
          await app.i18n.setLocaleFromProfile(profile.i18n);

          if (isAdmin()) {
            await onAdminLogin();
          } else {
            await onUserLogin();
          }
        } catch ({ response: { data } }) {
          onLoginError(data);
        } finally {
          uiState.loginInProgress = false;
        }
      };

      return {
        form,
        ...toRefs(uiState),
        executeLogin,
        loginEnabled,
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";
  @import "~@/styles/mixins.scss";

  .login-page {
    display: flex;
    height: 100vh;

    &__signup {
      @include gradient-background;
      width: 30%;
      display: flex;
      align-items: center;
      justify-content: space-around;
      color: white;
      font-size: 110%;
      padding: 10px;
      box-sizing: border-box;
    }

    &__login {
      width: 70%;
      background-color: white;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      &__logo {
        height: 150px;
        width: 150px;
        margin-bottom: 40px;
      }
    }

    &__login-form {
      width: 80%;

      @include respond-above-starting-with(lg) {
        width: 40%;
      }

      .el-button--primary {
        background: $primary-color-lighter-iii;
        border-color: $primary-color-lighter-iii;
        transition: all 0.25s;
        width: 100%;
        padding: 15px;
        text-transform: uppercase;
        font-weight: bold;

        &:hover, &:focus {
          background: $primary-color-lighter-ii;
          border-color: $primary-color-lighter-ii;
        }

        &.is-disabled {
          background: white !important;
          color: $primary-color-lighter-iii !important;
          border-color: $primary-color-lighter-iii !important;
          cursor: inherit !important;
        }
      }

      .el-checkbox {
        &__label {
          color: $primary-color-lighter-ii !important;
        }

        &__inner {
          background-color: white !important;
          border-color: $primary-color-lighter-ii !important;

          &::after {
            border-color: $primary-color-lighter-ii;
          }
        }
      }

      .el-input {
        &__inner {
          border-color: $primary-color-lighter-iii;

          &:focus {
            border-color: $primary-color-lighter-iii;
          }
        }

        &__prefix {
          display: inline-flex;
          align-items: center;
        }

        .sa-icon {
          margin-left: 5px;
        }
      }
    }

    &__login-error {
      height: 3pt;
      color: $danger-color;
      text-align: center;
      margin-top: 20px;
    }
  }
</style>
