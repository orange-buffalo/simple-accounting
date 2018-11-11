<template>
  <el-input :value="inputValue"
            @input="onInput"
            :disabled="!currency"
            class="money-input"/>
</template>

<script>
  import {mapActions, mapGetters, mapState} from 'vuex'

  // inspired by https://github.com/vuejs-tips/v-money

  export default {
    name: 'MoneyInput',

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
      this.loadCurrencies()
      this.onInput(this.value)
    },

    methods: {
      ...mapActions('app', {
        loadCurrencies: 'loadCurrencies'
      }),

      onInput: function (userInput) {
        if (!this.currency) {
          return
        }

        let numbers = parseInt((userInput ? userInput.toString() : '').replace(/\D+/g, '') || '0')

        // todo proper precision based on currency
        this.inputValue = this.getCurrencyFormatter(this.currency)(numbers / 100)

        this.$emit('input', numbers);
      }
    },

    computed: {
      ...mapState({}),

      ...mapGetters({
        getCurrencyFormatter: 'i18n/getCurrencyFormatter'
      })
    },

    watch: {
      currency() {
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