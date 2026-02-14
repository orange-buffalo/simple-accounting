<template>
  <SaOverviewItem :title="income.title">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        v-if="income.dateReceived"
        :tooltip="$t.incomesOverviewPanel.dateReceived.tooltip()"
        icon="calendar"
      >
        {{ $t.common.date.medium(income.dateReceived) }}
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <SaOverviewItemAttributePreviewIcon
        v-if="income.notes"
        icon="notes"
        :tooltip="$t.incomesOverviewPanel.notes.tooltip()"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        :tooltip="$t.incomesOverviewPanel.generalTax.tooltip()"
        icon="tax"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="income.attachments.length"
        :tooltip="$t.incomesOverviewPanel.attachments.tooltip()"
        icon="attachment"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        :tooltip="$t.incomesOverviewPanel.foreignCurrency.tooltip()"
        icon="multi-currency"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="linkedInvoice.exists"
        :tooltip="$t.incomesOverviewPanel.linkedInvoice.tooltip()"
        icon="invoice"
      />
    </template>

    <template #middle-column>
      <ElTooltip
        :content="incomeStatus.fullText"
        :disabled="incomeStatus.isSuccess"
        placement="bottom"
      >
        <SaStatusLabel :status="incomeStatus.value">
          {{ incomeStatus.shortText }}
        </SaStatusLabel>
      </ElTooltip>
    </template>

    <template #last-column>
      <SaOverviewItemAmountPanel
        :currency="totalAmount.currency"
        :amount="totalAmount.value"
      />
    </template>

    <template #details>
      <SaOverviewItemDetailsSectionActions>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToIncomeEdit"
        >
          {{ $t.incomesOverviewPanel.edit() }}
        </SaActionLink>
      </SaOverviewItemDetailsSectionActions>

      <SaOverviewItemDetailsSection
        :title="$t.incomesOverviewPanel.summary.header()"
      >
        <div class="row">
          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.status.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaStatusLabel
              :status="incomeStatus.value"
              :simplified="true"
            >
              {{ incomeStatus.fullText }}
            </SaStatusLabel>
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.category.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaCategoryOutput :category-id="income.category" />
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.dateReceived.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t.common.date.medium(income.dateReceived) }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.incomeTaxableAmounts.adjustedAmountInDefaultCurrency.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaMoneyOutput
              v-if="income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount-in-cents="income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
            />

            <span v-else>Not yet provided</span>
          </SaOverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isGeneralTaxApplicable">
          <div class="row">
            <SaOverviewItemDetailsSectionAttribute
              :label="$t.incomesOverviewPanel.generalTax.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <SaGeneralTaxOutput :general-tax-id="income.generalTax" />
            </SaOverviewItemDetailsSectionAttribute>

            <SaOverviewItemDetailsSectionAttribute
              :label="$t.incomesOverviewPanel.generalTaxRate.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{
                $t.incomesOverviewPanel.generalTaxRate.value(ensureDefined(income.generalTaxRateInBps))
              }}
            </SaOverviewItemDetailsSectionAttribute>

            <SaOverviewItemDetailsSectionAttribute
              :label="$t.incomesOverviewPanel.generalTaxAmount.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <SaMoneyOutput
                v-if="income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
                :currency="defaultCurrency"
                :amount-in-cents="income.generalTaxAmount"
              />

              <span v-else>{{ $t.incomesOverviewPanel.generalTaxAmount.notProvided() }}</span>
            </SaOverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        :title="$t.incomesOverviewPanel.generalInformation.header()"
      >
        <div class="row">
          <SaOverviewItemDetailsSectionAttribute
            v-if="isForeignCurrency"
            :label="$t.incomesOverviewPanel.originalCurrency.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ income.currency }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.originalAmount.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaMoneyOutput
              :currency="income.currency"
              :amount-in-cents="income.originalAmount"
            />
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            v-if="linkedInvoice.exists"
            :label="$t.incomesOverviewPanel.linkedInvoice.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaOutputLoader :loading="linkedInvoice.loading">
              {{ linkedInvoice.title }}
            </SaOutputLoader>
          </SaOverviewItemDetailsSectionAttribute>
        </div>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="isForeignCurrency"
        :title="$t.incomesOverviewPanel.foreignCurrency.header()"
      >
        <div class="row">
          <!-- eslint-disable -->
          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.label(defaultCurrency)"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaMoneyOutput
              v-if="income.convertedAmounts.originalAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount-in-cents="income.convertedAmounts.originalAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t.incomesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.notProvided() }}
            </span>
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.differentExchangeRate.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{
              $t.incomesOverviewPanel.differentExchangeRate.value(income.useDifferentExchangeRateForIncomeTaxPurposes)
            }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.incomeTaxableAmounts.originalAmountInDefaultCurrency.label(defaultCurrency)"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaMoneyOutput
              v-if="income.incomeTaxableAmounts.originalAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount-in-cents="income.incomeTaxableAmounts.originalAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t.incomesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.notProvided() }}
            </span>
          </SaOverviewItemDetailsSectionAttribute>
        </div>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="income.attachments.length"
        :title="$t.incomesOverviewPanel.attachments.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="income.attachments" />
          </div>
        </div>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="income.notes"
        :title="$t.incomesOverviewPanel.notes.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="income.notes" />
          </div>
        </div>
      </SaOverviewItemDetailsSection>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue';
import SaCategoryOutput from '@/components/category/SaCategoryOutput.vue';
import SaDocumentsList from '@/components/documents/SaDocumentsList.vue';
import SaGeneralTaxOutput from '@/components/general-tax/SaGeneralTaxOutput.vue';
import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
import SaOverviewItemAmountPanel from '@/components/overview-item/SaOverviewItemAmountPanel.vue';
import SaOverviewItemAttributePreviewIcon from '@/components/overview-item/SaOverviewItemAttributePreviewIcon.vue';
import SaOverviewItemDetailsSection from '@/components/overview-item/SaOverviewItemDetailsSection.vue';
import SaOverviewItemDetailsSectionActions from '@/components/overview-item/SaOverviewItemDetailsSectionActions.vue';
import SaOverviewItemDetailsSectionAttribute from '@/components/overview-item/SaOverviewItemDetailsSectionAttribute.vue';
import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
import SaActionLink from '@/components/SaActionLink.vue';
import SaMarkdownOutput from '@/components/SaMarkdownOutput.vue';
import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
import SaOutputLoader from '@/components/SaOutputLoader.vue';
import SaStatusLabel, { type StatusLabelStatus } from '@/components/SaStatusLabel.vue';
import type { IncomeDto } from '@/services/api';
import { invoicesApi } from '@/services/api';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { ensureDefined } from '@/services/utils';
import { useCurrentWorkspace } from '@/services/workspaces';

const props = defineProps<{ income: IncomeDto }>();

const { defaultCurrency, currentWorkspace } = useCurrentWorkspace();

type IncomeStatus = {
  isSuccess: boolean;
  value: StatusLabelStatus;
  shortText: string;
  fullText: string;
};
const incomeStatus = computed<IncomeStatus>(() => {
  const statusProto: IncomeStatus = {
    isSuccess: false,
    value: 'pending',
    shortText: $t.value.incomesOverviewPanel.status.short.pending(),
    fullText: '',
  };
  if (props.income.status === 'FINALIZED') {
    return {
      ...statusProto,
      isSuccess: true,
      value: 'success',
      shortText: $t.value.incomesOverviewPanel.status.short.finalized(),
      fullText: $t.value.incomesOverviewPanel.status.full.finalized(),
    };
  }
  if (props.income.status === 'PENDING_CONVERSION') {
    return {
      ...statusProto,
      fullText: $t.value.incomesOverviewPanel.status.full.pendingConversion(defaultCurrency),
    };
  }
  return {
    ...statusProto,
    fullText: $t.value.incomesOverviewPanel.status.full.waitingExchangeRate(),
  };
});

const totalAmount = computed(() => {
  if (props.income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency) {
    return {
      value: props.income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency,
      currency: defaultCurrency,
    };
  }
  if (props.income.convertedAmounts.adjustedAmountInDefaultCurrency) {
    return {
      value: props.income.convertedAmounts.adjustedAmountInDefaultCurrency,
      currency: defaultCurrency,
    };
  }
  return {
    value: props.income.originalAmount,
    currency: props.income.currency,
  };
});

const isForeignCurrency = computed(() => props.income.currency !== defaultCurrency);

const isGeneralTaxApplicable = computed(() => props.income.generalTax != null);

const { navigateToView } = useNavigation();
const navigateToIncomeEdit = () =>
  navigateToView({
    name: 'edit-income',
    params: { id: props.income.id },
  });

const linkedInvoice = ref({
  loading: false,
  exists: props.income.linkedInvoice != null,
  title: null as string | null,
});

async function loadLinkedInvoice() {
  if (props.income.linkedInvoice) {
    linkedInvoice.value.loading = true;
    try {
      const { currentWorkspaceId } = useCurrentWorkspace();
      const invoiceResponse = await invoicesApi.getInvoice({
        invoiceId: props.income.linkedInvoice,
        workspaceId: currentWorkspaceId,
      });
      linkedInvoice.value.title = invoiceResponse.title;
    } finally {
      linkedInvoice.value.loading = false;
    }
  }
}

loadLinkedInvoice();
</script>
