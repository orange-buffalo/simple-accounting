<template>
  <OverviewItem :title="expense.title">
    <template #primary-attributes>
      <OverviewItemPrimaryAttribute
        v-if="expense.datePaid"
        :tooltip="$t('expensesOverviewPanel.datePaid.tooltip')"
        icon="calendar"
      >
        {{ $t('common.date.medium', [expense.datePaid]) }}
      </OverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <OverviewItemAttributePreviewIcon
        v-if="expense.notes"
        icon="notes"
        :tooltip="$t('expensesOverviewPanel.notes.tooltip')"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        :tooltip="$t('expensesOverviewPanel.generalTax.tooltip')"
        icon="tax"
      />

      <OverviewItemAttributePreviewIcon
        v-if="expense.attachments.length"
        :tooltip="$t('expensesOverviewPanel.attachments.tooltip')"
        icon="attachment"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        :tooltip="$t('expensesOverviewPanel.foreignCurrency.tooltip')"
        icon="multi-currency"
      />

      <OverviewItemAttributePreviewIcon
        v-if="expense.percentOnBusiness < 100"
        :tooltip="$t('expensesOverviewPanel.partialBusinessPurpose.tooltip')"
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
      <OverviewItemAmountPanel
        :currency="totalAmount.currency"
        :amount="totalAmount.value"
      />
    </template>

    <template #details>
      <OverviewItemDetailsSectionActions>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="copy"
          @click="navigateToExpenseCreateWithPrototype"
        >
          {{ $t('expensesOverviewPanel.copy') }}
        </SaActionLink>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToExpenseEdit"
        >
          {{ $t('expensesOverviewPanel.edit') }}
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection :title="$t('expensesOverviewPanel.summary.header')">
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            :label="$t('expensesOverviewPanel.status.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaStatusLabel
              :status="status"
              :simplified="true"
            >
              {{ fullStatusText }}
            </SaStatusLabel>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('expensesOverviewPanel.category.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaCategoryOutput :category-id="expense.category" />
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('expensesOverviewPanel.datePaid.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [expense.datePaid]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('expensesOverviewPanel.incomeTaxableAmounts.adjustedAmountInDefaultCurrency.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t('expensesOverviewPanel.incomeTaxableAmounts.adjustedAmountInDefaultCurrency.notProvided') }}
            </span>
          </OverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isGeneralTaxApplicable">
          <div class="row">
            <OverviewItemDetailsSectionAttribute
              :label="$t('expensesOverviewPanel.generalTax.label')"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <SaGeneralTaxOutput :general-tax-id="expense.generalTax" />
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              :label="$t('expensesOverviewPanel.generalTaxRate.label')"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ $t('expensesOverviewPanel.generalTaxRate.value', [expense.generalTaxRateInBps]) }}
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              :label="$t('expensesOverviewPanel.generalTaxAmount.label')"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <MoneyOutput
                v-if="expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
                :currency="defaultCurrency"
                :amount="expense.generalTaxAmount"
              />

              <span v-else>{{ $t('expensesOverviewPanel.generalTaxAmount.notProvided') }}</span>
            </OverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection :title="$t('expensesOverviewPanel.generalInformation.header')">
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            v-if="isForeignCurrency"
            :label="$t('expensesOverviewPanel.originalCurrency.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ expense.currency }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('expensesOverviewPanel.originalAmount.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              :currency="expense.currency"
              :amount="expense.originalAmount"
            />
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="expense.percentOnBusiness < 100"
            :label="$t('expensesOverviewPanel.partialBusinessPurpose.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('expensesOverviewPanel.partialBusinessPurpose.value', [expense.percentOnBusiness / 100]) }}
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="isForeignCurrency"
        :title="$t('expensesOverviewPanel.foreignCurrency.header')"
      >
        <div class="row">
          <!-- eslint-disable max-len -->
          <OverviewItemDetailsSectionAttribute
            :label="$t('expensesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.label', [defaultCurrency])"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="expense.convertedAmounts.originalAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="expense.convertedAmounts.originalAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t('expensesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.notProvided') }}
            </span>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('expensesOverviewPanel.differentExchangeRate.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('expensesOverviewPanel.differentExchangeRate.value', [expense.useDifferentExchangeRateForIncomeTaxPurposes]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('expensesOverviewPanel.incomeTaxableAmounts.originalAmountInDefaultCurrency.label', [defaultCurrency])"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="expense.incomeTaxableAmounts.originalAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="expense.incomeTaxableAmounts.originalAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t('expensesOverviewPanel.incomeTaxableAmounts.originalAmountInDefaultCurrency.notProvided') }}
            </span>
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="expense.attachments.length"
        :title="$t('expensesOverviewPanel.attachments.header')"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="expense.attachments" />
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="expense.notes"
        :title="$t('expensesOverviewPanel.notes.header')"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="expense.notes" />
          </div>
        </div>
      </OverviewItemDetailsSection>
    </template>
  </OverviewItem>
</template>

<script lang="ts">
  import MoneyOutput from '@/components/MoneyOutput';
  import OverviewItem from '@/components/overview-item/OverviewItem';
  import OverviewItemAmountPanel from '@/components/overview-item/OverviewItemAmountPanel';
  import OverviewItemAttributePreviewIcon from '@/components/overview-item/OverviewItemAttributePreviewIcon';
  import OverviewItemDetailsSection from '@/components/overview-item/OverviewItemDetailsSection';
  import OverviewItemDetailsSectionActions from '@/components/overview-item/OverviewItemDetailsSectionActions';
  import OverviewItemDetailsSectionAttribute from '@/components/overview-item/OverviewItemDetailsSectionAttribute';
  import OverviewItemPrimaryAttribute from '@/components/overview-item/OverviewItemPrimaryAttribute';
  import SaActionLink from '@/components/SaActionLink';
  import SaDocumentsList from '@/components/documents/SaDocumentsList';
  import SaMarkdownOutput from '@/components/SaMarkdownOutput';
  import SaStatusLabel from '@/components/SaStatusLabel';
  import SaCategoryOutput from '@/components/category/SaCategoryOutput';
  import SaGeneralTaxOutput from '@/components/general-tax/SaGeneralTaxOutput';
  import { computed, defineComponent, PropType } from '@vue/composition-api';
  import { ExpenseDto } from '@/services/api';
  import i18n from '@/services/i18n';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/components/navigation/useNavigation';

  export default defineComponent({
    components: {
      SaGeneralTaxOutput,
      SaCategoryOutput,
      SaDocumentsList,
      MoneyOutput,
      OverviewItem,
      OverviewItemAttributePreviewIcon,
      OverviewItemPrimaryAttribute,
      OverviewItemDetailsSection,
      OverviewItemDetailsSectionAttribute,
      SaActionLink,
      OverviewItemDetailsSectionActions,
      OverviewItemAmountPanel,
      SaStatusLabel,
      SaMarkdownOutput,
    },

    props: {
      expense: {
        type: Object as PropType<ExpenseDto>,
        required: true,
      },
    },

    setup(props) {
      const { defaultCurrency, currentWorkspace } = useCurrentWorkspace();
      const { navigateToView } = useNavigation();

      const status = computed(() => (props.expense.status === 'FINALIZED' ? 'success' : 'pending'));

      const shortStatusText = computed(() => (props.expense.status === 'FINALIZED'
        ? i18n.t('expensesOverviewPanel.status.short.finalized')
        : i18n.t('expensesOverviewPanel.status.short.pending')));

      const fullStatusText = computed(() => {
        if (props.expense.status === 'FINALIZED') {
          return i18n.t('expensesOverviewPanel.status.full.finalized');
        }
        if (props.expense.status === 'PENDING_CONVERSION') {
          return i18n.t('expensesOverviewPanel.status.full.pendingConversion', [defaultCurrency]);
        }
        return i18n.t('expensesOverviewPanel.status.full.waitingExchangeRate');
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

      const isForeignCurrency = computed(() => (props.expense.currency !== defaultCurrency));

      const isGeneralTaxApplicable = computed(() => (props.expense.generalTax != null));

      const navigateToExpenseEdit = () => navigateToView({
        name: 'edit-expense',
        params: { id: `${props.expense.id}` },
      });

      const navigateToExpenseCreateWithPrototype = () => navigateToView({
        name: 'create-new-expense',
        params: { prototype: props.expense },
      });

      return {
        status,
        shortStatusText,
        fullStatusText,
        totalAmount,
        isForeignCurrency,
        isGeneralTaxApplicable,
        navigateToExpenseEdit,
        navigateToExpenseCreateWithPrototype,
        currentWorkspace,
        defaultCurrency,
      };
    },
  });
</script>
