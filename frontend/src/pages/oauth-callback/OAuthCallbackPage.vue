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
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api.ts';

  const loading = ref(true);
  const success = ref(false);
  const errorId = ref<string | undefined>();

  const completeOAuth2FlowMutation = useMutation(graphql(/* GraphQL */ `
    mutation completeOAuth2Flow($code: String, $error: String, $state: String!) {
      completeOAuth2Flow(code: $code, error: $error, state: $state) {
        success
        errorId
      }
    }
  `), 'completeOAuth2Flow');

  async function executeCallback() {
    const params = new URLSearchParams(window.location.search);

    const code: string | undefined = params.get('code') || undefined;
    const error: string | undefined = params.get('error') || undefined;
    const state: string = params.get('state') || '';
    const result = await completeOAuth2FlowMutation({ code, error, state });
    if (result.success) {
      success.value = true;
    } else {
      errorId.value = result.errorId ?? undefined;
    }
    loading.value = false;
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
