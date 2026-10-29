<template>
  <DashboardCard
    header-icon="invoices-overview"
    class="sa-dashboard__card--invoice"
    :title="$t.dashboard.cards.invoice.header()"
    :loaded="true"
  >
    <template #header>
      <SaMoneyOutput
        class="sa-dashboard__card__header__amount"
        :currency="invoice.currency"
        :amount-in-cents="invoice.amount"
      />

      <span class="sa-dashboard__card__header__finalized">{{ invoice.invoiceNumber }}</span>
      <span
        class="sa-dashboard__card__header__finalized sa-dashboard__invoice-status"
        :class="{ 'sa-dashboard__invoice-status--overdue': invoice.status === 'OVERDUE' }"
      >{{ invoiceStatus }}</span>
    </template>

    <template #content>
      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.invoice.to() }}</span>
        <span>{{ invoice.customer!.name }}</span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.invoice.issueDate() }}</span>
        <span>{{ $t.common.date.medium(invoice.dateIssued) }}</span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.invoice.dateSent() }}</span>
        <span>{{ invoice.dateSent ? $t.common.date.medium(invoice.dateSent) : '' }}</span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.invoice.dueDate() }}</span>
        <span>{{ $t.common.date.medium(invoice.dueDate) }}</span>
      </div>
    </template>
  </DashboardCard>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import DashboardCard from '@/pages/dashboard/DashboardCard.vue';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import { $t } from '@/services/i18n';
  import type { GetDashboardAnalyticsQuery } from '@/services/api/gql/graphql';

  type InvoiceNode = GetDashboardAnalyticsQuery['workspace']['invoices']['edges'][0]['node'];

  const props = defineProps<{
    invoice: InvoiceNode,
  }>();

  const invoiceStatus = computed(() => {
    if (props.invoice.status === 'OVERDUE') {
      return $t.value.dashboard.cards.invoice.status.overdue();
    }
    return $t.value.dashboard.cards.invoice.status.pending();
  });
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .sa-dashboard__card--invoice {
    .sa-dashboard__card__header {
      position: relative;
    }

    .sa-dashboard__card__header__title {
      padding-right: 100px;
    }

    .sa-dashboard__card__header__icon {
      display: none;
    }

    .sa-dashboard__card__header__finalized:not(.sa-dashboard__invoice-status) {
      color: $primary-text-color;
      font-weight: bold;
      overflow-wrap: anywhere;
    }
  }

  .sa-dashboard__invoice-status {
    position: absolute;
    top: 20px;
    right: 24px;
    display: inline-block;
    padding: 4px 9px;
    border-radius: $status-border-radius;
    color: $warning-color;
    background: $warning-color-bg;
    font-size: 12px;

    &--overdue {
      color: $danger-color;
      background: $danger-color-bg;
    }
  }
</style>
