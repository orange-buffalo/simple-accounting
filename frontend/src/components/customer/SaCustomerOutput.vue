<template>
  <SaOutputLoader :loading="loading">
    {{ customerName }}
  </SaOutputLoader>
</template>

<script lang="ts" setup>
  import SaOutputLoader from '@/components/SaOutputLoader.vue';
  import { useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    customerId?: number
  }>();

  const getCustomerQuery = useLazyQuery(graphql(`
    query getCustomerForOutput($workspaceId: Long!, $customerId: Long!) {
      workspace(id: $workspaceId) {
        customer(id: $customerId) {
          name
        }
      }
    }
  `), 'workspace');

  const {
    loading,
    value: customerName,
  } = useValueLoadedByCurrentWorkspaceAndProp(() => props.customerId, async (customerId, workspaceId) => {
    const workspace = await getCustomerQuery({ customerId, workspaceId });
    return workspace?.customer?.name;
  });
</script>
