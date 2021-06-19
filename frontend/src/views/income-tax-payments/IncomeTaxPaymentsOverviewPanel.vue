<template>
  <OverviewItem :title="taxPayment.title">
    <template #primary-attributes>
      <OverviewItemPrimaryAttribute
        v-if="taxPayment.datePaid"
        :tooltip="$t('incomeTaxPaymentsOverviewPanel.datePaid.label')"
        icon="calendar"
      >
        {{ $t('common.date.medium', [taxPayment.datePaid]) }}
      </OverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <OverviewItemAttributePreviewIcon
        v-if="taxPayment.notes"
        icon="notes"
        :tooltip="$t('incomeTaxPaymentsOverviewPanel.notes.tooltip')"
      />

      <OverviewItemAttributePreviewIcon
        v-if="taxPayment.attachments.length"
        :tooltip="$t('incomeTaxPaymentsOverviewPanel.attachments.tooltip')"
        icon="attachment"
      />
    </template>

    <template #last-column>
      <OverviewItemAmountPanel
        :currency="defaultCurrency"
        :amount="taxPayment.amount"
      />
    </template>

    <template #details>
      <OverviewItemDetailsSectionActions>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToTaxPaymentEdit"
        >
          {{ $t('incomeTaxPaymentsOverviewPanel.edit') }}
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection
        :title="$t('incomeTaxPaymentsOverviewPanel.summary.header')"
      >
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            :label="$t('incomeTaxPaymentsOverviewPanel.datePaid.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [taxPayment.datePaid]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('incomeTaxPaymentsOverviewPanel.reportingDate.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [taxPayment.reportingDate]) }}
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="taxPayment.attachments.length"
        :title="$t('incomeTaxPaymentsOverviewPanel.attachments.header')"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="taxPayment.attachments" />
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="taxPayment.notes"
        :title="$t('incomeTaxPaymentsOverviewPanel.notes.header')"
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

<script lang="ts">
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
  import { defineComponent, PropType } from '@vue/composition-api';
  import { IncomeTaxPaymentDto } from '@/services/api';
  import useNavigation from '@/components/navigation/useNavigation';
  import { useCurrentWorkspace } from '@/services/workspaces';

  export default defineComponent({
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

    props: {
      taxPayment: {
        type: Object as PropType<IncomeTaxPaymentDto>,
        required: true,
      },
    },

    setup(props) {
      const { navigateToView } = useNavigation();
      const navigateToTaxPaymentEdit = () => navigateToView({
        name: 'edit-income-tax-payment',
        params: { id: props.taxPayment.id },
      });

      const {
        currentWorkspace,
        defaultCurrency,
      } = useCurrentWorkspace();

      return {
        navigateToTaxPaymentEdit,
        currentWorkspace,
        defaultCurrency,
      };
    },
  });
</script>
