<template>
  <SaFormItemInternal
    v-bind="props"
    v-model="formValue"
    :input-error="inputError"
    :adapt-api-error="adaptApiError"
  >
    <ElInput
      :model-value="displayValue"
      :placeholder="placeholder"
      :inputmode="scaledDecimalPlaces === undefined ? undefined : 'decimal'"
      @update:model-value="updateValue"
      @blur="formatDisplayValue"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import { AdaptApiFieldError } from '@/components/form/sa-form-components-api.ts';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import { $t, getNumbersInfo } from '@/services/i18n';

  const props = defineProps<SaFormComponentProps & {
    placeholder?: string,
    // Store a scaled integer while displaying a decimal, e.g. 2029 for 20.29 with two places.
    scaledDecimalPlaces?: number,
  }>();

  const displayValue = ref<string>('');
  const formValue = ref<number | null>(null);

  function parseScaledDecimal(text: string, decimalPlaces: number): number | null | undefined {
    const value = text.trim();
    if (!value) return null;

    const { decimalSymbol, thousandsSeparator } = getNumbersInfo();
    const alternateDecimal = decimalSymbol === '.' ? ',' : '.';
    const sign = value.startsWith('-') ? '-' : '';
    const unsigned = /^[+-]/.test(value) ? value.slice(1) : value;

    function parseWithSeparator(separator: string, allowGrouping: boolean): number | undefined {
      const [whole, fraction = '', ...extra] = unsigned.split(separator);
      if (extra.length || !/^\d*$/.test(fraction) || fraction.length > decimalPlaces) return undefined;
      const groups = allowGrouping ? whole.split(thousandsSeparator) : [whole];
      const validGrouping = /^\d{1,3}$/.test(groups[0]) && groups.slice(1).every((group) => /^\d{3}$/.test(group));
      if (groups.length > 1 && !validGrouping) {
        return undefined;
      }
      const digits = groups.join('');
      if (!/^\d*$/.test(digits) || (!digits && !fraction)) return undefined;

      const scaled = Number(`${sign}${digits}${fraction.padEnd(decimalPlaces, '0')}`);
      return Number.isSafeInteger(scaled) ? scaled : undefined;
    }

    return parseWithSeparator(decimalSymbol, true) ?? parseWithSeparator(alternateDecimal, false);
  }

  function formatScaledDecimal(value: number, decimalPlaces: number): string {
    const { decimalSymbol, thousandsSeparator } = getNumbersInfo();
    const digits = String(Math.abs(value)).padStart(decimalPlaces + 1, '0');
    const whole = (decimalPlaces ? digits.slice(0, -decimalPlaces) : digits).replace(
      /\B(?=(\d{3})+(?!\d))/g, thousandsSeparator,
    );
    const fraction = decimalPlaces ? digits.slice(-decimalPlaces).replace(/0+$/, '') : '';
    const sign = value < 0 ? '-' : '';
    return fraction ? `${sign}${whole}${decimalSymbol}${fraction}` : `${sign}${whole}`;
  }

  const adaptApiError: AdaptApiFieldError = (error) => {
    const places = props.scaledDecimalPlaces;
    if (places === undefined || (error.error !== 'MinConstraintViolated' && error.error !== 'MaxConstraintViolated')) {
      return error;
    }
    const bound = error.params?.value;
    if (bound === undefined) return error;
    return { ...error, params: { ...error.params, value: String(Number(bound) / (10 ** places)) } };
  };
  const inputError = computed(() => {
    const places = props.scaledDecimalPlaces;
    return places !== undefined && parseScaledDecimal(displayValue.value, places) === undefined
      ? $t.value.formValidationMessages.invalidDecimal(places)
      : undefined;
  });

  function updateValue(text: string) {
    displayValue.value = text;
    if (props.scaledDecimalPlaces === undefined) {
      const number = Number(text);
      formValue.value = text === '' || Number.isNaN(number) ? null : number;
      return;
    }

    formValue.value = parseScaledDecimal(text, props.scaledDecimalPlaces) ?? null;
  }

  function formatDisplayValue() {
    const places = props.scaledDecimalPlaces;
    if (places !== undefined && formValue.value != null) {
      displayValue.value = formatScaledDecimal(formValue.value, places);
    }
  }

  watch(formValue, (value) => {
    const decimalPlaces = props.scaledDecimalPlaces;
    if (decimalPlaces === undefined) {
      displayValue.value = value == null ? '' : String(value);
    } else if ((parseScaledDecimal(displayValue.value, decimalPlaces) ?? null) !== value) {
      displayValue.value = value == null ? '' : formatScaledDecimal(value, decimalPlaces);
    }
  });
</script>
