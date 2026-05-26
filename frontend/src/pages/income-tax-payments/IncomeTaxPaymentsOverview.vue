<template>
  <SaOverviewPage :header="$t.incomeTaxPaymentsOverview.header()">
    <template #actions>
      <ElButton
        round
        :disabled="!currentWorkspace.editable"
        @click="navigateToCreateTaxPaymentView"
      >
        <SaIcon icon="plus-thin" />
        {{ $t.incomeTaxPaymentsOverview.create() }}
      </ElButton>
    </template>

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
  import SaIcon from '@/components/SaIcon.vue';
  import IncomeTaxPaymentsOverviewPanel from '@/pages/income-tax-payments/IncomeTaxPaymentsOverviewPanel.vue';
  import useNavigation from '@/services/use-navigation';
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

  const { navigateByViewName } = useNavigation();
  const navigateToCreateTaxPaymentView = () => navigateByViewName('create-new-income-tax-payment');
  const {
    currentWorkspace,
    currentWorkspaceId,
  } = useCurrentWorkspace();
</script>
