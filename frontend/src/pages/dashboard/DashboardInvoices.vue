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
  import DashboardCardInvoice from '@/pages/dashboard/DashboardCardInvoice.vue';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';
  import type { DashboardInvoicesQuery } from '@/services/api/gql/graphql';
  import { InvoiceStatus } from '@/services/api/gql/graphql';

  type InvoiceNode = DashboardInvoicesQuery['workspace']['invoices']['edges'][0]['node'];

  const props = defineProps<{
    fromDate: Date,
    toDate: Date,
  }>();

  const getDashboardInvoicesQuery = useLazyQuery(graphql(`
    query dashboardInvoices($workspaceId: Long!, $statusIn: [InvoiceStatus!]) {
      workspace(id: $workspaceId) {
        invoices(first: 100, statusIn: $statusIn) {
          edges {
            node {
              id
              title
              amount
              currency
              dateIssued
              dateSent
              dueDate
              status
              customer {
                id
              }
            }
          }
        }
      }
    }
  `), 'workspace');

  const {
    value: maybeInvoices,
  } = useValueLoadedByCurrentWorkspaceAndProp(
    () => props.fromDate && props.toDate,
    async (_, workspaceId) => {
      const workspace = await getDashboardInvoicesQuery({
        workspaceId,
        statusIn: [InvoiceStatus.Sent, InvoiceStatus.Overdue],
      });
      return workspace?.invoices.edges.map(e => e.node) ?? [];
    },
  );
  const invoices = computed<InvoiceNode[]>(() => maybeInvoices.value || []);
</script>

<style scoped>

</style>
