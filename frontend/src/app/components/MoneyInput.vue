<template>
  <el-input :value="inputValue"
            @input="onInput"
            :disabled="!currency"
            class="money-input"/>
</template>

<script>
  import withCurrencyFormatter from '@/app/components/mixins/with-currency-formatter'

  // inspired by https://github.com/vuejs-tips/v-money

  export default {
    name: 'MoneyInput',

    mixins: [withCurrencyFormatter],

    props: {
      value: null,
      currency: String
    },

    data: function () {
      return {
        inputValue: this.value
      }
    },

    created: function () {
      this.onInput(this.value)
    },

    methods: {
      onInput: function (userInput) {
        if (!this.currency) {
          return
        }

        let numbers = parseInt((userInput ? userInput.toString() : '').replace(/\D+/g, '') || '0')

        // todo proper precision based on currency
        this.inputValue = this.currencyFormatter(numbers / 100)

        this.$emit('input', numbers);
      }
    },

    watch: {
      currency: function (val) {
        this.ensureCurrencyFormatter(val)
        this.onInput(this.value)
      }
    }
  }
</script>

<style lang="scss">
  .money-input {
    input {
      text-align: right;
      max-width: 200px;
    }
  }
</style>