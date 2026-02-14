<template>
  <SaOverviewItem :title="expense.title">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        v-if="expense.datePaid"
        :tooltip="$t.expensesOverviewPanel.datePaid.tooltip()"
        icon="calendar"
      >
        {{ $t.common.date.medium(expense.datePaid) }}
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <SaOverviewItemAttributePreviewIcon
        v-if="expense.notes"
        icon="notes"
        :tooltip="$t.expensesOverviewPanel.notes.tooltip()"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        :tooltip="$t.expensesOverviewPanel.generalTax.tooltip()"
        icon="tax"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="expense.attachments.length"
        :tooltip="$t.expensesOverviewPanel.attachments.tooltip()"
        icon="attachment"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        :tooltip="$t.expensesOverviewPanel.foreignCurrency.tooltip()"
        icon="multi-currency"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="expense.percentOnBusiness < 100"
        :tooltip="$t.expensesOverviewPanel.partialBusinessPurpose.tooltip()"
        icon="percent"
      />
    </template>

    <template #middle-column>
      <ElTooltip
        :content="fullStatusText"
        :disabled="status === 'success'"
        placement="bottom"
      >
        <SaStatusLabel :status="status">
          {{ shortStatusText }}
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
          icon="copy"
          @click="navigateToExpenseCreateWithPrototype"
        >
          {{ $t.expensesOverviewPanel.copy() }}
        </SaActionLink>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToExpenseEdit"
        >
          {{ $t.expensesOverviewPanel.edit() }}
        </SaActionLink>
      </SaOverviewItemDetailsSectionActions>

      <SaOverviewItemDetailsSection :title="$t.expensesOverviewPanel.summary.header()">
        <div class="row">
          <SaOverviewItemDetailsSectionAttribute
            :label="$t.expensesOverviewPanel.status.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaStatusLabel
              :status="status"
              :simplified="true"
            >
              {{ fullStatusText }}
            </SaStatusLabel>
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.expensesOverviewPanel.category.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaCategoryOutput :category-id="expense.category" />
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.expensesOverviewPanel.datePaid.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t.common.date.medium(expense.datePaid) }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.expensesOverviewPanel.incomeTaxableAmounts.adjustedAmountInDefaultCurrency.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaMoneyOutput
              v-if="expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount-in-cents="expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t.expensesOverviewPanel.incomeTaxableAmounts.adjustedAmountInDefaultCurrency.notProvided() }}
            </span>
          </SaOverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isGeneralTaxApplicable">
          <div class="row">
            <SaOverviewItemDetailsSectionAttribute
              :label="$t.expensesOverviewPanel.generalTax.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <SaGeneralTaxOutput :general-tax-id="expense.generalTax" />
            </SaOverviewItemDetailsSectionAttribute>

            <SaOverviewItemDetailsSectionAttribute
              :label="$t.expensesOverviewPanel.generalTaxRate.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ $t.expensesOverviewPanel.generalTaxRate.value(expense.generalTaxRateInBps || 0) }}
            </SaOverviewItemDetailsSectionAttribute>

            <SaOverviewItemDetailsSectionAttribute
              :label="$t.expensesOverviewPanel.generalTaxAmount.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <SaMoneyOutput
                v-if="expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
                :currency="defaultCurrency"
                :amount-in-cents="expense.generalTaxAmount"
              />

              <span v-else>{{ $t.expensesOverviewPanel.generalTaxAmount.notProvided() }}</span>
            </SaOverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection :title="$t.expensesOverviewPanel.generalInformation.header()">
        <div class="row">
          <SaOverviewItemDetailsSectionAttribute
            v-if="isForeignCurrency"
            :label="$t.expensesOverviewPanel.originalCurrency.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ expense.currency }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.expensesOverviewPanel.originalAmount.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaMoneyOutput
              :currency="expense.currency"
              :amount-in-cents="expense.originalAmount"
            />
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            v-if="expense.percentOnBusiness < 100"
            :label="$t.expensesOverviewPanel.partialBusinessPurpose.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t.expensesOverviewPanel.partialBusinessPurpose.value(expense.percentOnBusiness / 100) }}
          </SaOverviewItemDetailsSectionAttribute>
        </div>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="isForeignCurrency"
        :title="$t.expensesOverviewPanel.foreignCurrency.header()"
      >
        <div class="row">
          <!-- eslint-disable max-len -->
          <SaOverviewItemDetailsSectionAttribute
            :label="$t.expensesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.label(defaultCurrency)"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaMoneyOutput
              v-if="expense.convertedAmounts.originalAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount-in-cents="expense.convertedAmounts.originalAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t.expensesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.notProvided() }}
            </span>
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.expensesOverviewPanel.differentExchangeRate.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{
              $t.expensesOverviewPanel.differentExchangeRate.value(expense.useDifferentExchangeRateForIncomeTaxPurposes)
            }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.expensesOverviewPanel.incomeTaxableAmounts.originalAmountInDefaultCurrency.label(defaultCurrency)"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaMoneyOutput
              v-if="expense.incomeTaxableAmounts.originalAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount-in-cents="expense.incomeTaxableAmounts.originalAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t.expensesOverviewPanel.incomeTaxableAmounts.originalAmountInDefaultCurrency.notProvided() }}
            </span>
          </SaOverviewItemDetailsSectionAttribute>
        </div>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="expense.attachments.length"
        :title="$t.expensesOverviewPanel.attachments.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="expense.attachments" />
          </div>
        </div>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="expense.notes"
        :title="$t.expensesOverviewPanel.notes.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="expense.notes" />
          </div>
        </div>
      </SaOverviewItemDetailsSection>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
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
import SaStatusLabel from '@/components/SaStatusLabel.vue';
import type { ExpenseDto } from '@/services/api';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { useCurrentWorkspace } from '@/services/workspaces';

const props = defineProps<{
  expense: ExpenseDto;
}>();

const { defaultCurrency, currentWorkspace } = useCurrentWorkspace();
const { navigateToView } = useNavigation();

const status = computed(() => (props.expense.status === 'FINALIZED' ? 'success' : 'pending'));

const shortStatusText = computed(() =>
  props.expense.status === 'FINALIZED'
    ? $t.value.expensesOverviewPanel.status.short.finalized()
    : $t.value.expensesOverviewPanel.status.short.pending(),
);

const fullStatusText = computed(() => {
  if (props.expense.status === 'FINALIZED') {
    return $t.value.expensesOverviewPanel.status.full.finalized();
  }
  if (props.expense.status === 'PENDING_CONVERSION') {
    return $t.value.expensesOverviewPanel.status.full.pendingConversion(defaultCurrency);
  }
  return $t.value.expensesOverviewPanel.status.full.waitingExchangeRate();
});

const totalAmount = computed(() => {
  if (props.expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency) {
    return {
      value: props.expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency,
      currency: defaultCurrency,
    };
  }
  if (props.expense.convertedAmounts.adjustedAmountInDefaultCurrency) {
    return {
      value: props.expense.convertedAmounts.adjustedAmountInDefaultCurrency,
      currency: defaultCurrency,
    };
  }
  return {
    value: props.expense.originalAmount,
    currency: props.expense.currency,
  };
});

const isForeignCurrency = computed(() => props.expense.currency !== defaultCurrency);

const isGeneralTaxApplicable = computed(() => props.expense.generalTax != null);

const navigateToExpenseEdit = () =>
  navigateToView({
    name: 'edit-expense',
    params: { id: props.expense.id },
  });

const navigateToExpenseCreateWithPrototype = () =>
  navigateToView({
    name: 'create-new-expense',
    params: { prototype: props.expense.id },
  });
</script>
