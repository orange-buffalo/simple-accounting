<template>
  <OverviewItem :title="taxPayment.title">
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute
        v-if="taxPayment.datePaid"
        :tooltip="$t('incomeTaxPaymentOverviewPanel.datePaid.label')"
        icon="calendar"
      >
        {{ $t('common.date.medium', [taxPayment.datePaid]) }}
      </OverviewItemPrimaryAttribute>
    </template>

    <template v-slot:attributes-preview>
      <OverviewItemAttributePreviewIcon
        v-if="taxPayment.notes"
        icon="notes"
        :tooltip="$t('incomeTaxPaymentOverviewPanel.notes.tooltip')"
      />

      <OverviewItemAttributePreviewIcon
        v-if="taxPayment.attachments.length"
        :tooltip="$t('incomeTaxPaymentOverviewPanel.attachments.tooltip')"
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
          {{ $t('incomeTaxPaymentOverviewPanel.edit') }}
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection
        :title="$t('incomeTaxPaymentOverviewPanel.summary.header')"
      >
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            :label="$t('incomeTaxPaymentOverviewPanel.datePaid.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [taxPayment.datePaid]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('incomeTaxPaymentOverviewPanel.reportingDate.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [taxPayment.reportingDate]) }}
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="taxPayment.attachments.length"
        :title="$t('incomeTaxPaymentOverviewPanel.attachments.header')"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="taxPayment.attachments" />
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="taxPayment.notes"
        :title="$t('incomeTaxPaymentOverviewPanel.notes.header')"
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

    mixins: [withWorkspaces],

    props: {
      taxPayment: {
        type: Object,
        required: true,
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
