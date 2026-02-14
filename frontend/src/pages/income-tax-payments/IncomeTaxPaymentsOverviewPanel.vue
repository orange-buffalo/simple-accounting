<template>
  <SaOverviewItem :title="taxPayment.title">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        v-if="taxPayment.datePaid"
        :tooltip="$t.incomeTaxPaymentsOverviewPanel.datePaid.label()"
        icon="calendar"
      >
        {{ $t.common.date.medium(taxPayment.datePaid) }}
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <SaOverviewItemAttributePreviewIcon
        v-if="taxPayment.notes"
        icon="notes"
        :tooltip="$t.incomeTaxPaymentsOverviewPanel.notes.tooltip()"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="taxPayment.attachments.length"
        :tooltip="$t.incomeTaxPaymentsOverviewPanel.attachments.tooltip()"
        icon="attachment"
      />
    </template>

    <template #last-column>
      <SaOverviewItemAmountPanel
        :currency="defaultCurrency"
        :amount="taxPayment.amount"
      />
    </template>

    <template #details>
      <SaOverviewItemDetailsSectionActions>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToTaxPaymentEdit"
        >
          {{ $t.incomeTaxPaymentsOverviewPanel.edit() }}
        </SaActionLink>
      </SaOverviewItemDetailsSectionActions>

      <SaOverviewItemDetailsSection
        :title="$t.incomeTaxPaymentsOverviewPanel.summary.header()"
      >
        <div class="row">
          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomeTaxPaymentsOverviewPanel.datePaid.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t.common.date.medium(taxPayment.datePaid) }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomeTaxPaymentsOverviewPanel.reportingDate.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t.common.date.medium(taxPayment.reportingDate) }}
          </SaOverviewItemDetailsSectionAttribute>
        </div>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="taxPayment.attachments.length"
        :title="$t.incomeTaxPaymentsOverviewPanel.attachments.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="taxPayment.attachments" />
          </div>
        </div>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="taxPayment.notes"
        :title="$t.incomeTaxPaymentsOverviewPanel.notes.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="taxPayment.notes" />
          </div>
        </div>
      </SaOverviewItemDetailsSection>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
import SaDocumentsList from '@/components/documents/SaDocumentsList.vue';
import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
import SaOverviewItemAmountPanel from '@/components/overview-item/SaOverviewItemAmountPanel.vue';
import SaOverviewItemAttributePreviewIcon from '@/components/overview-item/SaOverviewItemAttributePreviewIcon.vue';
import SaOverviewItemDetailsSection from '@/components/overview-item/SaOverviewItemDetailsSection.vue';
import SaOverviewItemDetailsSectionActions from '@/components/overview-item/SaOverviewItemDetailsSectionActions.vue';
import SaOverviewItemDetailsSectionAttribute from '@/components/overview-item/SaOverviewItemDetailsSectionAttribute.vue';
import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
import SaActionLink from '@/components/SaActionLink.vue';
import SaMarkdownOutput from '@/components/SaMarkdownOutput.vue';
import type { IncomeTaxPaymentDto } from '@/services/api';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { useCurrentWorkspace } from '@/services/workspaces';

const props = defineProps<{
  taxPayment: IncomeTaxPaymentDto;
}>();

const { navigateToView } = useNavigation();
const navigateToTaxPaymentEdit = () =>
  navigateToView({
    name: 'edit-income-tax-payment',
    params: { id: props.taxPayment.id },
  });

const { currentWorkspace, defaultCurrency } = useCurrentWorkspace();
</script>
