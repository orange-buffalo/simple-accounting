<template>
  <DashboardCard
    header-icon="invoices-overview"
    :loaded="true"
  >
    <template #header>
      <SaMoneyOutput
        class="sa-dashboard__card__header__amount"
        :currency="invoice.currency"
        :amount-in-cents="invoice.amount"
      />

      <span class="sa-dashboard__card__header__finalized">{{ invoice.title }}</span>
      <span class="sa-dashboard__card__header__finalized">{{ invoiceStatus }}</span>
      <span class="sa-dashboard__card__header__pending">&nbsp;</span>
    </template>

    <template #content>
      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.invoice.to() }}</span>
        <span><SaCustomerOutput :customer-id="invoice.customer" /></span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.invoice.issueDate() }}</span>
        <span>{{ $t.common.date.medium(invoice.dateIssued) }}</span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.invoice.dateSent() }}</span>
        <span>{{ $t.common.date.medium(invoice.dateSent) }}</span>
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
import SaCustomerOutput from '@/components/customer/SaCustomerOutput.vue';
import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
import DashboardCard from '@/pages/dashboard/DashboardCard.vue';
import type { InvoiceDto } from '@/services/api';
import { $t } from '@/services/i18n';

const props = defineProps<{
  invoice: InvoiceDto;
}>();

const invoiceStatus = computed(() => {
  if (props.invoice.status === 'OVERDUE') {
    return $t.value.dashboard.cards.invoice.status.overdue();
  }
  return $t.value.dashboard.cards.invoice.status.pending();
});
</script>
