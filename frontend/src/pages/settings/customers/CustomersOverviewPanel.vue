<template>
  <SaOverviewItem :title="customer.name">
    <template #last-column>
      <SaActionLink
        icon="pencil-solid"
        @click="navigateToCustomerEdit"
      >
        {{ $t.customersOverview.edit() }}
      </SaActionLink>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaActionLink from '@/components/SaActionLink.vue';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import type { CustomersPageQuery } from '@/services/api/gql/graphql';

  type CustomerNode = CustomersPageQuery['workspace']['customers']['edges'][0]['node'];

  const props = defineProps<{
    customer: CustomerNode
  }>();

  const { navigateToView } = useNavigation();
  const navigateToCustomerEdit = () => navigateToView({
    name: 'edit-customer',
    params: { id: props.customer.id },
  });
</script>
