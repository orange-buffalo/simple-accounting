<template>
  <OverviewItem :title="income.title">
    <template #primary-attributes>
      <OverviewItemPrimaryAttribute
        v-if="income.dateReceived"
        :tooltip="$t.incomesOverviewPanel.dateReceived.tooltip()"
        icon="calendar"
      >
        {{ $t('common.date.medium', [income.dateReceived]) }}
      </OverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <OverviewItemAttributePreviewIcon
        v-if="income.notes"
        icon="notes"
        :tooltip="$t.incomesOverviewPanel.notes.tooltip()"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        :tooltip="$t.incomesOverviewPanel.generalTax.tooltip()"
        icon="tax"
      />

      <OverviewItemAttributePreviewIcon
        v-if="income.attachments.length"
        :tooltip="$t.incomesOverviewPanel.attachments.tooltip()"
        icon="attachment"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        :tooltip="$t.incomesOverviewPanel.foreignCurrency.tooltip()"
        icon="multi-currency"
      />

      <OverviewItemAttributePreviewIcon
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
      <OverviewItemAmountPanel
        :currency="totalAmount.currency"
        :amount="totalAmount.value"
      />
    </template>

    <template #details>
      <OverviewItemDetailsSectionActions>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToIncomeEdit"
        >
          {{ $t.incomesOverviewPanel.edit() }}
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection
        :title="$t.incomesOverviewPanel.summary.header()"
      >
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.status.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaStatusLabel
              :status="incomeStatus.value"
              :simplified="true"
            >
              {{ incomeStatus.fullText }}
            </SaStatusLabel>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.category.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaCategoryOutput :category-id="income.category" />
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.dateReceived.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [income.dateReceived]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.incomeTaxableAmounts.adjustedAmountInDefaultCurrency.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
            />

            <span v-else>Not yet provided</span>
          </OverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isGeneralTaxApplicable">
          <div class="row">
            <OverviewItemDetailsSectionAttribute
              :label="$t.incomesOverviewPanel.generalTax.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <SaGeneralTaxOutput :general-tax-id="income.generalTax" />
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              :label="$t.incomesOverviewPanel.generalTaxRate.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ $t('incomesOverviewPanel.generalTaxRate.value', [income.generalTaxRateInBps]) }}
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              :label="$t.incomesOverviewPanel.generalTaxAmount.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <MoneyOutput
                v-if="income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
                :currency="defaultCurrency"
                :amount="income.generalTaxAmount"
              />

              <span v-else>{{ $t.incomesOverviewPanel.generalTaxAmount.notProvided() }}</span>
            </OverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        :title="$t.incomesOverviewPanel.generalInformation.header()"
      >
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            v-if="isForeignCurrency"
            :label="$t.incomesOverviewPanel.originalCurrency.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ income.currency }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.originalAmount.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              :currency="income.currency"
              :amount="income.originalAmount"
            />
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="linkedInvoice.exists"
            :label="$t.incomesOverviewPanel.linkedInvoice.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaOutputLoader :loading="linkedInvoice.loading">
              {{ linkedInvoice.title }}
            </SaOutputLoader>
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="isForeignCurrency"
        :title="$t.incomesOverviewPanel.foreignCurrency.header()"
      >
        <div class="row">
          <!-- eslint-disable -->
          <OverviewItemDetailsSectionAttribute
            :label="$t('incomesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.label', [defaultCurrency])"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="income.convertedAmounts.originalAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="income.convertedAmounts.originalAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t.incomesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.notProvided() }}
            </span>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t.incomesOverviewPanel.differentExchangeRate.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('incomesOverviewPanel.differentExchangeRate.value', [income.useDifferentExchangeRateForIncomeTaxPurposes]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('incomesOverviewPanel.incomeTaxableAmounts.originalAmountInDefaultCurrency.label', [defaultCurrency])"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="income.incomeTaxableAmounts.originalAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="income.incomeTaxableAmounts.originalAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t.incomesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.notProvided() }}
            </span>
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="income.attachments.length"
        :title="$t.incomesOverviewPanel.attachments.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="income.attachments" />
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="income.notes"
        :title="$t.incomesOverviewPanel.notes.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="income.notes" />
          </div>
        </div>
      </OverviewItemDetailsSection>
    </template>
  </OverviewItem>
</template>

<script lang="ts">
  import {
    toRefs, computed, ref, defineComponent, PropType, Ref,
  } from '@vue/composition-api';
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
  import i18n from '@/services/i18n';
  import useNavigation from '@/components/navigation/useNavigation';
  import SaOutputLoader from '@/components/SaOutputLoader';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { apiClient, IncomeDto } from '@/services/api';

  function useIncomeStatus(income: Ref<IncomeDto>) {
    const { defaultCurrency } = useCurrentWorkspace();
    const incomeStatus = computed(() => {
      const statusProto = {
        isSuccess: false,
        value: 'pending',
        shortText: i18n.t('incomesOverviewPanel.status.short.pending'),
      };
      if (income.value.status === 'FINALIZED') {
        return {
          ...statusProto,
          isSuccess: true,
          value: 'success',
          shortText: i18n.t('incomesOverviewPanel.status.short.finalized'),
          fullText: i18n.t('incomesOverviewPanel.status.full.finalized'),
        };
      }
      if (income.value.status === 'PENDING_CONVERSION') {
        return {
          ...statusProto,
          fullText: i18n.t('incomesOverviewPanel.status.full.pendingConversion', [defaultCurrency]),
        };
      }
      return {
        ...statusProto,
        fullText: i18n.t('incomesOverviewPanel.status.full.waitingExchangeRate'),
      };
    });
    return { incomeStatus };
  }

  function useUiState(income:Ref<IncomeDto>) {
    const { defaultCurrency } = useCurrentWorkspace();

    const totalAmount = computed(() => {
      if (income.value.incomeTaxableAmounts.adjustedAmountInDefaultCurrency) {
        return {
          value: income.value.incomeTaxableAmounts.adjustedAmountInDefaultCurrency,
          currency: defaultCurrency,
        };
      }
      if (income.value.convertedAmounts.adjustedAmountInDefaultCurrency) {
        return {
          value: income.value.convertedAmounts.adjustedAmountInDefaultCurrency,
          currency: defaultCurrency,
        };
      }
      return {
        value: income.value.originalAmount,
        currency: income.value.currency,
      };
    });

    const isForeignCurrency = computed(() => income.value.currency !== defaultCurrency);

    const isGeneralTaxApplicable = computed(() => income.value.generalTax != null);

    return {
      isForeignCurrency,
      isGeneralTaxApplicable,
      totalAmount,
    };
  }

  function useIncomeNavigation(income:Ref<IncomeDto>) {
    const { navigateToView } = useNavigation();
    const navigateToIncomeEdit = () => navigateToView({
      name: 'edit-income',
      params: { id: income.value.id },
    });
    return { navigateToIncomeEdit };
  }

  function useLinkedInvoice(income:Ref<IncomeDto>) {
    const linkedInvoice = ref({
      loading: false,
      exists: income.value.linkedInvoice != null,
      title: null as String | null,
    });

    async function loadLinkedInvoice() {
      if (income.value.linkedInvoice) {
        linkedInvoice.value.loading = true;
        try {
          const { currentWorkspaceId } = useCurrentWorkspace();
          const invoiceResponse = await apiClient.getInvoice({
            invoiceId: income.value.linkedInvoice,
            workspaceId: currentWorkspaceId,
          });
          linkedInvoice.value.title = invoiceResponse.data.title;
        } finally {
          linkedInvoice.value.loading = false;
        }
      }
    }

    loadLinkedInvoice();

    return { linkedInvoice };
  }

  export default defineComponent({
    components: {
      SaOutputLoader,
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
      income: {
        type: Object as PropType<IncomeDto>,
        required: true,
      },
    },

    setup(props) {
      const { income } = toRefs(props);

      return {
        ...useIncomeStatus(income),
        ...useUiState(income),
        ...useCurrentWorkspace(),
        ...useIncomeNavigation(income),
        ...useLinkedInvoice(income),
      };
    },
  });
</script>
