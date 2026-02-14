<template>
  <SaOutputLoader :loading="loading">
    {{ customerName }}
  </SaOutputLoader>
</template>

<script lang="ts" setup>
import SaOutputLoader from '@/components/SaOutputLoader.vue';
import { customersApi } from '@/services/api';
import { useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';

const props = defineProps<{
  customerId?: number;
}>();

const { loading, value: customerName } = useValueLoadedByCurrentWorkspaceAndProp(
  () => props.customerId,
  async (customerId, workspaceId) => {
    const customer = await customersApi.getCustomer({
      customerId,
      workspaceId,
    });
    return customer.name;
  },
);
</script>
