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
import type { CustomerDto } from '@/services/api';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';

const props = defineProps<{
  customer: CustomerDto;
}>();

const { navigateToView } = useNavigation();
const navigateToCustomerEdit = () =>
  navigateToView({
    name: 'edit-customer',
    params: { id: props.customer.id },
  });
</script>
