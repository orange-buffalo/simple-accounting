<template>
  <SaPage :header="$t.integrations.wise.setupHeader()">
    <div v-if="initializing" class="integration-wizard__result" role="status" aria-live="polite">
      <SaStatusLabel status="regular" custom-icon="loading">{{ $t.integrations.loading() }}</SaStatusLabel>
    </div>
    <SaWizard v-else v-model="step" :steps="wizardSteps">
      <template #token>
        <div class="integration-wizard__content">
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
          <SaForm v-model="form" :on-submit="verify" hide-buttons>
            <SaFormInput prop="token" type="password" :label="$t.integrations.wise.tokenLabel()" />
          </SaForm>
        </div>
      </template>
      <template #accounts>
        <div v-if="busy" class="integration-wizard__result" role="status" aria-live="polite">
          <SaStatusLabel status="regular" custom-icon="loading">{{ $t.integrations.wise.verifying() }}</SaStatusLabel>
        </div>
        <div v-else-if="error" role="alert" class="integration-wizard__result">
          <SaStatusLabel status="failure">{{ errorMessage }}</SaStatusLabel>
        </div>
        <div v-else class="integration-wizard__content">
          <p role="status" aria-live="polite">{{ $t.integrations.wise.selectAccounts() }}</p>
          <SaStatusLabel v-if="accounts.length === 0" status="regular" role="status" aria-live="polite">
            {{ $t.integrations.wise.noAccounts() }}
          </SaStatusLabel>
          <section v-for="profile in profiles" :key="profile.id" class="wise-profile" :aria-labelledby="`wise-profile-${profile.id}`">
            <h2 :id="`wise-profile-${profile.id}`" class="wise-profile__header">
              <span>{{ profile.name || $t.integrations.wise.profileId(profile.id) }}</span>
              <small v-if="profile.type">{{ profile.type === 'BUSINESS' ? $t.integrations.wise.businessAccount() : $t.integrations.wise.personalAccount() }}</small>
            </h2>
            <div class="integration-cards">
              <button v-for="account in profile.accounts" :key="accountKey(account)" type="button" class="integration-card integration-card--selectable"
                      :aria-pressed="selected.has(accountKey(account))" :disabled="account.deleted" @click="toggle(account)">
                <strong>{{ account.currency }}</strong>
                <SaMoneyOutput v-if="account.amountInCents !== null" class="integration-card__amount" :amount-in-cents="account.amountInCents" :currency="account.currency" />
                <span v-if="selected.has(accountKey(account))">✓ {{ $t.integrations.selected() }}</span>
                <ElTag v-if="account.deleted" type="danger">{{ $t.integrations.wise.deleted() }}</ElTag>
                <span v-if="account.deleted" class="integration-card__hint">{{ $t.integrations.wise.deletedHint() }}</span>
              </button>
            </div>
          </section>
          <section class="wise-configuration" aria-labelledby="wise-configuration-header">
            <h2 id="wise-configuration-header">{{ $t.integrations.wise.configurationHeader() }}</h2>
            <SaForm v-model="preferences" :on-submit="save" hide-buttons>
              <SaFormSwitchSection prop="fetchTransactionsPeriodically" :label="$t.integrations.wise.fetchTransactionsPeriodically()" />
            </SaForm>
            <p>{{ $t.integrations.wise.fetchTransactionsHint() }}</p>
          </section>
        </div>
      </template>
      <template #activation>
        <div class="integration-wizard__result" aria-live="polite">
          <SaStatusLabel v-if="busy" status="regular" custom-icon="loading" role="status">
            {{ $t.integrations.wise.saving() }}
          </SaStatusLabel>
          <template v-else-if="saved">
            <SaStatusLabel status="success">{{ $t.integrations.wise.success() }}</SaStatusLabel>
            <RouterLink :to="{ name: 'integrations' }">{{ $t.integrations.header() }}</RouterLink>
          </template>
          <template v-else-if="error">
            <SaStatusLabel status="failure" role="alert">{{ errorMessage }}</SaStatusLabel>
          </template>
        </div>
      </template>
    </SaWizard>
  </SaPage>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import { ElTag } from 'element-plus';
  import SaWizard from '@/components/wizard/SaWizard.vue';
  import type { WizardStep } from '@/components/wizard/sa-wizard';
  import SaPage from '@/components/SaPage.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaFormSwitchSection from '@/components/form/SaFormSwitchSection.vue';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
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
        accounts { profileId profileName profileType accountId currency amountInCents usedByIntegration deleted }
      }
    }
  `), 'listWiseAccounts');
  const saveSettings = useMutation(graphql(/* GraphQL */ `
    mutation saveWiseIntegrationSettings(
      $workspaceId: String!, $token: String, $accounts: [WiseAccountInput!]!, $fetchTransactionsPeriodically: Boolean!
    ) {
      saveWiseIntegrationSettings(
        workspaceId: $workspaceId, token: $token, accounts: $accounts,
        fetchTransactionsPeriodically: $fetchTransactionsPeriodically
      ) { success }
    }
  `), 'saveWiseIntegrationSettings');
  const [initializing, configuration] = useQuery(graphql(/* GraphQL */ `
    query wiseConfiguration($workspaceId: String!) {
      workspace(id: $workspaceId) { integrations { wise { active fetchTransactionsPeriodically } } }
    }
  `), 'workspace', { variables: { workspaceId } });
  type Account = ListWiseAccountsMutation['listWiseAccounts']['accounts'][number];
  const form = ref<{ token: string }>({ token: '' });
  const preferences = ref({ fetchTransactionsPeriodically: true });
  const steps = { TOKEN_INPUT: 'token', ACCOUNT_SELECTION: 'accounts', ACTIVATION: 'activation' } as const;
  type WiseStep = typeof steps[keyof typeof steps];
  const step = ref<WiseStep>(steps.TOKEN_INPUT);
  const configured = ref(false);
  const verified = ref(false);
  const verifiedToken = ref<string | null>(null);
  const busy = ref(false);
  const saved = ref(false);
  watch(() => preferences.value.fetchTransactionsPeriodically, () => { saved.value = false; });
  const error = ref<SaveWiseIntegrationSettingsErrorCodes | ListWiseAccountsErrorCodes | null>(null);
  const saveError = ref(false);
  const accounts = ref<Account[]>([]);
  const profiles = computed(() => {
    const groups = new Map<string, { id: string; name: string; type: string | null; accounts: Account[] }>();
    accounts.value.forEach((account) => {
      let profile = groups.get(account.profileId);
      if (!profile) {
        profile = { id: account.profileId, name: account.profileName, type: account.profileType, accounts: [] };
        groups.set(account.profileId, profile);
      }
      profile.accounts.push(account);
    });
    return [...groups.values()];
  });
  const selected = ref(new Set<string>());
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
  const clearError = () => {
    error.value = null;
    saveError.value = false;
  };
  const back = () => {
    clearError();
    step.value = steps.TOKEN_INPUT;
  };
  let revision = 0;
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
        fetchTransactionsPeriodically: preferences.value.fetchTransactionsPeriodically,
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
  const wizardSteps = computed<WizardStep<WiseStep>[]>(() => [
    {
      id: steps.TOKEN_INPUT,
      title: $t.value.integrations.wise.tokenStep(),
      status: verified.value ? 'success' : step.value === steps.TOKEN_INPUT ? 'process' : 'wait',
      navigationDisabled: busy.value,
      onEnter: clearError,
      next: {
        label: $t.value.integrations.next(), action: verify,
        disabled: busy.value || (!configured.value && !form.value.token.trim()),
      },
    },
    {
      id: steps.ACCOUNT_SELECTION,
      title: $t.value.integrations.wise.accountsStep(),
      status: saved.value || step.value === steps.ACTIVATION ? 'success'
        : step.value === steps.ACCOUNT_SELECTION ? 'process' : 'wait',
      navigationDisabled: busy.value || !verified.value,
      back: { label: $t.value.integrations.back(), target: steps.TOKEN_INPUT, disabled: busy.value },
      next: error.value ? undefined : {
        label: $t.value.integrations.next(), action: save,
        disabled: busy.value || selected.value.size === 0,
      },
    },
    {
      id: steps.ACTIVATION,
      title: $t.value.integrations.wise.finishStep(),
      status: saved.value ? 'success' : 'wait',
      navigationDisabled: busy.value || (!saved.value && !saveError.value),
      back: error.value ? { label: $t.value.integrations.back(), action: recover, disabled: busy.value } : undefined,
    },
  ]);
  watch(configuration, async (workspace) => {
    if (!workspace) return;
    configured.value = workspace.integrations.wise.active;
    preferences.value.fetchTransactionsPeriodically = workspace.integrations.wise.fetchTransactionsPeriodically;
    if (configured.value) await verify();
  });
</script>

<style scoped lang="scss">
  @use "@/styles/vars.scss" as *;

  .wise-profile {
    max-width: 840px;
    margin: 24px auto;

    &__header {
      display: flex;
      align-items: baseline;
      flex-wrap: wrap;
      gap: 12px;
      font-size: 20px;

      small { color: $secondary-text-color; font-size: 14px; font-weight: normal; }
    }
  }

  .wise-configuration {
    max-width: 840px;
    margin: 32px auto;
    h2 { font-size: 20px; }
    p { color: $secondary-text-color; }
  }

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
    &__content li { margin-bottom: 12px; }
    &__result { display: flex; justify-content: center; gap: 20px; margin: 24px 0; }
    &__result { flex-direction: column; align-items: center; text-align: center; }
  }

</style>
