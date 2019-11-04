<template>
  <OverviewItem
    :title="expense.title"
    @details-shown="loadAttachments"
  >
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute
        v-if="datePaid"
        tooltip="Date paid"
        icon="calendar"
      >
        {{ datePaid }}
      </OverviewItemPrimaryAttribute>
    </template>

    <template v-slot:attributes-preview>
      <OverviewItemAttributePreviewIcon
        v-if="expense.notes"
        icon="notes"
        tooltip="Additional notes provided"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        tooltip="General Tax applied"
        icon="tax"
      />

      <OverviewItemAttributePreviewIcon
        v-if="expense.attachments.length"
        tooltip="Attachments provided"
        icon="attachment"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        tooltip="In foreign currency"
        icon="multi-currency"
      />

      <OverviewItemAttributePreviewIcon
        v-if="expense.percentOnBusiness < 100"
        tooltip="Partial business purpose"
        icon="percent"
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
          icon="copy"
          @click="navigateToExpenseCreateWithPrototype"
        >
          Copy
        </SaActionLink>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToExpenseEdit"
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
            {{ categoryById(expense.category).name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Date Paid"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ datePaid }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Amount for Taxation Purposes"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              v-if="expense.reportedAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="expense.reportedAmountInDefaultCurrency"
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
              {{ expense.generalTaxRateInBps / 100 }}%
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              label="Applicable General Tax Amount"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <MoneyOutput
                v-if="isGeneralTaxAmountAvailable"
                :currency="defaultCurrency"
                :amount="expense.generalTaxAmount"
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
            {{ expense.currency }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Original Amount"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              :currency="expense.currency"
              :amount="expense.originalAmount"
            />
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="expense.percentOnBusiness < 100"
            label="Partial Business Purpose"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <!-- todo #6 localize -->
            {{ expense.percentOnBusiness }}% related to business activities
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
              v-if="expense.amountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="expense.amountInDefaultCurrency"
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
              v-if="expense.actualAmountInDefaultCurrency"
              :currency="defaultCurrency"
              :amount="expense.actualAmountInDefaultCurrency"
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
              <document-link :document="attachment" /><br>
            </span>
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="expense.notes"
        title="Additional Notes"
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

<script>
  import { isNil } from 'lodash/lang';
  import { withMediumDateFormatter } from '@/components/mixins/with-medium-date-formatter';
  import { withCategories } from '@/components/mixins/with-categories';
  import { withWorkspaces } from '@/components/mixins/with-workspaces';
  import { withGeneralTaxes } from '@/components/mixins/with-general-taxes';
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
    name: 'ExpenseOverviewPanel',

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

    mixins: [withMediumDateFormatter, withCategories, withWorkspaces, withGeneralTaxes],

    props: {
      expense: {
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
        return this.expense.status === 'FINALIZED' ? 'success' : 'pending';
      },

      shortStatusText() {
        return this.expense.status === 'FINALIZED' ? 'Finalized' : 'Pending';
      },

      fullStatusText() {
        if (this.expense.status === 'FINALIZED') {
          return 'Finalized';
        } if (this.expense.status === 'PENDING_CONVERSION') {
          return `Conversion to ${this.defaultCurrency} pending`;
        }
        return 'Waiting for exchange rate';
      },

      totalAmount() {
        if (this.expense.status === 'FINALIZED') {
          return {
            value: this.expense.reportedAmountInDefaultCurrency,
            currency: this.defaultCurrency,
          };
        } if (this.expense.status === 'PENDING_CONVERSION') {
          return {
            value: this.expense.originalAmount,
            currency: this.expense.currency,
          };
        }
        return {
          value: this.expense.amountInDefaultCurrency,
          currency: this.defaultCurrency,
        };
      },

      amountInDefaultCurrency() {
        return this.expense.currency === this.defaultCurrency
          ? this.expense.originalAmount : this.expense.amountInDefaultCurrency;
      },

      isForeignCurrency() {
        return this.expense.currency !== this.defaultCurrency;
      },

      isConverted() {
        return this.expense.amountInDefaultCurrency;
      },

      isReportedDifferentExchangeRate() {
        return !isNil(this.expense.actualAmountInDefaultCurrency)
          && (this.expense.actualAmountInDefaultCurrency !== this.expense.amountInDefaultCurrency);
      },

      datePaid() {
        return this.mediumDateFormatter(new Date(this.expense.datePaid));
      },

      isGeneralTaxApplicable() {
        return this.expense.generalTax && this.generalTaxTitle;
      },

      isGeneralTaxAmountAvailable() {
        return this.expense.status === 'FINALIZED';
      },

      generalTaxTitle() {
        return this.generalTaxById(this.expense.generalTax).title;
      },
    },

    methods: {
      async loadAttachments() {
        if (this.expense.attachments.length && !this.attachments.length) {
          this.attachments = await loadDocuments(
            this.attachments,
            this.expense.attachments,
            this.currentWorkspace.id,
          );
        }
      },

      navigateToExpenseEdit() {
        this.$router.push({ name: 'edit-expense', params: { id: this.expense.id } });
      },

      navigateToExpenseCreateWithPrototype() {
        this.$router.push({ name: 'create-new-expense', params: { prototype: this.expense } });
      },
    },
  };
</script>
