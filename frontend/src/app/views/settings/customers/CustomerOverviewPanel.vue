<template>
  <div class="customer-panel">
    <div class="customer-info">
      <div class="sa-item-title-panel">
        <h3>{{customer.name}}</h3>
        <span class="sa-item-edit-link">
          <svgicon name="pencil"/>
          <el-button type="text"
                     @click="navigateToCustomerEdit">Edit</el-button>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
  import '@/components/icons/pencil'

  export default {
    name: 'CustomerOverviewPanel',

    props: {
      customer: Object
    },

    methods: {
      navigateToCustomerEdit: function () {
        this.$router.push({name: 'edit-customer', params: {id: this.customer.id}})
      }
    }
  }
</script>

<style lang="scss">
  @import "@/app/main.scss";

  .customer-panel {
    display: flex;
    justify-content: space-between;
  }

  .customer-info {
    @extend .sa-item-info-panel;
    flex-grow: 1;
  }
</style>
