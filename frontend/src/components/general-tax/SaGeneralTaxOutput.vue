<template>
  <SaOutputLoader :loading="loading">
    {{ generalTaxTitle }}
  </SaOutputLoader>
</template>

<script lang="ts" setup>
  import SaOutputLoader from '@/components/SaOutputLoader.vue';
  import { useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';
  import { generalTaxesApi } from '@/services/api';

  const props = defineProps<{
    generalTaxId?: number;
  }>();

  const {
    loading,
    value: generalTaxTitle,
  } = useValueLoadedByCurrentWorkspaceAndProp(() => props.generalTaxId, async (taxId, workspaceId) => {
    const customer = await generalTaxesApi.getTax({
      taxId,
      workspaceId,
    });
    return customer.title;
  });
</script>
