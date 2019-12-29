<template>
  <OverviewItem :title="income.title">
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute
        v-if="income.dateReceived"
        :tooltip="$t('incomesOverviewPanel.dateReceived.tooltip')"
        icon="calendar"
      >
        {{ $t('common.date.medium', [income.dateReceived]) }}
      </OverviewItemPrimaryAttribute>
    </template>

    <template v-slot:attributes-preview>
      <OverviewItemAttributePreviewIcon
        v-if="income.notes"
        icon="notes"
        :tooltip="$t('incomesOverviewPanel.notes.tooltip')"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        :tooltip="$t('incomesOverviewPanel.generalTax.tooltip')"
        icon="tax"
      />

      <OverviewItemAttributePreviewIcon
        v-if="income.attachments.length"
        :tooltip="$t('incomesOverviewPanel.attachments.tooltip')"
        icon="attachment"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        :tooltip="$t('incomesOverviewPanel.foreignCurrency.tooltip')"
        icon="multi-currency"
      />

      <OverviewItemAttributePreviewIcon
        v-if="income.linkedInvoice"
        :tooltip="$t('incomesOverviewPanel.linkedInvoice.tooltip')"
        icon="invoice"
      />
    </template>

    <template v-slot:middle-column>
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

    <template v-slot:last-column>
      <OverviewItemAmountPanel
        :currency="totalAmount.currency"
        :amount="totalAmount.value"
      />
    </template>

    <template v-slot:details>
      <OverviewItemDetailsSectionActions>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToIncomeEdit"
        >
          {{ $t('incomesOverviewPanel.edit') }}
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection
        :title="$t('incomesOverviewPanel.summary.header')"
      >
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            :label="$t('incomesOverviewPanel.status.label')"
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
            :label="$t('incomesOverviewPanel.category.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ categoryById(income.category).name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('incomesOverviewPanel.dateReceived.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [income.dateReceived]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('incomesOverviewPanel.incomeTaxableAmounts.adjustedAmountInDefaultCurrency.label')"
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
              :label="$t('incomesOverviewPanel.generalTax.label')"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ generalTaxTitle }}
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              :label="$t('incomesOverviewPanel.generalTaxRate.label')"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ $t('incomesOverviewPanel.generalTaxRate.value', [income.generalTaxRateInBps]) }}
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              :label="$t('incomesOverviewPanel.generalTaxAmount.label')"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <MoneyOutput
                v-if="income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency"
                :currency="defaultCurrency"
                :amount="income.generalTaxAmount"
              />

              <span v-else>{{ $t('incomesOverviewPanel.generalTaxAmount.notProvided') }}</span>
            </OverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        :title="$t('incomesOverviewPanel.generalInformation.header')"
      >
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            v-if="isForeignCurrency"
            :label="$t('incomesOverviewPanel.originalCurrency.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ income.currency }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('incomesOverviewPanel.originalAmount.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              :currency="income.currency"
              :amount="income.originalAmount"
            />
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="income.linkedInvoice"
            :label="$t('incomesOverviewPanel.linkedInvoice.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ income.linkedInvoice.title }}
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="isForeignCurrency"
        :title="$t('incomesOverviewPanel.foreignCurrency.header')"
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
              {{ $t('incomesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.notProvided') }}
            </span>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('incomesOverviewPanel.differentExchangeRate.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('incomesOverviewPanel.differentExchangeRate.value', [income.useDifferentExchangeRateForIncomeTaxPurposes]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('incomesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.label', [defaultCurrency])"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="income.incomeTaxableAmounts.originalAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="income.incomeTaxableAmounts.originalAmountInDefaultCurrency"
            />

            <span v-else>
              {{ $t('incomesOverviewPanel.convertedAmounts.originalAmountInDefaultCurrency.notProvided') }}
            </span>
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="income.attachments.length"
        :title="$t('incomesOverviewPanel.attachments.header')"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="income.attachments" />
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="income.notes"
        :title="$t('incomesOverviewPanel.notes.header')"
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

<script>
  import withCategories from '@/components/mixins/with-categories';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import withGeneralTaxes from '@/components/mixins/with-general-taxes';
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

  export default {
    name: 'IncomesOverviewPanel',

    components: {
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

    mixins: [withWorkspaces, withCategories, withGeneralTaxes],

    props: {
      income: {
        type: Object,
        required: true,
      },
    },

    computed: {
      status() {
        return this.income.status === 'FINALIZED' ? 'success' : 'pending';
      },

      shortStatusText() {
        return this.income.status === 'FINALIZED'
          ? this.$t('incomesOverviewPanel.status.short.finalized')
          : this.$t('incomesOverviewPanel.status.short.pending');
      },

      fullStatusText() {
        if (this.income.status === 'FINALIZED') {
          return this.$t('incomesOverviewPanel.status.full.finalized');
        }
        if (this.income.status === 'PENDING_CONVERSION') {
          return this.$t('incomesOverviewPanel.status.full.pendingConversion', [this.defaultCurrency]);
        }
        return this.$t('incomesOverviewPanel.status.full.waitingExchangeRate');
      },

      totalAmount() {
        if (this.income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency) {
          return {
            value: this.income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency,
            currency: this.defaultCurrency,
          };
        }
        if (this.income.convertedAmounts.adjustedAmountInDefaultCurrency) {
          return {
            value: this.income.convertedAmounts.adjustedAmountInDefaultCurrency,
            currency: this.defaultCurrency,
          };
        }
        return {
          value: this.income.originalAmount,
          currency: this.income.currency,
        };
      },

      isForeignCurrency() {
        return this.income.currency !== this.defaultCurrency;
      },

      isGeneralTaxApplicable() {
        return this.income.generalTax && this.generalTaxTitle;
      },

      generalTaxTitle() {
        return this.generalTaxById(this.income.generalTax).title;
      },
    },

    methods: {
      navigateToIncomeEdit() {
        this.$router.push({
          name: 'edit-income',
          params: { id: this.income.id },
        });
      },
    },
  };
</script>
