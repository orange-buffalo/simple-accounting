<template>
  <div v-if="loading" role="status" aria-live="polite">
    <SaStatusLabel status="regular" custom-icon="loading">{{ $t.integrations.loading() }}</SaStatusLabel>
  </div>
  <SaStatusLabel v-else-if="integrations === null" status="failure">{{ $t.integrations.wise.unavailable() }}</SaStatusLabel>
  <div v-else class="integration-cards">
    <article v-for="provider in providers" :key="provider.id" class="integration-card">
      <component :is="provider.logo" class="integration-card__logo" aria-label="Wise" role="img" />
      <SaStatusLabel v-if="provider.active" status="success">{{ $t.integrations.active() }}</SaStatusLabel>
      <p>{{ provider.description }}</p>
      <RouterLink :to="{ name: provider.active ? provider.view : provider.setup }">
        {{ provider.active ? $t.integrations.view() : $t.integrations.setup() }} →
      </RouterLink>
    </article>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { graphql } from '@/services/api/gql';
  import { useQuery } from '@/services/api/use-gql-api';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import WiseLogo from '@/icons/svg/wise-simple.svg?component';
  import { $t } from '@/services/i18n';

  const props = defineProps<{ workspaceId: string }>();
  const [loading, workspace] = useQuery(graphql(/* GraphQL */ `
    query integrationProviders($workspaceId: String!) {
      workspace(id: $workspaceId) { integrations { wise { active } } }
    }
  `), 'workspace', { variables: { workspaceId: props.workspaceId } });
  const integrations = computed(() => workspace.value?.integrations ?? null);
  const providers = computed(() => [{
    id: 'wise', logo: WiseLogo, description: $t.value.integrations.wise.description(),
    active: integrations.value?.wise.active === true, setup: 'wise-setup', view: 'wise-view',
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
    border-radius: 6px;
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
