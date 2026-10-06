<template>
  <div class="integration-wizard">
    <ElSteps :active="step" align-center finish-status="success">
      <ElStep :title="$t.integrations.wise.tokenStep()" />
      <ElStep :title="$t.integrations.wise.accountsStep()" />
      <ElStep :title="$t.integrations.wise.finishStep()" :status="saved ? 'success' : 'wait'" />
    </ElSteps>

    <div class="integration-wizard__content">
      <template v-if="step === 0">
        <ol>
          <li>{{ $t.integrations.wise.instructions.login() }}</li>
          <li>{{ $t.integrations.wise.instructions.navigate() }}</li>
          <li>{{ $t.integrations.wise.instructions.readOnly() }}</li>
          <li>{{ $t.integrations.wise.instructions.copy() }}</li>
        </ol>
        <p>
          <a href="https://docs.wise.com/guides/developer/auth-and-security/personal-api-token" target="_blank" rel="noopener noreferrer">
            {{ $t.integrations.wise.instructions.documentation() }}
          </a>
        </p>
        <p>{{ $t.integrations.wise.instructions.security() }}</p>
        <SaForm v-model="form" :on-submit="verify" :submit-button-label="$t.integrations.next()" :submit-button-disabled="!form.token?.trim()">
          <SaFormInput prop="token" type="password" :label="$t.integrations.wise.tokenLabel()" />
        </SaForm>
      </template>

      <template v-else-if="step === 1">
        <div v-if="busy" role="status" aria-live="polite">
          <SaStatusLabel status="regular" custom-icon="loading">{{ $t.integrations.wise.verifying() }}</SaStatusLabel>
        </div>
        <div v-else-if="error" role="alert" class="integration-wizard__result">
          <SaStatusLabel status="failure">{{ errorMessage }}</SaStatusLabel>
          <ElButton @click="back">{{ $t.integrations.back() }}</ElButton>
        </div>
        <template v-else>
          <p>{{ $t.integrations.wise.selectAccounts() }}</p>
          <SaStatusLabel v-if="accounts.length === 0" status="regular">{{ $t.integrations.wise.noAccounts() }}</SaStatusLabel>
          <div class="integration-cards">
            <button v-for="account in accounts" :key="accountKey(account)" type="button" class="integration-card integration-card--selectable"
                    :aria-pressed="selected.has(accountKey(account))" @click="toggle(account)">
              <strong>{{ account.profileName }}</strong>
              <span>{{ account.currency }}</span>
              <span>{{ account.name ?? $t.integrations.wise.balance() }}</span>
              <small>{{ $t.integrations.wise.accountId(account.accountId) }}</small>
              <span v-if="selected.has(accountKey(account))">✓ {{ $t.integrations.selected() }}</span>
            </button>
          </div>
          <div class="integration-wizard__actions">
            <ElButton link @click="back">{{ $t.integrations.back() }}</ElButton>
            <ElButton type="primary" :disabled="selected.size === 0" @click="save">{{ $t.integrations.next() }}</ElButton>
          </div>
        </template>
      </template>

      <div v-else class="integration-wizard__result" aria-live="polite">
        <SaStatusLabel v-if="busy" status="regular" custom-icon="loading">{{ $t.integrations.wise.saving() }}</SaStatusLabel>
        <template v-else-if="saved">
          <SaStatusLabel status="success">{{ $t.integrations.wise.success() }}</SaStatusLabel>
          <RouterLink :to="{ name: 'integrations' }">{{ $t.integrations.header() }}</RouterLink>
        </template>
        <template v-else-if="error">
          <SaStatusLabel status="failure" role="alert">{{ errorMessage }}</SaStatusLabel>
          <ElButton @click="recover">{{ $t.integrations.back() }}</ElButton>
        </template>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onUnmounted, ref } from 'vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api';
  import {
    SetupWiseIntegrationErrorCodes, VerifyWiseIntegrationTokenErrorCodes,
  } from '@/services/api/gql/schema-types';
  import type { VerifyWiseTokenMutation } from '@/services/api/gql/graphql';
  import { handleGqlApiBusinessError } from '@/services/api/api-utils';
  import { $t } from '@/services/i18n';

  const props = defineProps<{ workspaceId: string }>();
  const verifyToken = useMutation(graphql(/* GraphQL */ `
    mutation verifyWiseToken($workspaceId: String!, $token: String!) {
      verifyWiseIntegrationToken(workspaceId: $workspaceId, token: $token) {
        accounts { profileId profileName accountId currency name type }
      }
    }
  `), 'verifyWiseIntegrationToken');
  const setup = useMutation(graphql(/* GraphQL */ `
    mutation saveWiseIntegration($workspaceId: String!, $token: String!, $accounts: [WiseAccountInput!]!) {
      setupWiseIntegration(workspaceId: $workspaceId, token: $token, accounts: $accounts) { success }
    }
  `), 'setupWiseIntegration');
  type Account = VerifyWiseTokenMutation['verifyWiseIntegrationToken']['accounts'][number];
  const form = ref<{ token: string }>({ token: '' });
  const step = ref(0);
  const busy = ref(false);
  const saved = ref(false);
  const error = ref<SetupWiseIntegrationErrorCodes | VerifyWiseIntegrationTokenErrorCodes | null>(null);
  const accounts = ref<Account[]>([]);
  const selected = ref(new Set<string>());
  let disposed = false;
  onUnmounted(() => { disposed = true; form.value.token = ''; });
  const errorMessage = computed(() => {
    switch (error.value) {
    case SetupWiseIntegrationErrorCodes.InvalidToken: return $t.value.integrations.wise.invalidToken();
    case SetupWiseIntegrationErrorCodes.InvalidAccounts: return $t.value.integrations.wise.invalidAccounts();
    case SetupWiseIntegrationErrorCodes.AlreadyActive: return $t.value.integrations.wise.alreadyActive();
    default: return $t.value.integrations.wise.unavailable();
    }
  });
  const accountKey = (account: Account) => `${account.profileId}:${account.accountId}`;
  const toggle = (account: Account) => {
    const key = accountKey(account);
    if (selected.value.has(key)) selected.value.delete(key);
    else selected.value.add(key);
  };
  const back = () => { step.value = 0; error.value = null; accounts.value = []; selected.value.clear(); };
  const verify = async () => {
    if (busy.value || !form.value.token.trim()) return;
    step.value = 1;
    busy.value = true;
    error.value = VerifyWiseIntegrationTokenErrorCodes.Unavailable;
    accounts.value = [];
    selected.value.clear();
    try {
      const result = await verifyToken({ workspaceId: props.workspaceId, token: form.value.token.trim() });
      if (!disposed) { accounts.value = result.accounts; error.value = null; }
    } catch (e: unknown) {
      const code = handleGqlApiBusinessError<VerifyWiseIntegrationTokenErrorCodes>(e);
      if (!disposed) error.value = code;
    } finally { busy.value = false; }
  };
  const recover = async () => {
    if (error.value === SetupWiseIntegrationErrorCodes.InvalidToken) back();
    else await verify();
  };
  const save = async () => {
    if (busy.value || selected.value.size === 0) return;
    step.value = 2;
    busy.value = true;
    error.value = SetupWiseIntegrationErrorCodes.Unavailable;
    try {
      const result = await setup({
        workspaceId: props.workspaceId, token: form.value.token.trim(),
        accounts: accounts.value.filter(account => selected.value.has(accountKey(account)))
          .map(({ profileId, accountId, currency }) => ({ profileId, accountId, currency })),
      });
      if (!disposed) {
        saved.value = result.success;
        error.value = null;
        if (result.success) form.value.token = '';
      }
    } catch (e: unknown) {
      const code = handleGqlApiBusinessError<SetupWiseIntegrationErrorCodes>(e);
      if (!disposed) error.value = code;
    } finally { busy.value = false; }
  };
</script>

<style scoped lang="scss">
  @use "@/styles/vars.scss" as *;

  .integration-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(280px, 100%), 1fr));
    gap: 20px;
    max-width: 840px;
    margin: 24px auto;
  }

  .integration-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    min-width: 0;
    padding: 26px;
    border: 1px solid $secondary-grey;
    border-radius: 6px;
    background: $white;
    color: $primary-text-color;
    font: inherit;
    text-align: center;
    overflow-wrap: anywhere;

    &--selectable {
      cursor: pointer;
      transition: border-color 0.2s, box-shadow 0.2s, background-color 0.2s;
      &:hover, &[aria-pressed="true"] { border-color: $accent-primary-color; background: $primary-grey; }
      &:focus-visible { outline: 2px solid $accent-primary-color; outline-offset: 3px; }
    }
  }

  .integration-wizard {
    padding: 20px;
    border: 1px solid $secondary-grey;
    border-radius: 2px;
    background: $white;
    &__content { margin-top: 24px; }
    &__content li { margin-bottom: 12px; }
    &__actions, &__result { display: flex; justify-content: center; gap: 20px; margin: 24px 0; }
    &__result { flex-direction: column; align-items: center; text-align: center; }
  }
</style>
