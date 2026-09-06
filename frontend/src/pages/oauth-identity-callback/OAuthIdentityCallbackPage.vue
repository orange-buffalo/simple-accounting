<template>
  <div class="oauth-identity-callback-page">
    <LogoLogin class="oauth-identity-callback-page__logo" />

    <SaStatusLabel
      v-if="loading"
      status="regular"
      custom-icon="loading"
    >
      {{ $t.oauthIdentityCallbackPage.loading() }}
    </SaStatusLabel>

    <template v-else>
      <SaStatusLabel
        status="failure"
        class="oauth-identity-callback-page__message"
      >
        {{ errorMessage }}
      </SaStatusLabel>

      <ElButton
        class="oauth-identity-callback-page__action"
        type="primary"
        @click="navigateToRecovery"
      >
        {{ recoveryActionLabel }}
      </ElButton>
    </template>
  </div>
</template>

<script lang="ts" setup>
  /// <reference types="vite-svg-loader" />
  import { computed, ref } from 'vue';
  import LogoLogin from '@/assets/logo-login.svg?component';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api.ts';
  import { handleGqlApiBusinessError, useAuth } from '@/services/api';
  import {
    CompleteOAuthAuthenticationErrorCodes,
    OAuthAuthenticationOutcome,
  } from '@/services/api/gql/schema-types.ts';
  import useNavigation from '@/services/use-navigation';
  import useNotifications from '@/components/notifications/use-notifications.ts';
  import { useAfterLoginNavigation } from '@/pages/login/after-login-navigation.ts';
  import { useWorkspaces } from '@/services/workspaces.ts';

  const loading = ref(true);
  const errorMessage = ref('');
  const sessionRecovered = ref(false);

  const { isAdmin, loginWithAccessToken, tryAutoLogin } = useAuth();
  const { loadWorkspaces } = useWorkspaces();
  const { navigateByViewName } = useNavigation();
  const { showSuccessNotification } = useNotifications();
  const navigateAfterLogin = useAfterLoginNavigation();

  const completeOAuthAuthenticationMutation = useMutation(graphql(/* GraphQL */ `
    mutation completeOAuthAuthentication($code: String, $error: String, $state: String!) {
      completeOAuthAuthentication(code: $code, error: $error, state: $state) {
        outcome
        accessToken
      }
    }
  `), 'completeOAuthAuthentication');

  // a failed linking flow starts from an authenticated session, so the user is taken back
  // to their profile whenever the session is still available
  const recoveryActionLabel = computed(() => (sessionRecovered.value
    ? $t.value.oauthIdentityCallbackPage.backToProfile()
    : $t.value.oauthIdentityCallbackPage.backToLogin()));

  const navigateToRecovery = async () => {
    await navigateByViewName(sessionRecovered.value ? 'my-profile' : 'login');
  };

  /**
   * The session of the initiating browser may be unavailable for the very reason the callback
   * failed, so a failure to recover it must not prevent reporting the outcome to the user.
   */
  const recoverSession = async () => {
    try {
      const recovered = await tryAutoLogin();
      if (recovered && !isAdmin()) {
        await loadWorkspaces();
      }
      return recovered;
    } catch (e: unknown) {
      console.error('Failed to recover the session', e);
      return false;
    }
  };

  /**
   * This page is the end of the flow, so it has to report every outcome itself instead of
   * letting infrastructure failures propagate to the global handler.
   */
  const resolveErrorMessage = (processingError: unknown): string => {
    let errorCode: CompleteOAuthAuthenticationErrorCodes | undefined;
    try {
      errorCode = handleGqlApiBusinessError<CompleteOAuthAuthenticationErrorCodes>(processingError);
    } catch (_: unknown) {
      console.error('OAuth authentication failure', processingError);
      return $t.value.oauthIdentityCallbackPage.errors.generalFailure();
    }
    if (errorCode === CompleteOAuthAuthenticationErrorCodes.IdentityMismatch) {
      return $t.value.oauthIdentityCallbackPage.errors.identityMismatch();
    }
    if (errorCode === CompleteOAuthAuthenticationErrorCodes.IdentityAlreadyInUse) {
      return $t.value.oauthIdentityCallbackPage.errors.identityAlreadyInUse();
    }
    if (errorCode === CompleteOAuthAuthenticationErrorCodes.UnknownAuthorizationRequest) {
      return $t.value.oauthIdentityCallbackPage.errors.unknownRequest();
    }
    return $t.value.oauthIdentityCallbackPage.errors.generalFailure();
  };

  const onFailure = async (processingError: unknown) => {
    errorMessage.value = resolveErrorMessage(processingError);
    sessionRecovered.value = await recoverSession();
  };

  const executeCallback = async () => {
    const params = new URLSearchParams(window.location.search);
    try {
      const result = await completeOAuthAuthenticationMutation({
        code: params.get('code') || null,
        error: params.get('error') || null,
        state: params.get('state') || '',
      });

      if (result.outcome === OAuthAuthenticationOutcome.Link) {
        // linking never grants a session, so the one of the initiating browser is restored
        if (await recoverSession()) {
          await navigateByViewName('my-profile');
          showSuccessNotification($t.value.oauthIdentityCallbackPage.linked());
        } else {
          await navigateByViewName('login');
        }
        return;
      }

      loginWithAccessToken(result.accessToken!);
      await navigateAfterLogin();
    } catch (e: unknown) {
      await onFailure(e);
    } finally {
      // the page must never be left in the loading state, whatever the outcome
      loading.value = false;
    }
  };

  // noinspection JSIgnoredPromiseFromCall
  executeCallback();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .oauth-identity-callback-page {
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

    &__message {
      padding: 10px;
    }

    &__action {
      margin-top: 20px;
    }
  }
</style>
