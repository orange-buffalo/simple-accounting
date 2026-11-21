<template>
  <SaFormItemInternal v-bind="props" v-model="formValue">
    <ElInput
      :model-value="displayValue"
      :placeholder="placeholder"
      :inputmode="scaledDecimalPlaces === undefined ? undefined : 'decimal'"
      @update:model-value="updateValue"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import { getNumbersInfo } from '@/services/i18n';

  const props = defineProps<SaFormComponentProps & {
    placeholder?: string,
    // Store a scaled integer while displaying a decimal, e.g. 2029 for 20.29 with two places.
    scaledDecimalPlaces?: number,
  }>();

  const invalidFormat = defineModel<boolean>('invalidFormat');
  const displayValue = ref<string>('');
  const formValue = ref<number | null>(null);

  function parseScaledDecimal(text: string, decimalPlaces: number): number | null | undefined {
    const value = text.trim().replace(getNumbersInfo().decimalSymbol, '.');
    if (!value) return null;

    const match = /^([+-]?)(\d*)(?:[.,](\d*))?$/.exec(value);
    if (!match) return undefined;
    const [, sign, whole, fraction = ''] = match;
    if ((!whole && !fraction) || fraction.length > decimalPlaces) return undefined;

    const scaled = Number(`${sign}${whole}${fraction.padEnd(decimalPlaces, '0')}`);
    return Number.isSafeInteger(scaled) ? scaled : undefined;
  }

  function formatScaledDecimal(value: number, decimalPlaces: number): string {
    if (decimalPlaces === 0) return String(value);

    const digits = String(Math.abs(value)).padStart(decimalPlaces + 1, '0');
    const whole = digits.slice(0, -decimalPlaces);
    const fraction = digits.slice(-decimalPlaces).replace(/0+$/, '');
    const sign = value < 0 ? '-' : '';
    return fraction ? `${sign}${whole}${getNumbersInfo().decimalSymbol}${fraction}` : `${sign}${whole}`;
  }

  function updateValue(text: string) {
    displayValue.value = text;
    if (props.scaledDecimalPlaces === undefined) {
      const number = Number(text);
      formValue.value = text === '' || Number.isNaN(number) ? null : number;
      return;
    }

    const scaled = parseScaledDecimal(text, props.scaledDecimalPlaces);
    invalidFormat.value = scaled === undefined;
    formValue.value = scaled ?? null;
  }

  watch(formValue, (value) => {
    const decimalPlaces = props.scaledDecimalPlaces;
    if (decimalPlaces === undefined) {
      displayValue.value = value == null ? '' : String(value);
    } else if ((parseScaledDecimal(displayValue.value, decimalPlaces) ?? null) !== value) {
      displayValue.value = value == null ? '' : formatScaledDecimal(value, decimalPlaces);
      invalidFormat.value = false;
    }
  });
</script>
