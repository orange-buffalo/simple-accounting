<template>
  <div class="customer-panel">
    <div class="customer-info">
      <div class="sa-item-title-panel">
        <h3>{{ customer.name }}</h3>
        <span class="sa-item-edit-link">
          <SaIcon icon="pencil-solid" />
          <ElButton
            link
            @click="navigateToCustomerEdit"
          >{{ $t.customersOverview.edit() }}</ElButton>
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import SaIcon from '@/components/SaIcon.vue';
  import useNavigation from '@/services/use-navigation';
  import type { CustomerDto } from '@/services/api';
  import { $t } from '@/services/i18n';

  const props = defineProps<{
    customer: CustomerDto
  }>();

  const { navigateToView } = useNavigation();
  const navigateToCustomerEdit = () => navigateToView({
    name: 'edit-customer',
    params: { id: props.customer.id },
  });
</script>

<style lang="scss">
  @use "@/styles/main.scss" as *;

  .customer-panel {
    display: flex;
    justify-content: space-between;
  }

  .customer-info {
    @extend .sa-item-info-panel;
    flex-grow: 1;
  }
</style>
