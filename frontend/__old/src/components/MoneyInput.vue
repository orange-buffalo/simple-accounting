<template>
  <div class="sa-money-input">
    <MaskedInput
      v-model="inputValue"
      type="text"
      class="el-input__inner sa-money-input__input"
      :mask="inputMask"
      :guide="false"
      :disabled="!currency"
    />

    <div class="sa-money-input__currency">
      {{ currency }}
    </div>
  </div>
</template>

<script>
  import MaskedInput from 'vue-text-mask';
  import createNumberMask from 'text-mask-addons/dist/createNumberMask';
  import i18n from '@/services/i18n';

  export default {
    components: {
      MaskedInput,
    },

    props: {
      value: {
        type: Number,
        default: null,
      },
      currency: {
        type: String,
        required: true,
      },
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
          thousandsSeparatorSymbol: i18n.getNumbersInfo().thousandsSeparator,
          allowDecimal: i18n.getCurrencyInfo(this.currency).digits > 0,
          decimalSymbol: i18n.getNumbersInfo().decimalSymbol,
        });
      },

      digitsMultiplier() {
        return 10 ** i18n.getCurrencyInfo(this.currency).digits;
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
  @import "~@/styles/vars.scss";
  @import "~@/styles/mixins.scss";

  .money-input {
    input {
      text-align: right;
    }
  }

  .sa-money-input {
    display: flex;
    @include input-width;

    &__input {
      flex-grow: 1;
      text-align: right;
      border-top-right-radius: 0 !important;
      border-bottom-right-radius: 0 !important;
    }

    &__currency {
      background-color: #F5F7FA;
      color: #909399;
      vertical-align: middle;
      display: table-cell;
      position: relative;
      border: 1px solid $components-border-color;
      border-left: none;
      border-radius: 0 $components-border-radius $components-border-radius 0;
      padding: 0 20px;
      white-space: nowrap;
      height: $input-height;
      box-sizing: border-box;
    }
  }
</style>
