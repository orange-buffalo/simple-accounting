<template>
  <SaOutputLoader :loading="loading">
    {{ customerName }}
  </SaOutputLoader>
</template>

<script lang="ts" setup>
  import SaOutputLoader from '@/components/SaOutputLoader.vue';
  import { customerApi } from '@/services/api';
  import { useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';

  const props = defineProps({
    customerId: {
      type: Number,
      default: null,
    },
  });

  const {
    loading,
    value: customerName,
  } = useValueLoadedByCurrentWorkspaceAndProp(() => props.customerId, async (customerId, workspaceId) => {
    const customer = await customerApi.getCustomer({
      customerId,
      workspaceId,
    });
    return customer.name;
  });
</script>
