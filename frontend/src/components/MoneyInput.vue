<template>
  <div class="el-input el-input-group el-input-group--append money-input">
    <!-- todo #6: phone?   -->
    <MaskedInput
      v-model="inputValue"
      type="text"
      name="phone"
      class="el-input__inner "
      :mask="inputMask"
      :guide="false"
      :disabled="!currency"
    />

    <div class="el-input-group__append">
      {{ currency }}
    </div>
  </div>
</template>

<script>
  import MaskedInput from 'vue-text-mask';
  import createNumberMask from 'text-mask-addons/dist/createNumberMask';
  import i18n from '@/services/i18n';
  import { withNumberFormatter } from '@/components/mixins/with-number-formatter';

  export default {
    name: 'MoneyInput',

    components: {
      MaskedInput,
    },

    mixins: [withNumberFormatter],

    props: {
      value: Number,
      currency: String,
    },

    data() {
      return {
        inputValue: null,
      };
    },

    computed: {
      inputMask() {
        return createNumberMask({
          prefix: '',
          thousandsSeparatorSymbol: this.thousandSeparator,
          allowDecimal: i18n.getCurrencyDigits(this.currency) > 0,
          decimalSymbol: this.decimalSeparator,
        });
      },

      digitsMultiplier() {
        return 10 ** i18n.getCurrencyDigits(this.currency);
      },
    },

    watch: {
      value(val) {
        this.inputValue = !val ? null : this.formatNumberDefault(this.value / this.digitsMultiplier);
      },

      inputValue(val) {
        this.$emit(
          'input',
          !val ? null : Math.round(this.parserNumberDefault(this.inputValue) * this.digitsMultiplier),
        );
      },
    },

    created() {
      this.inputValue = this.value ? this.formatNumberDefault(this.value / this.digitsMultiplier) : null;
    },
  };
</script>

<style lang="scss">
  .money-input {
    input {
      text-align: right;
    }
  }
</style>
