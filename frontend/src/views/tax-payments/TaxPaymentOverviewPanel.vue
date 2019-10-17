<template>
  <OverviewItem :title="taxPayment.title"
                @details-shown="loadAttachments">
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute v-if="datePaid"
                                    tooltip="Date paid"
                                    icon="calendar">
        {{datePaid}}
      </OverviewItemPrimaryAttribute>
    </template>

    <template v-slot:attributes-preview>
      <OverviewItemAttributePreviewIcon v-if="taxPayment.notes"
                                        icon="notes"
                                        tooltip="Additional notes provided"/>

      <OverviewItemAttributePreviewIcon v-if="taxPayment.attachments.length"
                                        tooltip="Attachments provided"
                                        icon="attachment"/>
    </template>

    <template v-slot:last-column>
      <OverviewItemAmountPanel :currency="defaultCurrency"
                               :amount="taxPayment.amount"/>
    </template>

    <template v-slot:details>
      <OverviewItemDetailsSectionActions>
        <SaActionLink icon="pencil-solid"
                      v-if="currentWorkspace.editable"
                      @click="navigateToTaxPaymentEdit">
          Edit
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection title="Summary">
        <div class="row">
          <OverviewItemDetailsSectionAttribute label="Date Paid"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{datePaid}}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Reporting Date"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{reportingDate}}
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
                                  v-if="taxPayment.notes">
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="taxPayment.notes"/>
          </div>
        </div>
      </OverviewItemDetailsSection>
    </template>
  </OverviewItem>
</template>

<script>
  import {withMediumDateFormatter} from '@/components/mixins/with-medium-date-formatter'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import {loadDocuments} from '@/services/app-services'
  import DocumentLink from '@/components/DocumentLink'
  import MoneyOutput from '@/components/MoneyOutput'
  import OverviewItem from '@/components/overview-item/OverviewItem'
  import OverviewItemAmountPanel from '@/components/overview-item/OverviewItemAmountPanel'
  import OverviewItemAttributePreviewIcon from '@/components/overview-item/OverviewItemAttributePreviewIcon'
  import OverviewItemDetailsSection from '@/components/overview-item/OverviewItemDetailsSection'
  import OverviewItemDetailsSectionActions from '@/components/overview-item/OverviewItemDetailsSectionActions'
  import OverviewItemDetailsSectionAttribute from '@/components/overview-item/OverviewItemDetailsSectionAttribute'
  import OverviewItemPrimaryAttribute from '@/components/overview-item/OverviewItemPrimaryAttribute'
  import SaActionLink from '@/components/SaActionLink'
  import SaMarkdownOutput from '@/components/SaMarkdownOutput'

  export default {
    name: 'TaxPaymentOverviewPanel',

    mixins: [withMediumDateFormatter, withWorkspaces],

    components: {
      OverviewItemDetailsSectionAttribute,
      OverviewItemDetailsSection,
      SaActionLink,
      OverviewItemDetailsSectionActions,
      OverviewItemAmountPanel,
      OverviewItemAttributePreviewIcon,
      OverviewItemPrimaryAttribute,
      OverviewItem,
      MoneyOutput,
      DocumentLink,
      SaMarkdownOutput
    },

    props: {
      taxPayment: {
        type: Object,
        required: true
      }
    },

    data: function () {
      return {
        attachments: []
      }
    },

    computed: {
      datePaid: function () {
        return this.mediumDateFormatter(new Date(this.taxPayment.datePaid))
      },

      reportingDate: function () {
        return this.mediumDateFormatter(new Date(this.taxPayment.reportingDate))
      }
    },

    methods: {
      loadAttachments: async function () {
        if (this.taxPayment.attachments.length && !this.attachments.length) {
          this.attachments = await loadDocuments(
              this.attachments,
              this.taxPayment.attachments,
              this.currentWorkspace.id)
        }
      },

      navigateToTaxPaymentEdit: function () {
        this.$router.push({name: 'edit-tax-payment', params: {id: this.taxPayment.id}})
      }
    }
  }
</script>