<template>
  <DashboardCardInvoice
    v-for="invoice in invoices"
    :key="invoice.id"
    :invoice="invoice"
  />
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { useValueLoadedByCurrentWorkspaceAndProp } from '@/services/utils';
  import type { InvoiceDto } from '@/services/api';
  import { consumeAllPages, invoicesApi } from '@/services/api';
  import DashboardCardInvoice from '@/pages/dashboard/DashboardCardInvoice.vue';

  const props = defineProps<{
    fromDate: Date,
    toDate: Date,
  }>();

  const {
    value: maybeInvoices,
  } = useValueLoadedByCurrentWorkspaceAndProp(
    () => props.fromDate && props.toDate,
    (_, workspaceId) => consumeAllPages(async (pageRequest) => invoicesApi.getInvoices({
      workspaceId,
      statusIn: ['SENT', 'OVERDUE'],
      ...pageRequest,
    })),
  );
  const invoices = computed<InvoiceDto[]>(() => maybeInvoices.value || []);
</script>

<style scoped>

</style>
