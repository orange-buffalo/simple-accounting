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

    <SaStatusLabel
      v-else
      status="failure"
      class="oauth-callback-page__message"
    >
      <div>
        <b>Authorization failed. Please try again or contact us.</b>
        <br />
        <span v-if="errorId">Error reference is '{{ errorId }}'</span>
      </div>
    </SaStatusLabel>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import LogoLogin from '@/assets/logo-login.svg?component';
import SaStatusLabel from '@/components/SaStatusLabel.vue';
import type { ErrorResponse } from '@/services/api';
import { oAuth2CallbackApi } from '@/services/api';
import { ApiBusinessError } from '@/services/api/api-errors.ts';

const loading = ref(true);
const errorId = ref<string | undefined>();
const success = ref(false);

async function executeCallback() {
  const params = new URLSearchParams(window.location.search);

  const code: string | undefined = params.get('code') || undefined;
  const error: string | undefined = params.get('error') || undefined;
  const state: string | undefined = params.get('state') || '';
  try {
    await oAuth2CallbackApi.authCallback({
      oAuth2AuthorizationCallbackRequest: {
        code,
        error,
        state,
      },
    });
    success.value = true;
  } catch (e: unknown) {
    if (e instanceof ApiBusinessError) {
      // TODO #1209: proper typing here, use e.errorAs
      const body = e.error as unknown as ErrorResponse;
      errorId.value = body.errorId;
    } else {
      throw e;
    }
  } finally {
    loading.value = false;
  }
}

executeCallback();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

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

    &__message {
      padding: 10px;

      div {
        padding-left: 5px;
      }
    }
  }
</style>
