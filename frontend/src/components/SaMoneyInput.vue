<template>
  <div class="sa-money-input">
    <div class="el-input">
      <div :class="['el-input__wrapper', 'sa-money-input__input-wrapper', inputElWrapperClass]">
        <input
          type="text"
          class="el-input__inner sa-money-input__input"
          ref="inputEl"
          @focus="inputElInFocus = !inputElInFocus"
          @blur="inputElInFocus = !inputElInFocus"
        />
      </div>
    </div>

    <div class="sa-money-input__currency">
      {{ currency }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import IMask from 'imask';
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { getCurrencyInfo, getNumbersInfo } from '@/services/i18n';

const props = defineProps<{
  modelValue?: number;
  currency: string;
}>();
const emit = defineEmits<{ (e: 'update:modelValue', value?: number): void }>();

const currencyInfo = getCurrencyInfo(props.currency);
const digitsMultiplier = 10 ** currencyInfo.digits;

const inputEl = ref<HTMLElement | undefined>(undefined);
// The library is not TS-friendly at all, we need to get rid of it
// @ts-ignore
let mask: IMask.InputMask<IMask.AnyMaskedOptions> | undefined;

const setMaskValue = () => {
  if (mask) {
    if (props.modelValue === undefined) {
      mask.value = '';
    } else {
      mask.typedValue = props.modelValue / digitsMultiplier;
    }
  }
};

onMounted(() => {
  if (!inputEl.value) throw new Error('Could not mount input');
  mask = IMask(inputEl.value as HTMLElement, {
    mask: Number,
    scale: currencyInfo.digits,
    signed: false,
    thousandsSeparator: getNumbersInfo().thousandsSeparator,
    padFractionalZeros: true,
    radix: getNumbersInfo().decimalSymbol,
    overwrite: 'shift',
  });
  setMaskValue();

  mask.on('accept', () => {
    let value: number | undefined;
    if (mask?.value) {
      value = Math.round((mask.typedValue as number) * digitsMultiplier);
    }
    emit('update:modelValue', value);
  });
});

onBeforeUnmount(() => {
  if (mask) {
    mask.destroy();
    mask = undefined;
  }
});

const inputElInFocus = ref(false);
const inputElWrapperClass = computed(() => (inputElInFocus.value ? 'is-focus' : ''));

watch(() => props.modelValue, setMaskValue);
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .money-input {
    input {
      text-align: right;
    }
  }

  .sa-money-input {
    display: flex;
    @include input-width;

    &__input-wrapper {
      border-top-right-radius: 0 !important;
      border-bottom-right-radius: 0 !important;
    }

    &__input {
      flex-grow: 1;
      text-align: right;

    }

    &__currency {
      background-color: #F5F7FA;
      color: #909399;
      display: inline-flex;
      flex-direction: column;
      justify-content: center;
      position: relative;
      border: 1px solid $components-border-color;
      border-left: none;
      border-radius: 0 $components-border-radius $components-border-radius 0;
      padding: 0 20px;
      white-space: nowrap;
      height: $sa-input-height;
      box-sizing: border-box;
    }
  }
</style>
