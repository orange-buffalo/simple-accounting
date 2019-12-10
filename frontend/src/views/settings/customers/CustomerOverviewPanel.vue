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

<script>
  import SaIcon from '@/components/SaIcon';

  export default {
    name: 'CustomerOverviewPanel',
    components: { SaIcon },
    props: {
      customer: {
        type: Object,
        required: true,
      },
    },

    methods: {
      navigateToCustomerEdit() {
        this.$router.push({
          name: 'edit-customer',
          params: { id: this.customer.id },
        });
      },
    },
  };
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
