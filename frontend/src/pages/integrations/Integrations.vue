<template>
  <SaPage :header="$t.integrations.header()">
    <div v-if="loading" role="status" aria-live="polite">
      <SaStatusLabel status="regular" custom-icon="loading">{{ $t.integrations.loading() }}</SaStatusLabel>
    </div>
    <div v-else-if="integrations" class="integration-cards">
      <article v-for="provider in providers" :key="provider.id" class="integration-card">
        <component :is="provider.logo" class="integration-card__logo" :aria-label="provider.name" role="img" />
        <ElTooltip
          v-if="provider.active && provider.authenticationRequired"
          :content="$t.integrations.wise.authenticationRequiredHint()"
          :trigger="['hover', 'focus']"
          :popper-style="{ maxWidth: '360px' }"
        >
          <SaStatusLabel status="failure" role="status" tabindex="0">{{ $t.integrations.wise.authenticationRequired() }}</SaStatusLabel>
        </ElTooltip>
        <SaStatusLabel v-else-if="provider.active && !provider.fetchTransactionsPeriodically" status="regular" role="status">{{ $t.integrations.inactive() }}</SaStatusLabel>
        <SaStatusLabel v-else-if="provider.active" status="success">{{ $t.integrations.active() }}</SaStatusLabel>
        <p>{{ provider.description }}</p>
        <span v-if="busy" aria-disabled="true">{{ provider.active ? $t.integrations.update() : $t.integrations.setup() }} →</span>
        <RouterLink v-else :to="{ name: provider.setup }">
          {{ provider.active ? $t.integrations.update() : $t.integrations.setup() }} →
        </RouterLink>
        <ElButton v-if="provider.active" type="danger" plain :disabled="busy" @click="removeIntegration">
          {{ $t.integrations.wise.remove() }}
        </ElButton>
        <SaStatusLabel v-if="busy" status="regular" custom-icon="loading" role="status" aria-live="polite">{{ $t.integrations.updating() }}</SaStatusLabel>
      </article>
    </div>
  </SaPage>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { ElButton, ElTooltip } from 'element-plus';
  import { useConfirmation } from '@/components/confirmation/use-confirmation';
  import SaPage from '@/components/SaPage.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { useQuery, useMutation } from '@/services/api/use-gql-api';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import WiseLogo from '@/icons/svg/wise-simple.svg?component';
  import { $t } from '@/services/i18n';

  const workspaceId = useCurrentWorkspace().currentWorkspace.id;
  const [loading, workspace] = useQuery(graphql(/* GraphQL */ `
    query integrationProviders($workspaceId: String!) {
      workspace(id: $workspaceId) {
        integrations { wise { active authenticationRequired fetchTransactionsPeriodically } }
      }
    }
  `), 'workspace', { variables: { workspaceId } });
  const integrations = computed(() => workspace.value?.integrations ?? null);
  const busy = ref(false);
  const removeWise = useMutation(graphql(/* GraphQL */ `
    mutation removeWiseIntegration($workspaceId: String!) {
      removeWiseIntegration(workspaceId: $workspaceId) { success }
    }
  `), 'removeWiseIntegration');
  const confirmRemoval = useConfirmation($t.value.integrations.wise.removeMessage(), {
    title: $t.value.integrations.wise.remove(),
    confirmButtonText: $t.value.integrations.wise.remove(),
    cancelButtonText: $t.value.integrations.wise.keep(),
    type: 'warning',
  }, async () => {
    await removeWise({ workspaceId });
    if (workspace.value) {
      workspace.value.integrations.wise.active = false;
      workspace.value.integrations.wise.authenticationRequired = false;
    }
  });
  const removeIntegration = async () => {
    if (busy.value) return;
    busy.value = true;
    try {
      await confirmRemoval();
    } finally {
      busy.value = false;
    }
  };
  const providers = computed(() => [{
    id: 'wise', name: $t.value.integrations.wise.name(), logo: WiseLogo,
    description: $t.value.integrations.wise.description(),
    active: integrations.value?.wise.active === true, setup: 'wise-setup',
    authenticationRequired: integrations.value?.wise.authenticationRequired === true,
    fetchTransactionsPeriodically: integrations.value?.wise.fetchTransactionsPeriodically === true,
  }]);
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
    border-radius: $panel-border-radius;
    background: $white;
    color: $primary-text-color;
    font: inherit;
    text-align: center;
    overflow-wrap: anywhere;

    p { color: $secondary-text-color; line-height: 1.5; }
    a { color: $secondary-color; font-weight: 600; }
    &__logo { width: 144px; height: 36px; color: $secondary-color; }
  }
</style>
