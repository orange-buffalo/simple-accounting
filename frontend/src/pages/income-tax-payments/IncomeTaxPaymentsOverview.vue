<template>
  <SaOverviewPage
    :header="$t.incomeTaxPaymentsOverview.header()"
    :create-action-label="$t.incomeTaxPaymentsOverview.create()"
    create-action-view-name="create-new-income-tax-payment"
    :create-action-disabled="!currentWorkspace.editable"
  >
    <SaPageableItems
      :page-query="incomeTaxPaymentsPageQuery"
      path="workspace.incomeTaxPayments"
      :page-query-arguments="{ workspaceId: currentWorkspaceId }"
      #default="{ item: taxPayment }"
    >
      <IncomeTaxPaymentsOverviewPanel :tax-payment="taxPayment" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import IncomeTaxPaymentsOverviewPanel from '@/pages/income-tax-payments/IncomeTaxPaymentsOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';

  const incomeTaxPaymentsPageQuery = graphql(`
    query incomeTaxPaymentsPage($workspaceId: String!, $first: Int!, $after: String) {
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
                ...DocumentData
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

  const {
    currentWorkspace,
    currentWorkspaceId,
  } = useCurrentWorkspace();
</script>
