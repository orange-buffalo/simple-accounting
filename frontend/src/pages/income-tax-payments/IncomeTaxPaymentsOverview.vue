<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.incomeTaxPaymentsOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.incomeTaxPaymentsOverview.filters.announcement() }}</span>
        </div>

        <ElButton
          round
          :disabled="!currentWorkspace.editable"
          @click="navigateToCreateTaxPaymentView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.incomeTaxPaymentsOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItemsGql
      :page-query="incomeTaxPaymentsPageQuery"
      path="workspace.incomeTaxPayments"
      :page-query-arguments="{ workspaceId: currentWorkspaceId }"
      #default="{ item: taxPayment }"
    >
      <IncomeTaxPaymentsOverviewPanel :tax-payment="taxPayment" />
    </SaPageableItemsGql>
  </div>
</template>

<script lang="ts" setup>
  import SaPageableItemsGql from '@/components/pageable-items/SaPageableItemsGql.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import IncomeTaxPaymentsOverviewPanel from '@/pages/income-tax-payments/IncomeTaxPaymentsOverviewPanel.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';

  const incomeTaxPaymentsPageQuery = graphql(`
    query incomeTaxPaymentsPage($workspaceId: Long!, $first: Int!, $after: String) {
      workspace(id: $workspaceId) {
        incomeTaxPayments(first: $first, after: $after) {
          edges {
            cursor
            node {
              id
              title
              datePaid
              reportingDate
              amount
              attachments {
                id
              }
              notes
            }
          }
          pageInfo {
            ...PaginationPageInfo
          }
          totalCount
        }
      }
    }
  `);

  const { navigateByViewName } = useNavigation();
  const navigateToCreateTaxPaymentView = () => navigateByViewName('create-new-income-tax-payment');
  const {
    currentWorkspace,
    currentWorkspaceId,
  } = useCurrentWorkspace();
</script>
