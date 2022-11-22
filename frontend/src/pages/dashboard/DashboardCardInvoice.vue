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
      <!--      TODO #459: translations-->
      <div class="sa-dashboard__card__details__item">
        <span>To</span>
        <span><SaCustomerOutput :customer-id="invoice.customer" /></span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>Issue Date</span>
        <span>{{ $t.common.date.medium(invoice.dateIssued) }}</span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>Date Sent</span>
        <span>{{ $t.common.date.medium(invoice.dateSent) }}</span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>Due Date</span>
        <span>{{ $t.common.date.medium(invoice.dueDate) }}</span>
      </div>
    </template>
  </DashboardCard>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import DashboardCard from '@/pages/dashboard/DashboardCard.vue';
  import type { InvoiceDto } from '@/services/api';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import { $t } from '@/services/i18n';
  import SaCustomerOutput from '@/components/customer/SaCustomerOutput.vue';

  const props = defineProps<{
    invoice: InvoiceDto,
  }>();

  // TODO #459: translations
  const invoiceStatus = computed(() => {
    if (props.invoice.status === 'OVERDUE') {
      return 'Overdue';
    }
    return 'Pending';
  });
</script>
