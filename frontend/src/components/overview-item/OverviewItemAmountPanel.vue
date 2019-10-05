<template>
  <div class="overview-item-amount-panel">
    <MoneyOutput :amount="amount" :currency="currency"/>
  </div>
</template>

<script>
  import MoneyOutput from '@/components/MoneyOutput'

  export default {
    name: 'OverviewItemAmountPanel',

    components: {
      MoneyOutput
    },

    props: {
      currency: {
        type: String,
        required: true
      },
      amount: {
        type: Number,
        required: true
      }
    }
  }
</script>

<style lang="scss">
  .overview-item-amount-panel {
    height: 100%;
    display: flex;
    width: 100%;
    align-items: center;
    justify-content: flex-end;
    font-size: 115%;
  }
</style>