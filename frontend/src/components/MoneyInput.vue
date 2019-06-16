<template>
  <div class="el-input el-input-group el-input-group--append money-input">

    <masked-input
        type="text"
        name="phone"
        class="el-input__inner "
        v-model="inputValue"
        :mask="inputMask"
        :guide="false"
        :disabled="!currency">
    </masked-input>

    <div class="el-input-group__append">
      {{currency}}
    </div>
  </div>
</template>

<script>
  import withCurrencyFormatter from '@/components/mixins/with-currency-formatter'
  import {withCurrencyInfo} from '@/components/mixins/with-currency-info'
  import {withNumberFormatter} from '@/components/mixins/with-number-formatter'
  import MaskedInput from 'vue-text-mask'
  import createNumberMask from 'text-mask-addons/dist/createNumberMask'

  export default {
    name: 'MoneyInput',

    mixins: [withCurrencyFormatter, withCurrencyInfo, withNumberFormatter],

    props: {
      value: Number,
      currency: String
    },

    components: {
      MaskedInput
    },

    data: function () {
      return {
        inputValue: null
      }
    },

    created: function () {
      this.inputValue = this.value ? this.formatNumberDefault(this.value / this.digitsMultiplier) : null
    },

    computed: {
      inputMask: function () {
        return createNumberMask({
          prefix: '',
          thousandsSeparatorSymbol: this.thousandSeparator,
          allowDecimal: this.currencyDigits(this.currency) > 0,
          decimalSymbol: this.decimalSeparator
        })
      },

      digitsMultiplier: function () {
        return Math.pow(10, this.currencyDigits(this.currency))
      }
    },

    watch: {
      value: function (val) {
        this.inputValue = !val ? null : this.formatNumberDefault(this.value / this.digitsMultiplier)
      },

      inputValue: function (val) {
        this.$emit('input', !val ? null : Math.round(this.parserNumberDefault(this.inputValue) * this.digitsMultiplier))
      }
    }
  }
</script>

<style lang="scss">
  .money-input {
    input {
      text-align: right;
    }
  }
</style>