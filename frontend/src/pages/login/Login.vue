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
        :disabled="uiState.requestInProgress"
        @keyup.enter="onEnterPressed"
        @submit.prevent
      >
        <ElFormItem>
          <ElInput
            v-model="form.userName"
            :placeholder="$t.loginPage.userName.placeholder()"
            :disabled="uiState.requestInProgress || !userNameStep"
          >
            <template #prefix>
              <SaIcon icon="login" />
            </template>
          </ElInput>
        </ElFormItem>

        <ElButton
          v-if="userNameStep"
          type="primary"
          :disabled="!userNameProvided || uiState.requestInProgress"
          @click="resolveAuthenticationMethods"
          data-testid="continue-button"
        >
          <SaIcon icon="loading" v-if="uiState.requestInProgress" />
          <span v-else>{{ $t.loginPage.continueAction() }}</span>
        </ElButton>

        <template v-else>
          <ElFormItem v-if="passwordLoginAvailable">
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
            v-if="passwordLoginAvailable"
            type="primary"
            :disabled="!loginEnabled || uiState.requestInProgress"
            @click="executeLogin"
            data-testid="login-button"
          >
            <SaIcon icon="loading" v-if="uiState.requestInProgress" />
            <span v-else>{{ $t.loginPage.login() }}</span>
          </ElButton>

          <ElButton
            v-for="provider in oauthProviders"
            :key="provider.providerId"
            type="primary"
            class="login-page__oauth-action"
            :disabled="uiState.requestInProgress"
            @click="executeOAuthLogin(provider.providerId)"
          >
            {{ $t.loginPage.continueWithProvider(provider.providerName ?? '') }}
          </ElButton>

          <ElButton
            link
            class="login-page__change-user-action"
            :disabled="uiState.requestInProgress"
            @click="backToUserNameStep"
            data-testid="change-user-button"
          >
            {{ $t.loginPage.changeUser() }}
          </ElButton>
        </template>

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

  import { $t } from '@/services/i18n';
  import LogoLogin from '@/assets/logo-login.svg?component';
  import SaIcon from '@/components/SaIcon.vue';
  import { useAuth, handleGqlApiBusinessError } from '@/services/api';
  import type { UserAuthenticationMethodsQuery } from '@/services/api/gql/graphql.ts';
  import {
    AuthenticationMethodType,
    CreateAccessTokenByCredentialsErrorCodes,
    type AccountLockedErrorExtensions,
  } from '@/services/api/gql/schema-types.ts';
  import { ApiBusinessError } from '@/services/api/api-errors.ts';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation } from '@/services/api/use-gql-api.ts';
  import { useAfterLoginNavigation } from '@/pages/login/after-login-navigation.ts';

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

  type AuthenticationMethod = UserAuthenticationMethodsQuery['userAuthenticationMethods'][0];

  interface UiState {
    loginError: string | null;
    requestInProgress: boolean,
  }

  const form = reactive({
    userName: '',
    password: '',
    rememberMe: true,
  });

  const uiState = reactive<UiState>({
    loginError: '',
    requestInProgress: false,
  });

  const authenticationMethods = ref<AuthenticationMethod[] | null>(null);
  const userNameStep = computed(() => authenticationMethods.value === null);
  const userNameProvided = computed(() => form.userName.trim().length > 0);
  const passwordLoginAvailable = computed(() => (authenticationMethods.value ?? [])
    .some((method) => method.type === AuthenticationMethodType.Password));
  const oauthProviders = computed(() => (authenticationMethods.value ?? [])
    .filter((method) => method.type === AuthenticationMethodType.Oauth));

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

  const loginEnabled = computed(() => userNameProvided.value && form.password && !accountLockTimer.isActive());

  const onLoginError = async (processingError: unknown) => {
    const errorCode = handleGqlApiBusinessError<
      CreateAccessTokenByCredentialsErrorCodes
    >(processingError);
    if (errorCode === CreateAccessTokenByCredentialsErrorCodes.AccountLocked
      && processingError instanceof ApiBusinessError) {
      const { lockExpiresInSec } = processingError
        .extensionsAs<AccountLockedErrorExtensions>();
      accountLockTimer.start(lockExpiresInSec);
    } else if (errorCode
      === CreateAccessTokenByCredentialsErrorCodes.LoginNotAvailable) {
      uiState.loginError = $t.value.loginPage.loginError.underAttack();
    } else if (errorCode
      === CreateAccessTokenByCredentialsErrorCodes.UserNotActivated) {
      uiState.loginError
        = $t.value.loginPage.loginError.userNotActivated();
    } else if (errorCode
      === CreateAccessTokenByCredentialsErrorCodes.PasswordLoginNotAllowed) {
      uiState.loginError
        = $t.value.loginPage.loginError.passwordLoginNotAllowed();
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

  const emit = defineEmits<{
    (e: 'login'): void;
  }>();

  const {
    isLoggedIn,
    login,
  } = useAuth();
  if (isLoggedIn()) {
    emit('login');
  }

  const navigateAfterLogin = useAfterLoginNavigation();

  const fetchAuthenticationMethods = useLazyQuery(graphql(/* GraphQL */ `
    query userAuthenticationMethods($userName: String!) {
      userAuthenticationMethods(userName: $userName) {
        type
        providerId
        providerName
      }
    }
  `), 'userAuthenticationMethods');

  const startOAuthLoginMutation = useMutation(graphql(/* GraphQL */ `
    mutation startOAuthLogin(
      $userName: String!
      $providerId: String!
      $issueRefreshTokenCookie: Boolean
    ) {
      startOAuthLogin(
        userName: $userName
        providerId: $providerId
        issueRefreshTokenCookie: $issueRefreshTokenCookie
      ) {
        authorizationUrl
      }
    }
  `), 'startOAuthLogin');

  const resolveAuthenticationMethods = async () => {
    if (!userNameStep.value || !userNameProvided.value || uiState.requestInProgress) return;
    uiState.loginError = null;
    uiState.requestInProgress = true;
    try {
      form.userName = form.userName.trim();
      authenticationMethods.value = await fetchAuthenticationMethods({ userName: form.userName });
    } catch (e: unknown) {
      console.error('Failed to resolve authentication methods', e);
      uiState.loginError = $t.value.loginPage.loginError.generalFailure();
    } finally {
      uiState.requestInProgress = false;
    }
  };

  const backToUserNameStep = () => {
    accountLockTimer.cancel();
    authenticationMethods.value = null;
    form.password = '';
    uiState.loginError = null;
  };

  const executeLogin = async () => {
    if (!loginEnabled.value || uiState.requestInProgress) return;
    uiState.loginError = null;
    uiState.requestInProgress = true;
    accountLockTimer.cancel();
    try {
      await login({ ...form });
      await navigateAfterLogin();
    } catch (e: unknown) {
      await onLoginError(e);
    } finally {
      uiState.requestInProgress = false;
    }
  };

  // the form has no submit button, so Enter is wired to the action of the current step
  const onEnterPressed = async () => {
    if (userNameStep.value) {
      await resolveAuthenticationMethods();
    } else if (passwordLoginAvailable.value) {
      await executeLogin();
    }
  };

  const executeOAuthLogin = async (providerId: string | null | undefined) => {
    uiState.loginError = null;
    uiState.requestInProgress = true;
    try {
      const { authorizationUrl } = await startOAuthLoginMutation({
        userName: form.userName,
        providerId: providerId ?? '',
        issueRefreshTokenCookie: form.rememberMe,
      });
      window.location.assign(authorizationUrl);
    } catch (e: unknown) {
      console.error('Failed to start OAuth login', e);
      uiState.loginError = $t.value.loginPage.loginError.generalFailure();
      uiState.requestInProgress = false;
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

    &__oauth-action {
      margin-left: 0 !important;
      margin-top: 10px;
    }

    &__change-user-action {
      width: 100%;
      margin-top: 15px;
      margin-left: 0 !important;
      color: $primary-color-lighter-ii !important;
    }

    &__login-error {
      height: 3pt;
      color: $danger-color;
      text-align: center;
      margin-top: 20px;
    }
  }
</style>
