<template>
  <OverviewItem :title="expense.title"
                @details-shown="loadAttachments">
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute v-if="datePaid"
                                    tooltip="Date paid"
                                    icon="calendar">
        {{datePaid}}
      </OverviewItemPrimaryAttribute>
    </template>

    <template v-slot:attributes-preview>
      <OverviewItemAttributePreviewIcon v-if="expense.notes"
                                        icon="notes"
                                        tooltip="Additional notes provided"/>

      <OverviewItemAttributePreviewIcon v-if="expense.tax && taxById(expense.tax).title"
                                        tooltip="Tax applied"
                                        icon="tax"/>

      <OverviewItemAttributePreviewIcon v-if="expense.attachments.length"
                                        tooltip="Attachments provided"
                                        icon="attachment"/>

      <OverviewItemAttributePreviewIcon v-if="isForeignCurrency"
                                        tooltip="In foreign currency"
                                        icon="multi-currency"/>

      <OverviewItemAttributePreviewIcon v-if="expense.percentOnBusiness < 100"
                                        tooltip="Partial business purpose"
                                        icon="percent"/>
    </template>

    <template v-slot:middle-column>
      <ElTooltip :content="fullStatusText"
                 :disabled="status === 'success'"
                 placement="bottom">
        <SaStatusLabel :status="status">{{ shortStatusText }}</SaStatusLabel>
      </ElTooltip>
    </template>

    <template v-slot:last-column>
      <OverviewItemAmountPanel :currency="totalAmount.currency"
                               :amount="totalAmount.value"/>
    </template>

    <template v-slot:details>
      <OverviewItemDetailsSectionActions>
        <SaActionLink icon="pencil" @click="navigateToExpenseEdit">
          Edit
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection title="Summary">
        <div class="row">
          <OverviewItemDetailsSectionAttribute label="Status"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            <SaStatusLabel :status="status" :simplified="true">{{ fullStatusText }}</SaStatusLabel>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Category"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{ categoryById(expense.category).name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Date Paid"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{datePaid}}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Amount for Taxation Purposes"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            <MoneyOutput v-if="expense.reportedAmountInDefaultCurrency"
                         :currency="currentWorkspace.defaultCurrency"
                         :amount="expense.reportedAmountInDefaultCurrency"/>

            <span v-else>Not yet provided</span>
          </OverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isTaxApplicable">
          <div class="row">
            <OverviewItemDetailsSectionAttribute label="Applicable Tax"
                                                 class="col col-xs-12 col-md-6 col-lg-4">
              {{taxTitle}}
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute label="Applicable Tax Rate"
                                                 class="col col-xs-12 col-md-6 col-lg-4">
              <!-- todo #6 localize-->
              {{expense.taxRateInBps / 100}}%
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute label="Applicable Tax Amount"
                                                 class="col col-xs-12 col-md-6 col-lg-4">
              <MoneyOutput v-if="expense.taxAmount"
                           :currency="currentWorkspace.defaultCurrency"
                           :amount="expense.taxAmount"/>

              <span v-else>Not yet available</span>
            </OverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection title="General Information">
        <div class="row">
          <OverviewItemDetailsSectionAttribute v-if="isForeignCurrency"
                                               label="Original Currency"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{expense.currency}}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Original Amount"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            <MoneyOutput :currency="expense.currency"
                         :amount="expense.originalAmount"/>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute v-if="expense.percentOnBusiness < 100"
                                               label="Partial Business Purpose"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            <!-- todo #6 localize -->
            {{expense.percentOnBusiness}}% related to business activities
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection v-if="isForeignCurrency"
                                  title="Currency Conversion">
        <div class="row">
          <OverviewItemDetailsSectionAttribute :label="`Amount in ${currentWorkspace.defaultCurrency}`"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            <MoneyOutput v-if="expense.amountInDefaultCurrency"
                         :currency="currentWorkspace.defaultCurrency"
                         :amount="expense.amountInDefaultCurrency"/>

            <span v-if="!expense.amountInDefaultCurrency">
              Not yet available
            </span>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Using different exchange rate for taxation purposes?"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            <!-- todo #6 localize -->
            <span v-if="isReportedDifferentExchangeRate">Yes</span>
            <span v-else>No</span>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
              :label="`Amount in ${currentWorkspace.defaultCurrency} for taxation purposes`"
              class="col col-xs-12 col-md-6 col-lg-4">
            <MoneyOutput v-if="expense.actualAmountInDefaultCurrency"
                         :currency="currentWorkspace.defaultCurrency"
                         :amount="expense.actualAmountInDefaultCurrency"/>

            <span v-if="!expense.actualAmountInDefaultCurrency">
              Not yet available
            </span>
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection title="Attachments"
                                  v-if="attachments.length">
        <div class="row">
          <div class="col col-xs-12">
            <span v-for="attachment in attachments"
                  :key="attachment.id">
             <document-link :document="attachment"/><br/>
            </span>
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection title="Additional Notes"
                                  v-if="expense.notes">
        <div class="row">
          <div class="col col-xs-12">
            {{expense.notes}}
          </div>
        </div>
      </OverviewItemDetailsSection>
    </template>
  </OverviewItem>
</template>

<script>
  import MoneyOutput from '@/components/MoneyOutput'
  import DocumentLink from '@/components/DocumentLink'
  import {withMediumDateFormatter} from '@/components/mixins/with-medium-date-formatter'
  import {withCategories} from '@/components/mixins/with-categories'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import {loadDocuments} from '@/services/app-services'
  import {withTaxes} from '@/components/mixins/with-taxes'
  import OverviewItem from '@/components/overview-item/OverviewItem'
  import SaIcon from '@/components/SaIcon'
  import OverviewItemAttributePreviewIcon from '@/components/overview-item/OverviewItemAttributePreviewIcon'
  import OverviewItemPrimaryAttribute from '@/components/overview-item/OverviewItemPrimaryAttribute'
  import OverviewItemDetailsSection from '@/components/overview-item/OverviewItemDetailsSection'
  import OverviewItemDetailsSectionAttribute from '@/components/overview-item/OverviewItemDetailsSectionAttribute'
  import {isNil} from 'lodash/lang'
  import SaActionLink from '@/components/SaActionLink'
  import OverviewItemDetailsSectionActions from '@/components/overview-item/OverviewItemDetailsSectionActions'
  import OverviewItemAmountPanel from '@/components/overview-item/OverviewItemAmountPanel'
  import SaStatusLabel from '@/components/SaStatusLabel'

  export default {
    name: 'ExpenseOverviewPanel',

    mixins: [withMediumDateFormatter, withCategories, withWorkspaces, withTaxes],

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
      SaStatusLabel
    },

    props: {
      expense: Object
    },

    data: function () {
      return {
        notesVisible: false,
        attachmentsVisible: false,
        attachments: []
      }
    },

    computed: {
      status: function () {
        return this.expense.status === 'FINALIZED' ? 'success' : 'pending'
      },

      shortStatusText: function () {
        return this.expense.status === 'FINALIZED' ? 'Finalized' : 'Pending'
      },

      fullStatusText: function () {
        if (this.expense.status === 'FINALIZED') {
          return 'Finalized'
        } else if (this.expense.status === 'PENDING_CONVERSION') {
          return `Conversion to ${this.defaultCurrency} pending`
        } else {
          return `Waiting for exchange rate`
        }
      },

      totalAmount: function () {
        if (this.expense.status === 'FINALIZED') {
          return {
            value: this.expense.reportedAmountInDefaultCurrency,
            currency: this.defaultCurrency
          }
        } else if (this.expense.status === 'PENDING_CONVERSION') {
          return {
            value: this.expense.originalAmount,
            currency: this.expense.currency
          }
        } else {
          return {
            value: this.expense.amountInDefaultCurrency,
            currency: this.defaultCurrency
          }
        }
      },

      amountInDefaultCurrency: function () {
        return this.expense.currency === this.defaultCurrency
            ? this.expense.originalAmount : this.expense.amountInDefaultCurrency
      },

      isForeignCurrency: function () {
        return this.expense.currency !== this.defaultCurrency
      },

      isConverted: function () {
        return this.expense.amountInDefaultCurrency
      },

      isReportedDifferentExchangeRate: function () {
        return !isNil(this.expense.actualAmountInDefaultCurrency)
            && (this.expense.actualAmountInDefaultCurrency !== this.expense.amountInDefaultCurrency)
      },

      datePaid: function () {
        return this.mediumDateFormatter(new Date(this.expense.datePaid))
      },

      isTaxApplicable: function () {
        return this.expense.tax && this.taxTitle
      },

      taxTitle: function () {
        return this.taxById(this.expense.tax).title
      }
    },

    methods: {
      loadAttachments: async function () {
        if (this.expense.attachments.length && !this.attachments.length) {
          this.attachments = await loadDocuments(
              this.attachments,
              this.expense.attachments,
              this.currentWorkspace.id)
        }
      },

      navigateToExpenseEdit: function () {
        this.$router.push({name: 'edit-expense', params: {id: this.expense.id}})
      }
    }
  }
</script>
