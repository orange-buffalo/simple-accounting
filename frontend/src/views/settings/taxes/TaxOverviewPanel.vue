<template>
  <div class="tax-panel">
    <div class="tax-info">
      <div class="sa-item-title-panel">
        <!--todo #6: localize-->
        <h3>{{ tax.title }} ({{ tax.rateInBps / 100 }}%)</h3>
        <span class="sa-item-edit-link">
          <svgicon name="pencil-solid" />
          <el-button
            type="text"
            @click="navigateToTaxEdit"
          >Edit</el-button>
        </span>
      </div>

      <div
        v-if="tax.description"
        class="sa-item-section"
      >
        <!--todo #80: linebreaks-->
        <span class="sa-item-additional-info">{{ tax.description }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import '@/components/icons/pencil-solid';
import '@/components/icons/percent';

export default {
  name: 'TaxOverviewPanel',

  props: {
    tax: Object,
  },

  methods: {
    navigateToTaxEdit() {
      this.$router.push({ name: 'edit-tax', params: { id: this.tax.id } });
    },
  },
};
</script>

<style lang="scss">
  @import "@/styles/main.scss";

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
