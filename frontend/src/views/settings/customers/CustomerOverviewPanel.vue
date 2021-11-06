<template>
  <div class="customer-panel">
    <div class="customer-info">
      <div class="sa-item-title-panel">
        <h3>{{ customer.name }}</h3>
        <span class="sa-item-edit-link">
          <SaIcon icon="pencil-solid" />
          <ElButton
            type="text"
            @click="navigateToCustomerEdit"
          >Edit</ElButton>
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import { defineComponent, PropType } from '@vue/composition-api';
  import SaIcon from '@/components/SaIcon';
  import useNavigation from '@/components/navigation/useNavigation';
  import { CustomerDto } from '@/services/api';

  export default defineComponent({
    components: { SaIcon },

    props: {
      customer: {
        type: Object as PropType<CustomerDto>,
        required: true,
      },
    },

    setup(props) {
      const { navigateToView } = useNavigation();
      const navigateToCustomerEdit = () => navigateToView({
        name: 'edit-customer',
        params: { id: props.customer.id },
      });
      return {
        navigateToCustomerEdit,
      };
    },
  });
</script>

<style lang="scss">
  @import "~@/styles/main.scss";

  .customer-panel {
    display: flex;
    justify-content: space-between;
  }

  .customer-info {
    @extend .sa-item-info-panel;
    flex-grow: 1;
  }
</style>
