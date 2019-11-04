<template>
  <OverviewItem
    :title="income.title"
    @details-shown="loadAttachments"
  >
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute
        v-if="dateReceived"
        tooltip="Date received"
        icon="calendar"
      >
        {{ dateReceived }}
      </OverviewItemPrimaryAttribute>
    </template>

    <template v-slot:attributes-preview>
      <OverviewItemAttributePreviewIcon
        v-if="income.notes"
        icon="notes"
        tooltip="Additional notes provided"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        tooltip="General Tax applied"
        icon="tax"
      />

      <OverviewItemAttributePreviewIcon
        v-if="income.attachments.length"
        tooltip="Attachments provided"
        icon="attachment"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        tooltip="In foreign currency"
        icon="multi-currency"
      />

      <OverviewItemAttributePreviewIcon
        v-if="income.linkedInvoice"
        tooltip="Invoice associated"
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
          Edit
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection title="Summary">
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            label="Status"
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
            label="Category"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ categoryById(income.category).name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Date Received"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ dateReceived }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Amount for Taxation Purposes"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="income.reportedAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="income.reportedAmountInDefaultCurrency"
            />

            <span v-else>Not yet provided</span>
          </OverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isGeneralTaxApplicable">
          <div class="row">
            <OverviewItemDetailsSectionAttribute
              label="Applicable General Tax"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ generalTaxTitle }}
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              label="Applicable General Tax Rate"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <!-- todo #6 localize-->
              {{ income.generalTaxRateInBps / 100 }}%
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              label="Applicable General Tax Amount"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <MoneyOutput
                v-if="isGeneralTaxAmountAvailable"
                :currency="defaultCurrency"
                :amount="income.generalTaxAmount"
              />

              <span v-else>Not yet available</span>
            </OverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection title="General Information">
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            v-if="isForeignCurrency"
            label="Original Currency"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ income.currency }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Original Amount"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              :currency="income.currency"
              :amount="income.originalAmount"
            />
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="income.linkedInvoice"
            label="Associated Invoice"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ income.linkedInvoice.title }}
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="isForeignCurrency"
        title="Currency Conversion"
      >
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            :label="`Amount in ${defaultCurrency}`"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="income.amountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="income.amountInDefaultCurrency"
            />

            <span v-else>Not yet available</span>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Using different exchange rate for taxation purposes?"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <!-- todo #6 localize -->
            <span v-if="isReportedDifferentExchangeRate">Yes</span>
            <span v-else>No</span>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="`Amount in ${defaultCurrency} for taxation purposes`"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="income.reportedAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="income.reportedAmountInDefaultCurrency"
            />

            <span v-else>Not yet available</span>
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="attachments.length"
        title="Attachments"
      >
        <div class="row">
          <div class="col col-xs-12">
            <span
              v-for="attachment in attachments"
              :key="attachment.id"
            >
              <DocumentLink :document="attachment" /><br>
            </span>
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="income.notes"
        title="Additional Notes"
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
  import { isNil } from 'lodash/lang';
  import withMediumDateFormatter from '@/components/mixins/with-medium-date-formatter';
  import withCategories from '@/components/mixins/with-categories';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import withGeneralTaxes from '@/components/mixins/with-general-taxes';
  import { loadDocuments } from '@/services/app-services';
  import DocumentLink from '@/components/DocumentLink';
  import MoneyOutput from '@/components/MoneyOutput';
  import OverviewItem from '@/components/overview-item/OverviewItem';
  import OverviewItemAmountPanel from '@/components/overview-item/OverviewItemAmountPanel';
  import OverviewItemAttributePreviewIcon from '@/components/overview-item/OverviewItemAttributePreviewIcon';
  import OverviewItemDetailsSection from '@/components/overview-item/OverviewItemDetailsSection';
  import OverviewItemDetailsSectionActions from '@/components/overview-item/OverviewItemDetailsSectionActions';
  import OverviewItemDetailsSectionAttribute from '@/components/overview-item/OverviewItemDetailsSectionAttribute';
  import OverviewItemPrimaryAttribute from '@/components/overview-item/OverviewItemPrimaryAttribute';
  import SaActionLink from '@/components/SaActionLink';
  import SaIcon from '@/components/SaIcon';
  import SaMarkdownOutput from '@/components/SaMarkdownOutput';
  import SaStatusLabel from '@/components/SaStatusLabel';

  export default {
    name: 'IncomeOverviewPanel',

    components: {
      MoneyOutput,
      DocumentLink,
      OverviewItem,
      SaIcon,
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

    mixins: [withMediumDateFormatter, withWorkspaces, withCategories, withGeneralTaxes],

    props: {
      income: {
        type: Object,
        required: true,
      },
    },

    data() {
      return {
        attachments: [],
      };
    },

    computed: {
      status() {
        return this.income.status === 'FINALIZED' ? 'success' : 'pending';
      },

      shortStatusText() {
        return this.income.status === 'FINALIZED' ? 'Finalized' : 'Pending';
      },

      fullStatusText() {
        if (this.income.status === 'FINALIZED') {
          return 'Finalized';
        } if (this.income.status === 'PENDING_CONVERSION') {
          return `Conversion to ${this.defaultCurrency} pending`;
        }
        return 'Waiting for exchange rate';
      },

      totalAmount() {
        if (this.income.status === 'FINALIZED') {
          return {
            value: this.income.reportedAmountInDefaultCurrency,
            currency: this.defaultCurrency,
          };
        } if (this.income.status === 'PENDING_CONVERSION') {
          return {
            value: this.income.originalAmount,
            currency: this.income.currency,
          };
        }
        return {
          value: this.income.amountInDefaultCurrency,
          currency: this.defaultCurrency,
        };
      },

      isForeignCurrency() {
        return this.income.currency !== this.defaultCurrency;
      },

      isReportedDifferentExchangeRate() {
        return !isNil(this.income.reportedAmountInDefaultCurrency)
          && (this.income.reportedAmountInDefaultCurrency !== this.income.amountInDefaultCurrency);
      },

      amountInDefaultCurrency() {
        return this.income.currency === this.defaultCurrency
          ? this.income.originalAmount : this.income.amountInDefaultCurrency;
      },

      isConverted() {
        return this.income.amountInDefaultCurrency;
      },

      dateReceived() {
        return this.mediumDateFormatter(new Date(this.income.dateReceived));
      },

      isGeneralTaxApplicable() {
        return this.income.generalTax && this.generalTaxTitle;
      },

      isGeneralTaxAmountAvailable() {
        return this.income.status === 'FINALIZED';
      },

      generalTaxTitle() {
        return this.generalTaxById(this.income.generalTax).title;
      },
    },

    methods: {
      async loadAttachments() {
        if (this.income.attachments.length && !this.attachments.length) {
          this.attachments = await loadDocuments(
            this.attachments,
            this.income.attachments,
            this.currentWorkspace.id,
          );
        }
      },

      navigateToIncomeEdit() {
        this.$router.push({ name: 'edit-income', params: { id: this.income.id } });
      },
    },
  };
</script>
