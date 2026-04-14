<template>
  <DashboardCardInvoice
    v-for="invoice in invoices"
    :key="invoice.id"
    :invoice="invoice"
  />
</template>

<script lang="ts" setup>
  import DashboardCardInvoice from '@/pages/dashboard/DashboardCardInvoice.vue';
  import type { GetDashboardAnalyticsQuery } from '@/services/api/gql/graphql';

  type InvoiceNode = GetDashboardAnalyticsQuery['workspace']['invoices']['edges'][0]['node'];

  defineProps<{
    invoices: InvoiceNode[],
  }>();
</script>

<style scoped>

</style>
