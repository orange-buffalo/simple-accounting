<template>
  <div class="tax-panel">
    <div class="tax-info">
      <div class="sa-item-title-panel">
        <!--todo #163: localize-->
        <h3>{{ tax.title }} ({{ tax.rateInBps / 100 }}%)</h3>
        <span class="sa-item-edit-link">
          <SaIcon icon="pencil-solid" />
          <ElButton
            type="text"
            @click="navigateToTaxEdit"
          >Edit</ElButton>
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
  import SaIcon from '@/components/SaIcon';

  export default {
    name: 'GeneralTaxOverviewPanel',
    components: { SaIcon },
    props: {
      tax: {
        type: Object,
        required: true,
      },
    },

    methods: {
      navigateToTaxEdit() {
        this.$router.push({
          name: 'edit-general-tax',
          params: { id: this.tax.id },
        });
      },
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/main.scss";

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
