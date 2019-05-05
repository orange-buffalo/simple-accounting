<template>
  <div class="tax-panel">
    <div class="tax-info">
      <div class="sa-item-title-panel">
        <!--todo #6: localize-->
        <h3>{{tax.title}} ({{tax.rateInBps / 100}}%)</h3>
        <span class="sa-item-edit-link">
          <svgicon name="pencil"/>
          <el-button type="text"
                     @click="navigateToTaxEdit">Edit</el-button>
        </span>
      </div>

      <div class="sa-item-section" v-if="tax.description">
        <!--todo #80: linebreaks-->
        <span class="sa-item-additional-info">{{tax.description}}</span>
      </div>
    </div>
  </div>
</template>

<script>
  import '@/components/icons/pencil'
  import '@/components/icons/percent'

  export default {
    name: 'TaxOverviewPanel',

    props: {
      tax: Object
    },

    methods: {
      navigateToTaxEdit: function () {
        this.$router.push({name: 'edit-tax', params: {id: this.tax.id}})
      }
    }
  }
</script>

<style lang="scss">
  @import "@/app/styles/main.scss";

  .tax-panel {
    display: flex;
    justify-content: space-between;
  }

  .tax-info {
    @extend .sa-item-info-panel;
    flex-grow: 1;

    .sa-item-section {
      margin-top: 0;
    }
  }
</style>
