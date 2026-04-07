<template>
  <SaOutputLoader :loading="loading">
    {{ generalTaxTitle }}
  </SaOutputLoader>
</template>

<script lang="ts" setup>
  import SaOutputLoader from '@/components/SaOutputLoader.vue';
  import { useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    generalTaxId?: number;
  }>();

  const getGeneralTaxQuery = useLazyQuery(graphql(`
    query getGeneralTaxForOutput($workspaceId: Long!, $taxId: Long!) {
      workspace(id: $workspaceId) {
        generalTax(id: $taxId) {
          title
        }
      }
    }
  `), 'workspace');

  const {
    loading,
    value: generalTaxTitle,
  } = useValueLoadedByCurrentWorkspaceAndProp(() => props.generalTaxId, async (taxId, workspaceId) => {
    const workspace = await getGeneralTaxQuery({ taxId, workspaceId });
    return workspace?.generalTax?.title;
  });
</script>
