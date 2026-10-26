<template>
  <div class="login-page">
    <main class="login-page__login" aria-labelledby="login-heading">
      <h1 id="login-heading">{{ $t.loginPage.title() }}</h1>
      <p class="login-page__intro">{{ $t.loginPage.introduction() }}</p>

      <ElForm
        class="login-page__login-form"
        :model="form"
        label-position="top"
        :disabled="uiState.requestInProgress"
        @keyup.enter="onEnterPressed"
        @submit.prevent
      >
        <ElFormItem :label="$t.loginPage.userName.label()" for="login-username">
          <ElInput
            id="login-username"
            v-model="form.userName"
            name="username"
            autocomplete="username"
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
          <ElFormItem v-if="passwordLoginAvailable" :label="$t.loginPage.password.placeholder()" for="login-password">
            <ElInput
              id="login-password"
              v-model="form.password"
              type="password"
              name="password"
              autocomplete="current-password"
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

        <div v-if="uiState.loginError" class="login-page__login-error" role="alert">
          {{ uiState.loginError }}
        </div>
      </ElForm>
      <p class="login-page__hint">{{ $t.loginPage.existingAccountHint() }}</p>
    </main>
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

  .login-page {
    display: flex;
    min-height: 100vh;
    min-height: 100dvh;
    align-items: center;
    justify-content: center;
    background: $primary-grey;
    padding: 24px;
    box-sizing: border-box;

    &__login {
      width: 100%;
      max-width: 570px;
      padding: clamp(24px, 5vw, 56px);
      box-sizing: border-box;
      background: $white;
      border: 1px solid $secondary-grey;
      border-radius: 18px;
      box-shadow: 0 16px 48px rgba(20, 43, 64, 0.06);

      h1 {
        font-size: 30px;
        letter-spacing: -0.6px;
        margin: 0 0 12px;
      }
    }

    &__intro {
      color: $secondary-text-color;
      line-height: 1.6;
      margin: 0 0 32px;
    }

    &__hint {
      color: $secondary-text-color;
      text-align: center;
      font-size: 12px;
      line-height: 1.6;
      margin: 28px 0 0;
    }

    &__login-form {
      width: 100%;

      .el-button--primary {
        width: 100%;
        min-height: 46px;
        font-weight: bold;
      }

      .el-form-item__label {
        font-weight: 600;
      }

      .el-input {
        --el-input-height: 46px;
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
    }

    &__login-error {
      color: $danger-color;
      text-align: center;
      margin-top: 20px;
      line-height: 1.6;
      overflow-wrap: anywhere;
    }
  }
</style>
