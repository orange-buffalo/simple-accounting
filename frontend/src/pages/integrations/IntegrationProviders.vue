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
  import './integrations.scss';

  const props = defineProps<{ workspaceId: string }>();
  const [loading, integrations] = useQuery(graphql(/* GraphQL */ `
    query integrationProviders($workspaceId: String!) {
      integrations(workspaceId: $workspaceId) { wise { active } }
    }
  `), 'integrations', { variables: { workspaceId: props.workspaceId } });
  const providers = computed(() => [{
    id: 'wise', logo: WiseLogo, description: $t.value.integrations.wise.description(),
    active: integrations.value?.wise.active === true, setup: 'wise-setup', view: 'wise-view',
  }]);
</script>
