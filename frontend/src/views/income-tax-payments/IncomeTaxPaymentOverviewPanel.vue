<template>
  <OverviewItem :title="taxPayment.title">
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
        v-if="taxPayment.notes"
        icon="notes"
        tooltip="Additional notes provided"
      />

      <OverviewItemAttributePreviewIcon
        v-if="taxPayment.attachments.length"
        tooltip="Attachments provided"
        icon="attachment"
      />
    </template>

    <template v-slot:last-column>
      <OverviewItemAmountPanel
        :currency="defaultCurrency"
        :amount="taxPayment.amount"
      />
    </template>

    <template v-slot:details>
      <OverviewItemDetailsSectionActions>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToTaxPaymentEdit"
        >
          Edit
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection title="Summary">
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            label="Date Paid"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ datePaid }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Reporting Date"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ reportingDate }}
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="taxPayment.attachments.length"
        title="Attachments"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="taxPayment.attachments" />
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="taxPayment.notes"
        title="Additional Notes"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="taxPayment.notes" />
          </div>
        </div>
      </OverviewItemDetailsSection>
    </template>
  </OverviewItem>
</template>

<script>
  import withMediumDateFormatter from '@/components/mixins/with-medium-date-formatter';
  import withWorkspaces from '@/components/mixins/with-workspaces';
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

  export default {
    name: 'IncomeTaxPaymentOverviewPanel',

    components: {
      SaDocumentsList,
      OverviewItemDetailsSectionAttribute,
      OverviewItemDetailsSection,
      SaActionLink,
      OverviewItemDetailsSectionActions,
      OverviewItemAmountPanel,
      OverviewItemAttributePreviewIcon,
      OverviewItemPrimaryAttribute,
      OverviewItem,
      SaMarkdownOutput,
    },

    mixins: [withMediumDateFormatter, withWorkspaces],

    props: {
      taxPayment: {
        type: Object,
        required: true,
      },
    },

    computed: {
      datePaid() {
        return this.mediumDateFormatter(new Date(this.taxPayment.datePaid));
      },

      reportingDate() {
        return this.mediumDateFormatter(new Date(this.taxPayment.reportingDate));
      },
    },

    methods: {
      navigateToTaxPaymentEdit() {
        this.$router.push({
          name: 'edit-income-tax-payment',
          params: { id: this.taxPayment.id },
        });
      },
    },
  };
</script>
