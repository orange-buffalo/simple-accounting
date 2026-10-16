<template>
  <SaPage :header="$t.integrations.wise.setupHeader()">
    <div class="integration-wizard">
      <div v-if="initializing" class="integration-wizard__result" role="status" aria-live="polite">
        <SaStatusLabel status="regular" custom-icon="loading">{{ $t.integrations.loading() }}</SaStatusLabel>
      </div>
      <template v-else>
        <ElSteps :active="step" align-center finish-status="success">
          <ElStep v-for="item in wizardSteps" :key="item.value" :status="item.status">
            <template #title>
              <button type="button" class="integration-wizard__step" :disabled="item.disabled"
                      :aria-current="step === item.value ? 'step' : undefined" @click="navigateStep(item.value)">
                {{ item.title }}
              </button>
            </template>
          </ElStep>
        </ElSteps>
        <div v-if="step === steps.TOKEN_INPUT" class="integration-wizard__content">
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
          <p v-if="configured">{{ $t.integrations.wise.savedToken() }}</p>
          <SaForm v-model="form" :on-submit="verify" :submit-button-label="$t.integrations.next()" :submit-button-disabled="!configured && !form.token?.trim()">
            <SaFormInput prop="token" type="password" :label="$t.integrations.wise.tokenLabel()" />
          </SaForm>
        </div>
        <template v-else-if="step === steps.ACCOUNT_SELECTION">
          <div v-if="busy" class="integration-wizard__result" role="status" aria-live="polite">
            <SaStatusLabel status="regular" custom-icon="loading">{{ $t.integrations.wise.verifying() }}</SaStatusLabel>
          </div>
          <div v-else-if="error" role="alert" class="integration-wizard__result">
            <SaStatusLabel status="failure">{{ errorMessage }}</SaStatusLabel>
            <ElButton @click="back">{{ $t.integrations.back() }}</ElButton>
          </div>
          <div v-else class="integration-wizard__content">
            <p role="status" aria-live="polite">{{ $t.integrations.wise.selectAccounts() }}</p>
            <SaStatusLabel v-if="accounts.length === 0" status="regular" role="status" aria-live="polite">
              {{ $t.integrations.wise.noAccounts() }}
            </SaStatusLabel>
            <div class="integration-cards">
              <button v-for="account in accounts" :key="accountKey(account)" type="button" class="integration-card integration-card--selectable"
                      :aria-pressed="selected.has(accountKey(account))" :disabled="account.deleted" @click="toggle(account)">
                <strong>{{ account.profileName || $t.integrations.wise.profileId(account.profileId) }}</strong>
                <span>{{ account.currency }}</span>
                <span>{{ account.name ?? $t.integrations.wise.balance() }}</span>
                <small>{{ $t.integrations.wise.accountId(account.accountId) }}</small>
                <span v-if="selected.has(accountKey(account))">✓ {{ $t.integrations.selected() }}</span>
                <ElTag v-if="account.deleted" type="danger">{{ $t.integrations.wise.deleted() }}</ElTag>
                <span v-if="account.deleted" class="integration-card__hint">{{ $t.integrations.wise.deletedHint() }}</span>
              </button>
            </div>
            <div class="integration-wizard__actions">
              <ElButton link @click="back">{{ $t.integrations.back() }}</ElButton>
              <ElButton type="primary" :disabled="selected.size === 0" @click="save">{{ $t.integrations.next() }}</ElButton>
            </div>
          </div>
        </template>
        <div v-else class="integration-wizard__result" aria-live="polite">
          <SaStatusLabel v-if="busy" status="regular" custom-icon="loading" role="status">
            {{ $t.integrations.wise.saving() }}
          </SaStatusLabel>
          <template v-else-if="saved">
            <SaStatusLabel status="success">{{ $t.integrations.wise.success() }}</SaStatusLabel>
            <RouterLink :to="{ name: 'integrations' }">{{ $t.integrations.header() }}</RouterLink>
          </template>
          <template v-else-if="error">
            <SaStatusLabel status="failure" role="alert">{{ errorMessage }}</SaStatusLabel>
            <ElButton @click="recover">{{ $t.integrations.back() }}</ElButton>
          </template>
        </div>
      </template>
    </div>
  </SaPage>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import { ElStep, ElSteps, ElTag } from 'element-plus';
  import SaPage from '@/components/SaPage.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { graphql } from '@/services/api/gql';
  import { useQuery, useMutation } from '@/services/api/use-gql-api';
  import {
    SaveWiseIntegrationSettingsErrorCodes, ListWiseAccountsErrorCodes,
  } from '@/services/api/gql/schema-types';
  import type { ListWiseAccountsMutation } from '@/services/api/gql/graphql';
  import { handleGqlApiBusinessError } from '@/services/api/api-utils';
  import { $t } from '@/services/i18n';

  const workspaceId = useCurrentWorkspace().currentWorkspace.id;
  const listAccounts = useMutation(graphql(/* GraphQL */ `
    mutation listWiseAccounts($workspaceId: String!, $token: String) {
      listWiseAccounts(workspaceId: $workspaceId, token: $token) {
        accounts { profileId profileName accountId currency name type usedByIntegration deleted }
      }
    }
  `), 'listWiseAccounts');
  const saveSettings = useMutation(graphql(/* GraphQL */ `
    mutation saveWiseIntegrationSettings($workspaceId: String!, $token: String, $accounts: [WiseAccountInput!]!) {
      saveWiseIntegrationSettings(workspaceId: $workspaceId, token: $token, accounts: $accounts) { success }
    }
  `), 'saveWiseIntegrationSettings');
  const [initializing, configuration] = useQuery(graphql(/* GraphQL */ `
    query wiseConfiguration($workspaceId: String!) {
      workspace(id: $workspaceId) { integrations { wise { active } } }
    }
  `), 'workspace', { variables: { workspaceId } });
  type Account = ListWiseAccountsMutation['listWiseAccounts']['accounts'][number];
  const form = ref<{ token: string }>({ token: '' });
  const steps = { TOKEN_INPUT: 0, ACCOUNT_SELECTION: 1, ACTIVATION: 2 };
  const step = ref(steps.TOKEN_INPUT);
  const configured = ref(false);
  const verified = ref(false);
  const verifiedToken = ref<string | null>(null);
  const busy = ref(false);
  const saved = ref(false);
  const error = ref<SaveWiseIntegrationSettingsErrorCodes | ListWiseAccountsErrorCodes | null>(null);
  const saveError = ref(false);
  const accounts = ref<Account[]>([]);
  const selected = ref(new Set<string>());
  const wizardSteps = computed(() => [
    {
      value: steps.TOKEN_INPUT,
      title: $t.value.integrations.wise.tokenStep(),
      status: verified.value ? 'success' as const
        : step.value === steps.TOKEN_INPUT ? 'process' as const : 'wait' as const,
      disabled: busy.value,
    },
    {
      value: steps.ACCOUNT_SELECTION,
      title: $t.value.integrations.wise.accountsStep(),
      status: saved.value || step.value === steps.ACTIVATION ? 'success' as const
        : step.value === steps.ACCOUNT_SELECTION ? 'process' as const : 'wait' as const,
      disabled: busy.value || !verified.value,
    },
    {
      value: steps.ACTIVATION,
      title: $t.value.integrations.wise.finishStep(),
      status: saved.value ? 'success' as const : 'wait' as const,
      disabled: busy.value || (!saved.value && !saveError.value),
    },
  ]);
  const errorMessage = computed(() => {
    switch (error.value) {
    case SaveWiseIntegrationSettingsErrorCodes.InvalidToken: return $t.value.integrations.wise.invalidToken();
    case SaveWiseIntegrationSettingsErrorCodes.InvalidAccounts: return $t.value.integrations.wise.invalidAccounts();
    default: return $t.value.integrations.wise.unavailable();
    }
  });
  const accountKey = (account: Account) => `${account.profileId}:${account.accountId}:${account.currency}`;
  const toggle = (account: Account) => {
    saved.value = false;
    const key = accountKey(account);
    if (selected.value.has(key)) selected.value.delete(key);
    else selected.value.add(key);
  };
  const back = () => {
    step.value = steps.TOKEN_INPUT;
    error.value = null;
    saveError.value = false;
  };
  let revision = 0;
  const navigateStep = (target: number) => {
    if (target === steps.TOKEN_INPUT) back();
    else step.value = target;
  };
  watch(() => form.value.token, () => {
    revision += 1;
    verified.value = false;
    saved.value = false;
    error.value = null;
    saveError.value = false;
    accounts.value = [];
    selected.value.clear();
  }, { flush: 'sync' });
  const verify = async () => {
    if (busy.value || (!configured.value && !form.value.token.trim())) return;
    const requestRevision = revision;
    const token = form.value.token.trim() || null;
    step.value = steps.ACCOUNT_SELECTION;
    busy.value = true;
    error.value = null;
    saveError.value = false;
    saved.value = false;
    verified.value = false;
    accounts.value = [];
    selected.value.clear();
    try {
      const result = await listAccounts({ workspaceId, token });
      if (requestRevision !== revision) return;
      accounts.value = result.accounts;
      selected.value = new Set(result.accounts
        .filter(account => account.usedByIntegration && !account.deleted).map(accountKey));
      verifiedToken.value = token;
      verified.value = true;
      error.value = null;
    } catch (e: unknown) {
      const code = handleGqlApiBusinessError<ListWiseAccountsErrorCodes>(e);
      if (requestRevision === revision) error.value = code;
    } finally { busy.value = false; }
  };
  const recover = async () => {
    if (error.value === SaveWiseIntegrationSettingsErrorCodes.InvalidToken) back();
    else await verify();
  };
  const save = async () => {
    if (busy.value || !verified.value || selected.value.size === 0) return;
    step.value = steps.ACTIVATION;
    busy.value = true;
    error.value = null;
    saveError.value = false;
    try {
      const result = await saveSettings({
        workspaceId, token: verifiedToken.value,
        accounts: accounts.value.filter(account => !account.deleted && selected.value.has(accountKey(account)))
          .map(({ profileId, accountId, currency }) => ({ profileId, accountId, currency })),
      });
      error.value = null;
      if (result.success) {
        const savedAccounts = accounts.value.filter(account => !account.deleted)
          .map(account => ({ ...account, usedByIntegration: selected.value.has(accountKey(account)) }));
        form.value.token = '';
        configured.value = true;
        verifiedToken.value = null;
        accounts.value = savedAccounts;
        selected.value = new Set(savedAccounts.filter(account => account.usedByIntegration).map(accountKey));
        verified.value = true;
        saved.value = true;
      }
    } catch (e: unknown) {
      const code = handleGqlApiBusinessError<SaveWiseIntegrationSettingsErrorCodes>(e);
      error.value = code;
      saveError.value = true;
    } finally { busy.value = false; }
  };
  watch(configuration, async (workspace) => {
    if (!workspace) return;
    configured.value = workspace.integrations.wise.active;
    if (configured.value) await verify();
  });
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

    &__hint { display: block; }

    &--selectable {
      cursor: pointer;
      transition: border-color 0.2s, box-shadow 0.2s, background-color 0.2s;
      &:hover, &[aria-pressed="true"] { border-color: $accent-primary-color; background: $primary-grey; }
      &:focus-visible { outline: 2px solid $accent-primary-color; outline-offset: 3px; }
      &:disabled { cursor: default; border-style: dashed; background: $primary-grey; }
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

  .integration-wizard__step {
    padding: 0;
    border: 0;
    background: none;
    color: inherit;
    font: inherit;
    cursor: pointer;
    &:disabled { cursor: default; }
    &:focus-visible { outline: 2px solid $accent-primary-color; outline-offset: 3px; }
  }
</style>
