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

  export default {
    name: 'MoneyInput',

    components: {
      MaskedInput,
    },

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
          thousandsSeparatorSymbol: i18n.getThousandSeparator(),
          allowDecimal: i18n.getCurrencyDigits(this.currency) > 0,
          decimalSymbol: i18n.getDecimalSeparator(),
        });
      },

      digitsMultiplier() {
        return 10 ** i18n.getCurrencyDigits(this.currency);
      },
    },

    watch: {
      value(val) {
        this.setInputValue(val);
      },

      inputValue(val) {
        this.$emit(
          'input',
          !val ? null : Math.round(i18n.parserNumber(this.inputValue) * this.digitsMultiplier),
        );
      },
    },

    created() {
      this.setInputValue(this.value);
    },

    methods: {
      setInputValue(val) {
        this.inputValue = !val ? null : this.$i18n.n(this.value / this.digitsMultiplier);
      },
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
