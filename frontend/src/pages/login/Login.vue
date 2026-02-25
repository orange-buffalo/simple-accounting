<template>
  <div class="login-page">
    <div class="login-page__signup">
      {{ $t.loginPage.announcement() }}
    </div>
    <div class="login-page__login">
      <LogoLogin class="login-page__login__logo" />

      <ElForm
        class="login-page__login-form"
        :model="form"
        label-width="0px"
        :disabled="uiState.loginInProgress"
      >
        <ElFormItem>
          <ElInput
            v-model="form.userName"
            :placeholder="$t.loginPage.userName.placeholder()"
          >
            <template #prefix>
              <SaIcon icon="login" />
            </template>
          </ElInput>
        </ElFormItem>

        <ElFormItem>
          <ElInput
            v-model="form.password"
            type="password"
            :placeholder="$t.loginPage.password.placeholder()"
          >
            <template #prefix>
              <SaIcon icon="password" />
            </template>
          </ElInput>
        </ElFormItem>

        <!--suppress HtmlDeprecatedAttribute -->
        <ElFormItem align="center">
          <ElCheckbox v-model="form.rememberMe">
            {{ $t.loginPage.rememberMe.label() }}
          </ElCheckbox>
        </ElFormItem>

        <ElButton
          type="primary"
          :disabled="!loginEnabled || uiState.loginInProgress"
          @click="executeLogin"
          data-testid="login-button"
        >
          <SaIcon icon="loading" v-if="uiState.loginInProgress" />
          <span v-else>{{ $t.loginPage.login() }}</span>
        </ElButton>

        <div class="login-page__login-error">
          {{ uiState.loginError }}
        </div>
      </ElForm>
    </div>
  </div>
</template>

<script lang="ts" setup>
  /// <reference types="vite-svg-loader" />
  import type { Ref } from 'vue';
  import {
    computed,
    reactive,
    ref,
    watch,
  } from 'vue';

  import { useWorkspaces } from '@/services/workspaces';
  import { $t, setLocaleFromProfile } from '@/services/i18n';
  import LogoLogin from '@/assets/logo-login.svg?component';
  import SaIcon from '@/components/SaIcon.vue';
  import useNavigation from '@/services/use-navigation';
  import { useAuth, profileApi, handleGqlApiBusinessError } from '@/services/api';
  import { useLastView } from '@/services/use-last-view';
  import { CreateAccessTokenByCredentialsErrorCodes } from '@/services/api/gql/graphql.ts';

  class AccountLockTimer {
    private readonly $onTimerUpdate: (remainingDurationInSec: number) => void;

    private $remainingDurationInSec: Ref<number | null>;

    private $timerRef: ReturnType<typeof setTimeout> | null;

    constructor(onTimerUpdate: (remainingDurationInSec: number) => void) {
      this.$remainingDurationInSec = ref(null);
      this.$onTimerUpdate = onTimerUpdate;
      this.$timerRef = null;
    }

    start(durationInSec: number) {
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

    // noinspection JSUnusedGlobalSymbols
    $handler() {
      if (this.$remainingDurationInSec.value == null) throw new Error('No active');
      this.$remainingDurationInSec.value -= 1;
      this.$onTimerUpdate(this.$remainingDurationInSec.value);
      if (this.$remainingDurationInSec.value === 0) {
        this.cancel();
      }
    }
  }

  interface UiState {
    loginError: string | null;
    loginInProgress: boolean,
  }

  const form = reactive({
    userName: '',
    password: '',
    rememberMe: true,
  });

  const uiState = reactive<UiState>({
    loginError: '',
    loginInProgress: false,
  });

  const accountLockTimer = new AccountLockTimer((lockDurationInSec) => {
    if (lockDurationInSec === 0) {
      uiState.loginError = null;
    } else {
      uiState.loginError = $t.value.loginPage.loginError.accountLocked(lockDurationInSec);
    }
  });

  watch(() => [form.userName, form.password], () => {
    if (form.password || form.userName) {
      uiState.loginError = null;
    }
  }, { immediate: true });

  const loginEnabled = computed(() => form.userName && form.password && !accountLockTimer.isActive());

  const onLoginError = async (processingError: unknown) => {
    const errorCode = handleGqlApiBusinessError<
      CreateAccessTokenByCredentialsErrorCodes
    >(processingError);
    if (errorCode === CreateAccessTokenByCredentialsErrorCodes.AccountLocked
      && (processingError as any).lockExpiresInSec !== undefined) {
      accountLockTimer.start(
        (processingError as any).lockExpiresInSec,
      );
    } else if (errorCode
      === CreateAccessTokenByCredentialsErrorCodes.LoginNotAvailable) {
      uiState.loginError = $t.value.loginPage.loginError.underAttack();
    } else if (errorCode
      === CreateAccessTokenByCredentialsErrorCodes.UserNotActivated) {
      uiState.loginError
        = $t.value.loginPage.loginError.userNotActivated();
    } else if (errorCode
      === CreateAccessTokenByCredentialsErrorCodes.BadCredentials) {
      uiState.loginError
        = $t.value.loginPage.loginError.generalFailure();
    } else {
      console.error('Login failure', processingError);
      uiState.loginError
        = $t.value.loginPage.loginError.generalFailure();
    }
  };

  const { navigateByViewName } = useNavigation();

  const onAdminLogin = async () => {
    await navigateByViewName('users-overview');
  };

  const onUserLogin = async () => {
    const hasAnyWorkspaces = await useWorkspaces()
      .loadWorkspaces();
    if (hasAnyWorkspaces) {
      const { lastView } = useLastView();
      if (lastView) {
        await navigateByViewName(lastView);
      } else {
        await navigateByViewName('dashboard');
      }
    } else {
      await navigateByViewName('account-setup');
    }
  };

  const emit = defineEmits<{
    (e: 'login'): void;
  }>();

  const {
    isLoggedIn,
    login,
    isAdmin,
  } = useAuth();
  if (isLoggedIn()) {
    emit('login');
  }

  const executeLogin = async () => {
    uiState.loginError = null;
    uiState.loginInProgress = true;
    accountLockTimer.cancel();
    try {
      await login({ ...form });
      const profile = await profileApi.getProfile();
      await setLocaleFromProfile(profile.i18n.locale, profile.i18n.language);

      if (isAdmin()) {
        await onAdminLogin();
      } else {
        await onUserLogin();
      }
    } catch (e: unknown) {
      await onLoginError(e);
    } finally {
      uiState.loginInProgress = false;
    }
  };
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

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

          &:after {
            border-color: $primary-color-lighter-ii !important;
          }

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
